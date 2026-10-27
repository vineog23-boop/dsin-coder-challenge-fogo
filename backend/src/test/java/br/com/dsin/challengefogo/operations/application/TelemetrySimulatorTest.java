package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetrySimulatorTest {

    private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");

    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final TelemetrySimulator simulator = new TelemetrySimulator(agentRepository,
            Clock.fixed(NOW, ZoneOffset.UTC), new Random(42));

    @Test
    void deveMoverAgenteEmAtividadeEGuardarHorarioQuandoExecuta() {
        AgentEntity agent = agent(AgentStatus.AVAILABLE);
        when(agentRepository.findByStatusIn(OperationsService.ACTIVE_AGENTS)).thenReturn(List.of(agent));

        simulator.tick();

        assertThat(agent.getCurrentLatitude()).isNotNull();
        assertThat(agent.getCurrentLongitude()).isNotNull();
        assertThat(distanceFromBase(agent)).isPositive().isLessThanOrEqualTo(TelemetrySimulator.STEP_DEGREES);
        assertThat(agent.getCurrentLatitude().scale()).isEqualTo(6);
        assertThat(agent.getPositionUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void naoDeveSairDoRaioDaBaseQuandoExecutaMuitasVezes() {
        AgentEntity agent = agent(AgentStatus.ON_OCCURRENCE);
        when(agentRepository.findByStatusIn(OperationsService.ACTIVE_AGENTS)).thenReturn(List.of(agent));

        double farthest = 0;
        for (int i = 0; i < 2_000; i++) {
            simulator.tick();
            farthest = Math.max(farthest, distanceFromBase(agent));
        }

        // tolerância do arredondamento para 6 casas
        assertThat(farthest).isLessThanOrEqualTo(TelemetrySimulator.MAX_DISTANCE_DEGREES + 0.000002);
        assertThat(farthest).isGreaterThan(TelemetrySimulator.STEP_DEGREES);
    }

    @Test
    void deveBuscarSoAgentesEmAtividade() {
        simulator.tick();

        verify(agentRepository).findByStatusIn(OperationsService.ACTIVE_AGENTS);
    }

    @Test
    void naoDeveMoverAgenteForaDeServicoMesmoQueVenhaNaLista() {
        AgentEntity offDuty = agent(AgentStatus.OFF_DUTY);
        when(agentRepository.findByStatusIn(OperationsService.ACTIVE_AGENTS)).thenReturn(List.of(offDuty));

        simulator.tick();

        assertThat(offDuty.getCurrentLatitude()).isNull();
        assertThat(offDuty.getPositionUpdatedAt()).isNull();
    }

    private static AgentEntity agent(AgentStatus status) {
        return new AgentEntity("Ana", "GM-1", AgentType.MUNICIPAL_GUARD, status,
                new Location(null, "Rua A", null, "Centro", "Presidente Prudente", "SP", Region.CENTER,
                        new BigDecimal("-22.125600"), new BigDecimal("-51.388900")), null);
    }

    private static double distanceFromBase(AgentEntity agent) {
        double dLat = agent.getCurrentLatitude().doubleValue() - agent.getBaseLocation().getLatitude().doubleValue();
        double dLng = agent.getCurrentLongitude().doubleValue() - agent.getBaseLocation().getLongitude().doubleValue();
        return Math.hypot(dLat, dLng);
    }
}
