package br.com.dsin.challengefogo.operations.web;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.auth.application.TokenService;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.operations.application.AlertService;
import br.com.dsin.challengefogo.operations.application.OperationsService;
import br.com.dsin.challengefogo.operations.application.dto.AgentMarkerDto;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.EquipmentMarkerDto;
import br.com.dsin.challengefogo.operations.application.dto.MapDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.MapFilter;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse;
import br.com.dsin.challengefogo.shared.config.ClockConfig;
import br.com.dsin.challengefogo.shared.config.WebConfig;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.security.ProblemAuthenticationEntryPoint;
import br.com.dsin.challengefogo.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationsController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class, TokenService.class, ClockConfig.class,
        WebConfig.class})
class OperationsControllerTest {

    private static final String BASE = "/api/operations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationsService operationsService;

    @MockitoBean
    private AlertService alertService;

    private static RequestPostProcessor gestor() {
        return jwt().jwt(j -> j.subject("gestor")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }

    private static RequestPostProcessor operador() {
        return jwt().jwt(j -> j.subject("operador")).authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"));
    }

    private static SummaryDtoResponse summary() {
        return new SummaryDtoResponse(
                new SummaryDtoResponse.EquipmentSummary(10, Map.of(EquipmentStatus.MALFUNCTION, 3L), 1, 2, 4),
                new SummaryDtoResponse.AgentSummary(5, 4, Map.of(AgentStatus.AVAILABLE, 4L)),
                List.of(new SummaryDtoResponse.RegionSummary(Region.NORTH, 3, 2)),
                Instant.parse("2026-01-01T10:00:00Z"));
    }

    private static MapDtoResponse mapa() {
        EquipmentMarkerDto equipment = new EquipmentMarkerDto(UUID.randomUUID(), EquipmentType.RADAR, "SN-1",
                EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE, new BigDecimal("-23.5"),
                new BigDecimal("-46.6"), Region.NORTH);
        AgentMarkerDto agent = new AgentMarkerDto(UUID.randomUUID(), "Ana", AgentType.MUNICIPAL_GUARD,
                AgentStatus.AVAILABLE, new BigDecimal("-23.5"), new BigDecimal("-46.6"),
                AgentMarkerDto.PositionSource.BASE, null, null, Region.NORTH);

        return new MapDtoResponse(List.of(equipment), List.of(agent), true);
    }

    private static AlertDtoResponse alerta() {
        return new AlertDtoResponse(AlertDtoResponse.Kind.EQUIPMENT_MALFUNCTION, AlertDtoResponse.Severity.HIGH,
                "Radar com defeito", Region.NORTH, UUID.randomUUID(), EquipmentType.RADAR,
                Instant.parse("2026-01-01T10:00:00Z"));
    }

    private Pageable capturarPageable(String query) throws Exception {
        when(alertService.alerts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alerta()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE + "/alerts" + query).with(gestor())).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertService).alerts(captor.capture());
        return captor.getValue();
    }

    @Test
    void deveRetornarResumoQuandoGestorConsulta() throws Exception {
        when(operationsService.summary()).thenReturn(summary());

        mockMvc.perform(get(BASE + "/summary").with(gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments.byStatus.MALFUNCTION").value(3))
                .andExpect(jsonPath("$.equipments.upcomingMaintenance").value(4))
                .andExpect(jsonPath("$.agents.active").value(4))
                .andExpect(jsonPath("$.regions[0].region").value("NORTH"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void deveRetornarResumoQuandoOperadorConsulta() throws Exception {
        when(operationsService.summary()).thenReturn(summary());

        mockMvc.perform(get(BASE + "/summary").with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments.byStatus.MALFUNCTION").value(3))
                .andExpect(jsonPath("$.agents.active").value(4))
                .andExpect(jsonPath("$.regions[0].region").value("NORTH"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void deveRepassarFiltrosAoServiceQuandoMapaRecebeParametros() throws Exception {
        when(operationsService.map(any(MapFilter.class))).thenReturn(mapa());

        mockMvc.perform(get(BASE + "/map")
                        .param("equipmentType", "RADAR")
                        .param("equipmentStatus", "MALFUNCTION")
                        .param("agentStatus", "OFF_DUTY")
                        .param("region", "NORTH")
                        .param("agency", "MUNICIPAL_TRAFFIC")
                        .param("neighborhood", "Vila Marcondes")
                        .with(gestor()))
                .andExpect(status().isOk());

        ArgumentCaptor<MapFilter> captor = ArgumentCaptor.forClass(MapFilter.class);
        verify(operationsService).map(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new MapFilter(EquipmentType.RADAR, EquipmentStatus.MALFUNCTION,
                AgentStatus.OFF_DUTY, Region.NORTH, Agency.MUNICIPAL_TRAFFIC, "Vila Marcondes"));
    }

    @Test
    void deveRepassarFiltroVazioAoServiceQuandoMapaNaoRecebeParametros() throws Exception {
        when(operationsService.map(any(MapFilter.class))).thenReturn(mapa());

        mockMvc.perform(get(BASE + "/map").with(operador())).andExpect(status().isOk());

        ArgumentCaptor<MapFilter> captor = ArgumentCaptor.forClass(MapFilter.class);
        verify(operationsService).map(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new MapFilter(null, null, null, null, null, null));
    }

    @Test
    void deveListarBairrosParaOOperadorQuandoConsulta() throws Exception {
        when(operationsService.neighborhoods()).thenReturn(List.of("Centro", "Vila Marcondes"));

        mockMvc.perform(get(BASE + "/neighborhoods").with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("Vila Marcondes"));
    }

    @Test
    void naoDeveListarBairrosQuandoSemLogin() throws Exception {
        mockMvc.perform(get(BASE + "/neighborhoods")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveSerializarMapaQuandoServiceRetornaPontos() throws Exception {
        when(operationsService.map(any(MapFilter.class))).thenReturn(mapa());

        mockMvc.perform(get(BASE + "/map").with(gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].latitude").value(-23.5))
                .andExpect(jsonPath("$.agents[0].positionSource").value("BASE"))
                .andExpect(jsonPath("$.truncated").value(true));
    }

    @Test
    void deveRetornar400QuandoTipoDeEquipamentoDoMapaEInvalido() throws Exception {
        mockMvc.perform(get(BASE + "/map").param("equipmentType", "FOO").with(gestor()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(operationsService);
    }

    @Test
    void deveRepassarPaginaETamanhoQuandoAlertasRecebeParametros() throws Exception {
        Pageable pageable = capturarPageable("?page=1&size=5");

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void deveUsarPaginaPadraoQuandoAlertasNaoRecebeParametros() throws Exception {
        Pageable pageable = capturarPageable("");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void deveLimitarTamanhoA100QuandoAlertasPedeTamanhoMaior() throws Exception {
        Pageable pageable = capturarPageable("?size=500");

        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void deveSerializarAlertasQuandoServiceRetornaPagina() throws Exception {
        when(alertService.alerts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alerta()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE + "/alerts").with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].kind").value("EQUIPMENT_MALFUNCTION"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void deveRetornar401QuandoSemTokenNasRotasDaCentral() throws Exception {
        for (String rota : List.of("/summary", "/map", "/alerts")) {
            mockMvc.perform(get(BASE + rota))
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(operationsService, alertService);
    }

    @Test
    void deveRetornar200QuandoOperadorConsultaAsTresRotas() throws Exception {
        when(operationsService.summary()).thenReturn(summary());
        when(operationsService.map(any(MapFilter.class))).thenReturn(mapa());
        when(alertService.alerts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alerta()), PageRequest.of(0, 20), 1));

        for (String rota : List.of("/summary", "/map", "/alerts")) {
            mockMvc.perform(get(BASE + rota).with(operador()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void deveRetornar405QuandoPostNoResumo() throws Exception {
        mockMvc.perform(post(BASE + "/summary").with(gestor()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(operationsService);
    }
}
