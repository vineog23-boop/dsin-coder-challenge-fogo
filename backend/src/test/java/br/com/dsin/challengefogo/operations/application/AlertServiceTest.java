package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentAlertView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.persistence.StatusSinceView;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse.Kind;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse.Severity;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final LocalDate IN_A_WEEK = LocalDate.of(2026, 11, 2);

    private final EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    private final EquipmentStatusHistoryRepository historyRepository = mock(EquipmentStatusHistoryRepository.class);
    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final AlertService service = new AlertService(equipmentRepository, historyRepository, agentRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID oldFailure = UUID.randomUUID();
    private final UUID recentFailure = UUID.randomUUID();
    private final UUID stolen = UUID.randomUUID();
    private final UUID overdue = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Toda região tem agente em atividade, salvo quando o teste disser o contrário.
        when(agentRepository.countByRegionWithStatusIn(OperationsService.ACTIVE_AGENTS)).thenReturn(List.of(
                new RegionCount(Region.CENTER, 2), new RegionCount(Region.NORTH, 1), new RegionCount(Region.SOUTH, 1),
                new RegionCount(Region.EAST, 3), new RegionCount(Region.WEST, 1)));
    }

    @Test
    void deveOrdenarPorSeveridadeEDepoisPeloMaisAntigo() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(overdue, "CAN-1", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 20), Region.SOUTH),
                candidate(recentFailure, "RAD-2", EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE, null,
                        Region.NORTH),
                candidate(stolen, "PAT-1", EquipmentStatus.STOLEN, MaintenanceNeed.NONE, null, Region.EAST),
                candidate(oldFailure, "RAD-1", EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE, null,
                        Region.NORTH)));
        when(historyRepository.findLastEntryInto(eq(EquipmentStatus.MALFUNCTION), any())).thenReturn(List.of(
                new StatusSinceView(oldFailure, Instant.parse("2026-10-01T10:00:00Z")),
                new StatusSinceView(recentFailure, Instant.parse("2026-10-25T22:00:00Z"))));
        when(historyRepository.findLastEntryInto(eq(EquipmentStatus.STOLEN), any())).thenReturn(List.of(
                new StatusSinceView(stolen, Instant.parse("2026-10-23T08:00:00Z"))));

        List<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20)).getContent();

        assertThat(alerts).extracting(AlertDtoResponse::kind).containsExactly(Kind.EQUIPMENT_MALFUNCTION,
                Kind.EQUIPMENT_STOLEN, Kind.EQUIPMENT_MALFUNCTION, Kind.MAINTENANCE_OVERDUE);
        assertThat(alerts).extracting(AlertDtoResponse::equipmentId).containsExactly(oldFailure, stolen,
                recentFailure, overdue);
        assertThat(alerts).extracting(AlertDtoResponse::severity).containsExactly(Severity.HIGH, Severity.HIGH,
                Severity.HIGH, Severity.MEDIUM);
    }

    @Test
    void deveMontarAlertaDeFalhaComDesdeERegiao() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(oldFailure, "RAD-1", EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE, null,
                        Region.NORTH)));
        when(historyRepository.findLastEntryInto(eq(EquipmentStatus.MALFUNCTION), any())).thenReturn(List.of(
                new StatusSinceView(oldFailure, Instant.parse("2026-10-01T10:00:00Z"))));

        AlertDtoResponse alert = service.alerts(PageRequest.of(0, 20)).getContent().getFirst();

        assertThat(alert.since()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(alert.region()).isEqualTo(Region.NORTH);
        assertThat(alert.equipmentType()).isEqualTo(EquipmentType.RADAR);
        assertThat(alert.message()).contains("RAD-1").contains("falha");
    }

    @Test
    void deveUsarDataDaManutencaoComoDesdeQuandoAtrasada() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(overdue, "CAN-1", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 20), Region.SOUTH)));

        AlertDtoResponse alert = service.alerts(PageRequest.of(0, 20)).getContent().getFirst();

        assertThat(alert.kind()).isEqualTo(Kind.MAINTENANCE_OVERDUE);
        assertThat(alert.since()).isEqualTo(Instant.parse("2026-10-20T03:00:00Z")); // meia-noite em Prudente
        assertThat(alert.message()).contains("CAN-1").contains("20/10/2026");
    }

    @Test
    void deveGerarDoisAlertasQuandoEquipamentoComFalhaTambemEstaComManutencaoAtrasada() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(oldFailure, "RAD-1", EquipmentStatus.MALFUNCTION, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 10), Region.NORTH)));

        List<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20)).getContent();

        assertThat(alerts).extracting(AlertDtoResponse::kind)
                .containsExactly(Kind.EQUIPMENT_MALFUNCTION, Kind.MAINTENANCE_OVERDUE);
    }

    @Test
    void deveAlertarRegiaoSemAgenteEmAtividadeAntesDosEquipamentos() {
        when(agentRepository.countByRegionWithStatusIn(OperationsService.ACTIVE_AGENTS)).thenReturn(List.of(
                new RegionCount(Region.CENTER, 2), new RegionCount(Region.SOUTH, 1), new RegionCount(Region.EAST, 3),
                new RegionCount(Region.WEST, 1)));
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(oldFailure, "RAD-1", EquipmentStatus.MALFUNCTION, MaintenanceNeed.IMMEDIATE, null,
                        Region.NORTH)));
        when(historyRepository.findLastEntryInto(eq(EquipmentStatus.MALFUNCTION), any())).thenReturn(List.of(
                new StatusSinceView(oldFailure, Instant.parse("2026-10-01T10:00:00Z"))));

        List<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20)).getContent();

        assertThat(alerts.getFirst().kind()).isEqualTo(Kind.REGION_WITHOUT_ACTIVE_AGENT);
        assertThat(alerts.getFirst().region()).isEqualTo(Region.NORTH);
        assertThat(alerts.getFirst().equipmentId()).isNull();
        assertThat(alerts.getFirst().message()).contains("Norte");
        assertThat(alerts).hasSize(2);
    }

    @Test
    void deveAlertarManutencaoProximaComSeveridadeBaixaDepoisDasAtrasadasQuandoVenceEmAteSeteDias() {
        UUID dueSoon = UUID.randomUUID();
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(dueSoon, "CAN-9", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 30), Region.WEST),
                candidate(overdue, "CAN-1", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 20), Region.SOUTH)));

        List<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20)).getContent();

        assertThat(alerts).extracting(AlertDtoResponse::kind)
                .containsExactly(Kind.MAINTENANCE_OVERDUE, Kind.MAINTENANCE_DUE_SOON);
        AlertDtoResponse soon = alerts.get(1);
        assertThat(soon.severity()).isEqualTo(Severity.LOW);
        assertThat(soon.equipmentId()).isEqualTo(dueSoon);
        assertThat(soon.since()).isNull();
        assertThat(soon.message()).contains("CAN-9").contains("30/10/2026");
    }

    @Test
    void naoDeveAlertarManutencaoProximaQuandoEquipamentoEstaDesativado() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(UUID.randomUUID(), "CAN-9", EquipmentStatus.DEACTIVATED, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 30), Region.WEST)));

        Page<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20));

        assertThat(alerts.getTotalElements()).isZero();
    }

    @Test
    void naoDeveGerarAlertaQuandoNaoHaCandidatosETodasRegioesTemAgente() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of());

        Page<AlertDtoResponse> alerts = service.alerts(PageRequest.of(0, 20));

        assertThat(alerts.getTotalElements()).isZero();
    }

    @Test
    void devePaginarAlertasQuandoHaMaisQueOTamanhoDaPagina() {
        when(equipmentRepository.findAlertCandidates(IN_A_WEEK)).thenReturn(List.of(
                candidate(UUID.randomUUID(), "CAN-1", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 1), Region.SOUTH),
                candidate(UUID.randomUUID(), "CAN-2", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 2), Region.SOUTH),
                candidate(UUID.randomUUID(), "CAN-3", EquipmentStatus.OPERATIONAL, MaintenanceNeed.SCHEDULED,
                        LocalDate.of(2026, 10, 3), Region.SOUTH)));

        Page<AlertDtoResponse> second = service.alerts(PageRequest.of(1, 2));

        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(AlertDtoResponse::message)
                .singleElement().asString().contains("CAN-3");
    }

    private static EquipmentAlertView candidate(UUID id, String serial, EquipmentStatus status, MaintenanceNeed need,
                                                LocalDate next, Region region) {
        EquipmentType type = serial.startsWith("RAD") ? EquipmentType.RADAR
                : serial.startsWith("PAT") ? EquipmentType.ELECTRIC_SCOOTER : EquipmentType.AUTOMATED_GATE;
        return new EquipmentAlertView(id, type, serial, status, need, next, region);
    }
}
