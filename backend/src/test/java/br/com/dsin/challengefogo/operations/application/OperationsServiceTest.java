package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentMarkerView;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusCount;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMarkerView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusCount;
import br.com.dsin.challengefogo.operations.application.dto.AgentMarkerDto;
import br.com.dsin.challengefogo.operations.application.dto.AgentMarkerDto.PositionSource;
import br.com.dsin.challengefogo.operations.application.dto.MapDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.MapFilter;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationsServiceTest {

    // 01:30 em UTC ainda é dia 26 em Presidente Prudente: "hoje" tem que sair no fuso da cidade.
    private static final Instant NOW = Instant.parse("2026-10-27T01:30:00Z");
    private static final Set<AgentStatus> ACTIVE = EnumSet.of(AgentStatus.AVAILABLE, AgentStatus.ON_OCCURRENCE);

    private final EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final OperationsService service = new OperationsService(equipmentRepository, agentRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    // ---------- resumo ----------

    @Test
    void deveMontarIndicadoresComTodasAsSituacoesERegioesQuandoResume() {
        when(equipmentRepository.countByStatus()).thenReturn(List.of(
                new EquipmentStatusCount(EquipmentStatus.OPERATIONAL, 10),
                new EquipmentStatusCount(EquipmentStatus.MALFUNCTION, 3)));
        when(equipmentRepository.countByMaintenanceNeed(MaintenanceNeed.IMMEDIATE)).thenReturn(4L);
        when(equipmentRepository.countOverdueMaintenance(LocalDate.of(2026, 10, 26))).thenReturn(2L);
        when(equipmentRepository.countUpcomingMaintenance(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2)))
                .thenReturn(5L);
        when(equipmentRepository.countByRegionWithStatus(EquipmentStatus.MALFUNCTION))
                .thenReturn(List.of(new RegionCount(Region.NORTH, 3)));
        when(agentRepository.countByStatus()).thenReturn(List.of(
                new AgentStatusCount(AgentStatus.AVAILABLE, 5),
                new AgentStatusCount(AgentStatus.ON_OCCURRENCE, 2),
                new AgentStatusCount(AgentStatus.OFF_DUTY, 6)));
        when(agentRepository.countByRegionWithStatusIn(ACTIVE))
                .thenReturn(List.of(new RegionCount(Region.CENTER, 7)));

        SummaryDtoResponse summary = service.summary();

        assertThat(summary.equipments().total()).isEqualTo(13);
        assertThat(summary.equipments().byStatus()).hasSize(EquipmentStatus.values().length)
                .containsEntry(EquipmentStatus.STOLEN, 0L).containsEntry(EquipmentStatus.MALFUNCTION, 3L);
        assertThat(summary.equipments().immediateMaintenance()).isEqualTo(4);
        assertThat(summary.equipments().overdueMaintenance()).isEqualTo(2);
        assertThat(summary.equipments().upcomingMaintenance()).isEqualTo(5);
        assertThat(summary.agents().total()).isEqualTo(13);
        assertThat(summary.agents().active()).isEqualTo(7);
        assertThat(summary.agents().byStatus()).containsEntry(AgentStatus.ON_LEAVE, 0L);
        assertThat(summary.regions()).containsExactly(
                new SummaryDtoResponse.RegionSummary(Region.CENTER, 0, 7),
                new SummaryDtoResponse.RegionSummary(Region.NORTH, 3, 0),
                new SummaryDtoResponse.RegionSummary(Region.SOUTH, 0, 0),
                new SummaryDtoResponse.RegionSummary(Region.EAST, 0, 0),
                new SummaryDtoResponse.RegionSummary(Region.WEST, 0, 0));
        assertThat(summary.generatedAt()).isEqualTo(NOW);
    }

    // ---------- mapa (regras 1 e 2) ----------

    @Test
    void deveUsarPosicaoAtualQuandoAgenteTemPosicao() {
        when(agentRepository.findMarkers(any(), any(), any(), any(), any())).thenReturn(List.of(agent(true)));

        AgentMarkerDto marker = service.map(emptyFilter()).agents().getFirst();

        assertThat(marker.positionSource()).isEqualTo(PositionSource.CURRENT);
        assertThat(marker.latitude()).isEqualByComparingTo("-22.100000");
        assertThat(marker.positionUpdatedAt()).isEqualTo(Instant.parse("2026-10-26T12:00:00Z"));
    }

    @Test
    void deveUsarBaseQuandoAgenteNaoTemPosicao() {
        when(agentRepository.findMarkers(any(), any(), any(), any(), any())).thenReturn(List.of(agent(false)));

        AgentMarkerDto marker = service.map(emptyFilter()).agents().getFirst();

        assertThat(marker.positionSource()).isEqualTo(PositionSource.BASE);
        assertThat(marker.latitude()).isEqualByComparingTo("-22.125600");
        assertThat(marker.positionUpdatedAt()).isNull();
    }

    @Test
    void deveBuscarSoAgentesEmAtividadeQuandoSemFiltroDeSituacao() {
        service.map(emptyFilter());

        verify(agentRepository).findMarkers(eq(ACTIVE), isNull(), isNull(), isNull(),
                eq(Limit.of(OperationsService.MARKER_LIMIT + 1)));
    }

    @Test
    void deveRepassarFiltrosQuandoInformados() {
        MapFilter filter = new MapFilter(EquipmentType.RADAR, EquipmentStatus.MALFUNCTION, AgentStatus.OFF_DUTY,
                Region.NORTH, Agency.MUNICIPAL_TRAFFIC, "  Vila Marcondes ");

        service.map(filter);

        verify(equipmentRepository).findMarkers(EquipmentType.RADAR, EquipmentStatus.MALFUNCTION, Region.NORTH,
                Agency.MUNICIPAL_TRAFFIC, "vila marcondes", Limit.of(OperationsService.MARKER_LIMIT + 1));
        verify(agentRepository).findMarkers(Set.of(AgentStatus.OFF_DUTY), Region.NORTH, Agency.MUNICIPAL_TRAFFIC,
                "vila marcondes", Limit.of(OperationsService.MARKER_LIMIT + 1));
    }

    @Test
    void deveIgnorarBairroQuandoVemEmBranco() {
        service.map(new MapFilter(null, null, null, null, null, "   "));

        verify(equipmentRepository).findMarkers(isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(Limit.of(OperationsService.MARKER_LIMIT + 1)));
    }

    // ---------- bairros (filtro do mapa) ----------

    @Test
    void deveListarBairrosDeEquipamentosEBasesSemRepeticaoEmOrdemQuandoConsulta() {
        when(equipmentRepository.findNeighborhoods()).thenReturn(List.of("Vila Marcondes", "Centro"));
        when(agentRepository.findBaseNeighborhoods()).thenReturn(List.of("Centro", "Ana Jacinta"));

        List<String> neighborhoods = service.neighborhoods();

        assertThat(neighborhoods).containsExactly("Ana Jacinta", "Centro", "Vila Marcondes");
    }

    @Test
    void deveAvisarECortarQuandoCamadaPassaDoTeto() {
        EquipmentMarkerView marker = new EquipmentMarkerView(UUID.randomUUID(), EquipmentType.RADAR, "RAD-1",
                EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, BigDecimal.ONE, BigDecimal.ONE, Region.CENTER);
        when(equipmentRepository.findMarkers(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.nCopies(OperationsService.MARKER_LIMIT + 1, marker));

        MapDtoResponse map = service.map(emptyFilter());

        assertThat(map.truncated()).isTrue();
        assertThat(map.equipments()).hasSize(OperationsService.MARKER_LIMIT);
    }

    @Test
    void naoDeveAvisarQuandoCabeNoTeto() {
        MapDtoResponse map = service.map(emptyFilter());

        assertThat(map.truncated()).isFalse();
    }

    private static MapFilter emptyFilter() {
        return new MapFilter(null, null, null, null, null, null);
    }

    private static AgentMarkerView agent(boolean withPosition) {
        return new AgentMarkerView(UUID.randomUUID(), "Ana", AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                withPosition ? new BigDecimal("-22.100000") : null, withPosition ? new BigDecimal("-51.300000") : null,
                withPosition ? Instant.parse("2026-10-26T12:00:00Z") : null,
                new BigDecimal("-22.125600"), new BigDecimal("-51.388900"), Region.CENTER, "GMA1B23");
    }
}
