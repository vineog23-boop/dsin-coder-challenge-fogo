package br.com.dsin.challengefogo.agent.web;

import br.com.dsin.challengefogo.agent.application.AgentNotFoundException;
import br.com.dsin.challengefogo.agent.application.AgentService;
import br.com.dsin.challengefogo.agent.application.VehicleNotFoundException;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.AgentDtoResponse;
import br.com.dsin.challengefogo.agent.application.dto.AgentFilter;
import br.com.dsin.challengefogo.agent.application.dto.AgentStatusChangeDtoRequest;
import br.com.dsin.challengefogo.agent.application.dto.PositionDtoRequest;
import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.auth.application.TokenService;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.config.ClockConfig;
import br.com.dsin.challengefogo.shared.config.WebConfig;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
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
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(AgentController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class, TokenService.class, ClockConfig.class,
        WebConfig.class})
class AgentControllerTest {

    private static final String BASE = "/api/agents";
    private static final String BASE_LOCATION = """
            {"street":"Rua A","neighborhood":"Centro","city":"São Paulo","state":"SP","region":"NORTH",
             "latitude":-23.5,"longitude":-46.65}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentService agentService;

    private static RequestPostProcessor gestor() {
        return jwt().jwt(j -> j.subject("gestor")).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }

    private static RequestPostProcessor operador() {
        return jwt().jwt(j -> j.subject("operador")).authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"));
    }

    private static String json(String name, String baseLocation) {
        String base = baseLocation == null ? "" : ",\"baseLocation\":" + baseLocation;

        return """
                {"name":"%s","registrationNumber":"GCM-001","agentType":"MUNICIPAL_GUARD"%s}
                """.formatted(name, base);
    }

    private static AgentDtoResponse response(UUID id) {
        LocationDto location = new LocationDto(null, "Rua A", null, "Centro", "São Paulo", "SP", Region.NORTH,
                new BigDecimal("-23.5"), new BigDecimal("-46.65"));

        return new AgentDtoResponse(id, "Ana Souza", "GCM-001", AgentType.MUNICIPAL_GUARD, Agency.MUNICIPAL_GUARD,
                AgentStatus.AVAILABLE, location, null, null, null, null, Instant.parse("2026-10-26T15:00:00Z"),
                Instant.parse("2026-10-26T15:00:00Z"));
    }

    @Test
    void deveResponder201ComLocationQuandoPostValido() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.create(any(AgentDtoRequest.class))).thenReturn(response(id));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("Ana Souza", BASE_LOCATION)));

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(BASE + "/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.registrationNumber").value("GCM-001"));
    }

    @Test
    void deveResponder400QuandoNomeEmBrancoEBaseAusente() throws Exception {
        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("  ", null)));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION.toString()))
                .andExpect(jsonPath("$.errors[?(@.field=='name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='baseLocation')]").exists());
        verifyNoInteractions(agentService);
    }

    @Test
    void deveResponder409QuandoServiceLancaConflictException() throws Exception {
        when(agentService.create(any(AgentDtoRequest.class))).thenThrow(new ConflictException("Duplicada."));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("Ana Souza", BASE_LOCATION)));

        result.andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.CONFLICT.toString()));
    }

    @Test
    void deveResponder404QuandoAgenteNaoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.findById(id)).thenThrow(new AgentNotFoundException(id));

        var result = mockMvc.perform(get(BASE + "/" + id).with(gestor()));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()));
    }

    @Test
    void deveResponder404QuandoViaturaNaoExiste() throws Exception {
        when(agentService.create(any(AgentDtoRequest.class)))
                .thenThrow(new VehicleNotFoundException(UUID.randomUUID()));

        var result = mockMvc.perform(post(BASE).with(gestor()).contentType(MediaType.APPLICATION_JSON)
                .content(json("Ana Souza", BASE_LOCATION)));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()));
    }

    @Test
    void deveResponder422QuandoRegraDeNegocioFalha() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.changeStatus(eq(id), any(AgentStatusChangeDtoRequest.class), any()))
                .thenThrow(new BusinessRuleException("Mesma situação."));

        var result = mockMvc.perform(patch(BASE + "/" + id + "/status").with(gestor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AVAILABLE\"}"));

        result.andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemTypes.BUSINESS_RULE.toString()));
    }

    @Test
    void devePermitirGetQuandoOperador() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.findById(id)).thenReturn(response(id));

        var result = mockMvc.perform(get(BASE + "/" + id).with(operador()));

        result.andExpect(status().isOk());
    }

    @Test
    void devePermitirPatchDePosicaoQuandoOperador() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.updatePosition(eq(id), any(PositionDtoRequest.class))).thenReturn(response(id));

        var result = mockMvc.perform(patch(BASE + "/" + id + "/position").with(operador())
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":-23.5,\"longitude\":-46.65}"));

        result.andExpect(status().isOk());
        verify(agentService).updatePosition(id,
                new PositionDtoRequest(new BigDecimal("-23.5"), new BigDecimal("-46.65")));
    }

    @Test
    void deveResponder400QuandoLatitudeDaPosicaoFicaForaDoIntervalo() throws Exception {
        UUID id = UUID.randomUUID();

        var result = mockMvc.perform(patch(BASE + "/" + id + "/position").with(operador())
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":91,\"longitude\":-46.65}"));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='latitude')]").exists());
        verifyNoInteractions(agentService);
    }

    @Test
    void naoDevePermitirPatchDeStatusQuandoOperador() throws Exception {
        UUID id = UUID.randomUUID();

        var result = mockMvc.perform(patch(BASE + "/" + id + "/status").with(operador())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OFF_DUTY\"}"));

        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(agentService);
    }

    @Test
    void naoDevePermitirPostQuandoOperador() throws Exception {
        var result = mockMvc.perform(post(BASE).with(operador()).contentType(MediaType.APPLICATION_JSON)
                .content(json("Ana Souza", BASE_LOCATION)));

        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(agentService);
    }

    @Test
    void deveResponder401QuandoSemToken() throws Exception {
        var result = mockMvc.perform(get(BASE));

        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(agentService);
    }

    @Test
    void devePassarSubjectDoTokenAoServiceQuandoPatchDeStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(agentService.changeStatus(eq(id), any(AgentStatusChangeDtoRequest.class), eq("gestor")))
                .thenReturn(response(id));

        var result = mockMvc.perform(patch(BASE + "/" + id + "/status").with(gestor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OFF_DUTY\",\"note\":\"fim\"}"));

        result.andExpect(status().isOk());
        verify(agentService).changeStatus(id, new AgentStatusChangeDtoRequest(AgentStatus.OFF_DUTY, "fim"),
                "gestor");
    }

    @Test
    void deveSerializarPaginaEPassarFiltrosQuandoListagem() throws Exception {
        when(agentService.search(any(AgentFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(UUID.randomUUID())), PageRequest.of(0, 20), 1));

        var result = mockMvc.perform(get(BASE).with(operador())
                .param("agency", "MUNICIPAL_GUARD").param("status", "AVAILABLE").param("region", "NORTH")
                .param("agentType", "MUNICIPAL_GUARD").param("search", "ana"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));
        ArgumentCaptor<AgentFilter> filter = ArgumentCaptor.forClass(AgentFilter.class);
        verify(agentService).search(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue()).isEqualTo(new AgentFilter(AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                Agency.MUNICIPAL_GUARD, Region.NORTH, "ana"));
    }

    @Test
    void deveOrdenarPorNomeAscQuandoSemSortNaListagem() throws Exception {
        when(agentService.search(any(AgentFilter.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE).with(gestor())).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(agentService).search(any(AgentFilter.class), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }
}
