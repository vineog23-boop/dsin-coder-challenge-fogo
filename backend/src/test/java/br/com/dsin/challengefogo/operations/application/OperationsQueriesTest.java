package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.TestcontainersConfiguration;
import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentMarkerView;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusCount;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentAlertView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMarkerView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusCount;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
import br.com.dsin.challengefogo.equipment.persistence.StatusSinceView;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas de agregação da central contra PostgreSQL real, com um conjunto pequeno e conhecido de dados. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OperationsQueriesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final EnumSet<AgentStatus> ACTIVE = EnumSet.of(AgentStatus.AVAILABLE, AgentStatus.ON_OCCURRENCE);

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentStatusHistoryRepository historyRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private EntityManager entityManager;

    private RadarEntity brokenNorth;
    private EquipmentEntity overdueSouth;

    @BeforeEach
    void setUp() {
        brokenNorth = equipmentRepository.save(radar("RAD-1", EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE,
                null, Region.NORTH));
        equipmentRepository.save(radar("RAD-2", EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, Region.NORTH));
        overdueSouth = equipmentRepository.save(gate("CAN-1", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                TODAY.minusDays(3), Region.SOUTH));
        equipmentRepository.save(gate("CAN-2", EquipmentStatus.DEACTIVATED, MaintenanceNeed.SCHEDULED,
                TODAY.minusDays(30), Region.SOUTH));
        equipmentRepository.save(gate("CAN-3", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                TODAY, Region.EAST));
        VehicleEntity car = equipmentRepository.save(new VehicleEntity(EquipmentType.MUNICIPAL_GUARD_VEHICLE, "VEI-1",
                "Marca", "V1", Agency.MUNICIPAL_GUARD, EquipmentStatus.OPERATIONAL, MaintenanceNeed.NONE, null, null,
                location(Region.CENTER), "GMA1B23", 4, false));

        AgentEntity withPosition = new AgentEntity("Ana", "GM-1", AgentType.MUNICIPAL_GUARD, AgentStatus.AVAILABLE,
                location(Region.CENTER), car);
        withPosition.updatePosition(new BigDecimal("-22.100000"), new BigDecimal("-51.300000"),
                Instant.parse("2026-10-26T12:00:00Z"));
        agentRepository.save(withPosition);
        agentRepository.save(new AgentEntity("Bruno", "PM-1", AgentType.MILITARY_POLICE, AgentStatus.ON_OCCURRENCE,
                location(Region.NORTH), null));
        agentRepository.save(new AgentEntity("Carla", "AT-1", AgentType.TRAFFIC_AGENT, AgentStatus.OFF_DUTY,
                location(Region.NORTH), null));

        historyRepository.save(new EquipmentStatusHistoryEntity(brokenNorth.getId(), EquipmentStatus.OPERATIONAL,
                EquipmentStatus.MALFUNCTION, Instant.parse("2026-10-01T10:00:00Z"), "gestor", null));
        historyRepository.save(new EquipmentStatusHistoryEntity(brokenNorth.getId(), EquipmentStatus.MALFUNCTION,
                EquipmentStatus.OPERATIONAL, Instant.parse("2026-10-02T10:00:00Z"), "gestor", null));
        historyRepository.save(new EquipmentStatusHistoryEntity(brokenNorth.getId(), EquipmentStatus.OPERATIONAL,
                EquipmentStatus.MALFUNCTION, Instant.parse("2026-10-20T22:00:00Z"), "gestor", null));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deveContarEquipamentosPorSituacaoQuandoAgrega() {
        Map<EquipmentStatus, Long> byStatus = equipmentRepository.countByStatus().stream()
                .collect(Collectors.toMap(EquipmentStatusCount::status, EquipmentStatusCount::total));

        assertThat(byStatus).containsEntry(EquipmentStatus.OPERATIONAL, 4L)
                .containsEntry(EquipmentStatus.MALFUNCTION, 1L)
                .containsEntry(EquipmentStatus.DEACTIVATED, 1L);
    }

    @Test
    void deveContarSoManutencaoVencidaDeEquipamentoEmUsoQuandoConsultaAtraso() {
        long overdue = equipmentRepository.countOverdueMaintenance(TODAY);
        long immediate = equipmentRepository.countByMaintenanceNeed(MaintenanceNeed.IMMEDIATE);

        assertThat(overdue).isEqualTo(1);
        assertThat(immediate).isEqualTo(1);
    }

    @Test
    void deveAgruparFalhasEAgentesEmAtividadePorRegiao() {
        List<RegionCount> failures = equipmentRepository.countByRegionWithStatus(EquipmentStatus.MALFUNCTION);
        List<RegionCount> activeAgents = agentRepository.countByRegionWithStatusIn(ACTIVE);
        Map<AgentStatus, Long> agentsByStatus = agentRepository.countByStatus().stream()
                .collect(Collectors.toMap(AgentStatusCount::status, AgentStatusCount::total));

        assertThat(failures).containsExactly(new RegionCount(Region.NORTH, 1));
        assertThat(activeAgents).containsExactlyInAnyOrder(new RegionCount(Region.CENTER, 1),
                new RegionCount(Region.NORTH, 1));
        assertThat(agentsByStatus).containsEntry(AgentStatus.OFF_DUTY, 1L).containsEntry(AgentStatus.AVAILABLE, 1L);
    }

    @Test
    void deveFiltrarMarcadoresDeEquipamentoQuandoCombinaFiltros() {
        List<EquipmentMarkerView> all = equipmentRepository.findMarkers(null, null, null, null, Limit.of(100));
        List<EquipmentMarkerView> filtered = equipmentRepository.findMarkers(EquipmentType.RADAR,
                EquipmentStatus.MALFUNCTION, Region.NORTH, Agency.MUNICIPAL_TRAFFIC, Limit.of(100));
        List<EquipmentMarkerView> limited = equipmentRepository.findMarkers(null, null, null, null, Limit.of(2));

        assertThat(all).hasSize(6);
        assertThat(filtered).extracting(EquipmentMarkerView::serialNumber).containsExactly("RAD-1");
        assertThat(filtered.getFirst().latitude()).isEqualByComparingTo("-22.125600");
        assertThat(limited).hasSize(2);
    }

    @Test
    void deveTrazerMarcadoresDeAgenteComBasePosicaoEPlacaQuandoFiltraPorSituacao() {
        List<AgentMarkerView> active = agentRepository.findMarkers(ACTIVE, null, null, Limit.of(100));
        List<AgentMarkerView> northGuard = agentRepository.findMarkers(EnumSet.allOf(AgentStatus.class), Region.NORTH,
                Agency.MILITARY_POLICE, Limit.of(100));

        assertThat(active).extracting(AgentMarkerView::name).containsExactly("Ana", "Bruno");
        AgentMarkerView ana = active.getFirst();
        assertThat(ana.currentLatitude()).isEqualByComparingTo("-22.1");
        assertThat(ana.baseLatitude()).isEqualByComparingTo("-22.1256");
        assertThat(ana.vehiclePlate()).isEqualTo("GMA1B23");
        assertThat(active.get(1).vehiclePlate()).isNull();
        assertThat(northGuard).extracting(AgentMarkerView::name).containsExactly("Bruno");
    }

    @Test
    void deveTrazerCandidatosAAlertaEUltimaEntradaEmFalha() {
        List<EquipmentAlertView> candidates = equipmentRepository.findAlertCandidates(TODAY);
        List<StatusSinceView> since = historyRepository.findLastEntryInto(EquipmentStatus.MALFUNCTION,
                List.of(brokenNorth.getId(), overdueSouth.getId()));

        assertThat(candidates).extracting(EquipmentAlertView::serialNumber).containsExactlyInAnyOrder("RAD-1", "CAN-1");
        assertThat(since).containsExactly(new StatusSinceView(brokenNorth.getId(),
                Instant.parse("2026-10-20T22:00:00Z")));
    }

    private static Location location(Region region) {
        return new Location("19010000", "Rua A", null, "Centro", "Presidente Prudente", "SP", region,
                new BigDecimal("-22.125600"), new BigDecimal("-51.388900"));
    }

    private static RadarEntity radar(String serial, EquipmentStatus status, MaintenanceNeed need, LocalDate next,
                                     Region region) {
        return new RadarEntity(serial, "Marca", "R1", Agency.MUNICIPAL_TRAFFIC, status, need, next, null,
                location(region), RadarModality.RED_LIGHT, null, 2);
    }

    private static EquipmentEntity gate(String serial, EquipmentStatus status, MaintenanceNeed need, LocalDate next,
                                        Region region) {
        return new EquipmentEntity(EquipmentType.AUTOMATED_GATE, serial, "Marca", "G1", Agency.MUNICIPAL_TRAFFIC,
                status, need, next, null, location(region));
    }
}
