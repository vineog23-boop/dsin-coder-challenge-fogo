package br.com.dsin.challengefogo.equipment.web;

import br.com.dsin.challengefogo.auth.application.TokenService;
import br.com.dsin.challengefogo.equipment.application.EquipmentNotFoundException;
import br.com.dsin.challengefogo.equipment.application.EquipmentService;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentFilter;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.RadarDto;
import br.com.dsin.challengefogo.equipment.application.dto.StatusChangeDtoRequest;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.config.ClockConfig;
import br.com.dsin.challengefogo.shared.config.WebConfig;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException;
import br.com.dsin.challengefogo.shared.domain.FieldValidationException.FieldViolation;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.security.ProblemAuthenticationEntryPoint;
import br.com.dsin.challengefogo.shared.security.SecurityConfig;
import br.com.dsin.challengefogo.shared.web.ProblemTypes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class, TokenService.class, ClockConfig.class,
        WebConfig.class})
class EquipmentControllerTest {

    private static final String BASE = "/api/equipments";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService equipmentService;

    private static RequestPostProcessor gestor() {
        return jwt().jwt(j -> j.subject("gestor")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }

    private static RequestPostProcessor operador() {
        return jwt().jwt(j -> j.subject("operador")).authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"));
    }

    private static String json(String serialNumber, String plate, String latitude) {
        String vehicle = plate == null ? "" : ",\"vehicle\":{\"licensePlate\":\"" + plate + "\",\"autonomous\":false}";

        return """
                {"type":"%s","serialNumber":"%s","brand":"Marca","model":"M1","agency":"MUNICIPAL_TRAFFIC",
                 "location":{"street":"Rua A","neighborhood":"Centro","city":"São Paulo","state":"SP",
                 "region":"NORTH","latitude":%s,"longitude":-46.65}%s}
                """.formatted(plate == null ? "RADAR" : "TAXI", serialNumber, latitude, vehicle);
    }

    private static EquipmentDtoResponse response(UUID id) {
        LocationDto location = new LocationDto(null, "Rua A", null, "Centro", "São Paulo", "SP", Region.NORTH,
                new BigDecimal("-23.5"), new BigDecimal("-46.65"));

        return new EquipmentDtoResponse(id, EquipmentType.RADAR, "RAD-001", "Marca", "M1", Agency.MUNICIPAL_TRAFFIC,
                EquipmentStatus.NOT_INSTALLED, MaintenanceNeed.NONE, null, null, location,
                new RadarDto(RadarModality.SPEED, 60, 2), null, null, null, null, Instant.parse("2026-10-26T15:00:00Z"),
                Instant.parse("2026-10-26T15:00:00Z"));
    }

    @Test
    void deveResponder201ComLocationQuandoPostValido() throws Exception {
        UUID id = UUID.randomUUID();
        when(equipmentService.create(any(EquipmentDtoRequest.class))).thenReturn(response(id));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("RAD-001", null, "-23.5")));

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(BASE + "/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.serialNumber").value("RAD-001"));
    }

    @Test
    void deveResponder400ApontandoCamposAninhadosQuandoBeanValidationFalha() throws Exception {
        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("  ", "abc-1234", "91")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION.toString()))
                .andExpect(jsonPath("$.errors[?(@.field=='serialNumber')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='vehicle.licensePlate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='location.latitude')]").exists());
        verifyNoInteractions(equipmentService);
    }

    @Test
    void deveResponder400ComCampoDaViolacaoQuandoServiceLancaFieldValidationException() throws Exception {
        when(equipmentService.create(any(EquipmentDtoRequest.class))).thenThrow(new FieldValidationException(
                List.of(new FieldViolation("camera", "Radar não aceita características de câmera."))));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("RAD-001", null, "-23.5")));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION.toString()))
                .andExpect(jsonPath("$.errors[0].field").value("camera"));
    }

    @Test
    void deveResponder409QuandoServiceLancaConflictException() throws Exception {
        when(equipmentService.create(any(EquipmentDtoRequest.class))).thenThrow(new ConflictException("Duplicado."));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("RAD-001", null, "-23.5")));

        result.andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.CONFLICT.toString()));
    }

    @Test
    void deveResponder404QuandoEquipamentoNaoExisteNoGetPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(equipmentService.findById(id)).thenThrow(new EquipmentNotFoundException(id));

        var result = mockMvc.perform(get(BASE + "/" + id).with(gestor()));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()));
    }

    @Test
    void deveResponder422QuandoRegraDeNegocioFalhaNoPatchDeStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(equipmentService.changeStatus(eq(id), any(StatusChangeDtoRequest.class), any()))
                .thenThrow(new BusinessRuleException("Mesma situação."));

        var result = mockMvc.perform(patch(BASE + "/" + id + "/status").with(gestor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPERATIONAL\"}"));

        result.andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.BUSINESS_RULE.toString()));
    }

    @Test
    void devePassarSubjectDoTokenAoServiceQuandoPatchDeStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(equipmentService.changeStatus(eq(id), any(StatusChangeDtoRequest.class), eq("gestor")))
                .thenReturn(response(id));

        var result = mockMvc.perform(patch(BASE + "/" + id + "/status").with(gestor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPERATIONAL\",\"note\":\"ok\"}"));

        result.andExpect(status().isOk());
        verify(equipmentService).changeStatus(eq(id),
                eq(new StatusChangeDtoRequest(EquipmentStatus.OPERATIONAL, "ok")), eq("gestor"));
    }

    @Test
    void deveResponder201QuandoRegistraManutencao() throws Exception {
        UUID id = UUID.randomUUID();
        Instant performedAt = Instant.parse("2026-10-20T10:00:00Z");
        when(equipmentService.registerMaintenance(eq(id), any(MaintenanceDtoRequest.class), eq("gestor")))
                .thenReturn(new MaintenanceDtoResponse(UUID.randomUUID(), MaintenanceKind.PREVENTIVE, performedAt,
                        "Troca de lente", "gestor"));

        var result = mockMvc.perform(post(BASE + "/" + id + "/maintenances").with(gestor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"PREVENTIVE\",\"performedAt\":\"2026-10-20T10:00:00Z\","
                        + "\"description\":\"Troca de lente\"}"));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.performedBy").value("gestor"));
    }

    @Test
    void devePermitirGetEBloquearPostQuandoOperador() throws Exception {
        UUID id = UUID.randomUUID();
        when(equipmentService.findById(id)).thenReturn(response(id));

        mockMvc.perform(get(BASE + "/" + id).with(operador())).andExpect(status().isOk());
        mockMvc.perform(post(BASE).with(operador()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("RAD-001", null, "-23.5")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(equipmentService).findById(id);
    }

    @Test
    void deveResponder401QuandoSemToken() throws Exception {
        var result = mockMvc.perform(get(BASE));

        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(equipmentService);
    }

    @Test
    void deveSerializarPaginaNoFormatoEstavelEPassarFiltrosQuandoListagem() throws Exception {
        when(equipmentService.search(any(EquipmentFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(UUID.randomUUID())), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        var result = mockMvc.perform(get(BASE).with(operador())
                .param("type", "RADAR").param("region", "NORTH").param("status", "MALFUNCTION")
                .param("search", "abc"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serialNumber").value("RAD-001"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));
        ArgumentCaptor<EquipmentFilter> filter = ArgumentCaptor.forClass(EquipmentFilter.class);
        verify(equipmentService).search(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue()).isEqualTo(new EquipmentFilter(EquipmentType.RADAR, EquipmentStatus.MALFUNCTION,
                Region.NORTH, null, null, null, "abc"));
    }

    @Test
    void deveLimitarTamanhoDaPaginaA100QuandoSizeMaiorQueOMaximo() throws Exception {
        when(equipmentService.search(any(EquipmentFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE).with(gestor()).param("size", "500")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(equipmentService).search(any(EquipmentFilter.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void deveOrdenarPorCreatedAtDescQuandoSemSortNaListagem() throws Exception {
        when(equipmentService.search(any(EquipmentFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE).with(gestor())).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(equipmentService).search(any(EquipmentFilter.class), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }
}
