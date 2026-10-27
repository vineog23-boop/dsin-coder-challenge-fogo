package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentAlertView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.persistence.StatusSinceView;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse.Kind;
import br.com.dsin.challengefogo.operations.application.dto.AlertDtoResponse.Severity;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Alertas da central, calculados a cada consulta a partir do estado atual (regra 5 da spec operations). */
@Service
public class AlertService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<Region, String> REGION_NAME = new EnumMap<>(Map.of(Region.CENTER, "Centro",
            Region.NORTH, "Norte", Region.SOUTH, "Sul", Region.EAST, "Leste", Region.WEST, "Oeste"));

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusHistoryRepository statusHistoryRepository;
    private final AgentRepository agentRepository;
    private final Clock clock;

    public AlertService(EquipmentRepository equipmentRepository,
                        EquipmentStatusHistoryRepository statusHistoryRepository, AgentRepository agentRepository,
                        Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.agentRepository = agentRepository;
        this.clock = clock;
    }

    /**
     * Ordem: severidade (alta primeiro) e depois o mais antigo; alertas de região, que não têm "desde",
     * abrem a lista da sua severidade. A lista é pequena (dezenas), então a paginação é feita em memória.
     */
    @Transactional(readOnly = true)
    public Page<AlertDtoResponse> alerts(Pageable pageable) {
        LocalDate today = LocalDate.now(clock.withZone(OperationsService.CITY_ZONE));
        List<AlertDtoResponse> alerts = new ArrayList<>(regionsWithoutActiveAgent());
        alerts.addAll(equipmentAlerts(today));
        alerts.sort(Comparator.comparing(AlertDtoResponse::severity)
                .thenComparing(AlertDtoResponse::since, Comparator.nullsFirst(Comparator.naturalOrder())));

        int from = (int) Math.min(pageable.getOffset(), alerts.size());
        int to = Math.min(from + pageable.getPageSize(), alerts.size());
        return new PageImpl<>(alerts.subList(from, to), pageable, alerts.size());
    }

    private List<AlertDtoResponse> regionsWithoutActiveAgent() {
        Set<Region> covered = EnumSet.noneOf(Region.class);
        agentRepository.countByRegionWithStatusIn(OperationsService.ACTIVE_AGENTS).stream()
                .filter(count -> count.total() > 0)
                .forEach(count -> covered.add(count.region()));

        return Arrays.stream(Region.values())
                .filter(region -> !covered.contains(region))
                .map(region -> new AlertDtoResponse(Kind.REGION_WITHOUT_ACTIVE_AGENT, Severity.HIGH,
                        "Região " + REGION_NAME.get(region) + " sem agente em atividade.", region, null, null, null))
                .toList();
    }

    private List<AlertDtoResponse> equipmentAlerts(LocalDate today) {
        List<EquipmentAlertView> candidates = equipmentRepository.findAlertCandidates(today);
        Map<UUID, Instant> malfunctionSince = lastEntryInto(EquipmentStatus.MALFUNCTION, candidates);
        Map<UUID, Instant> stolenSince = lastEntryInto(EquipmentStatus.STOLEN, candidates);
        List<AlertDtoResponse> alerts = new ArrayList<>();

        for (EquipmentAlertView equipment : candidates) {
            if (equipment.status() == EquipmentStatus.MALFUNCTION) {
                alerts.add(equipmentAlert(Kind.EQUIPMENT_MALFUNCTION, Severity.HIGH, equipment,
                        "Equipamento " + equipment.serialNumber() + " com falha.",
                        malfunctionSince.get(equipment.id())));
            }
            if (equipment.status() == EquipmentStatus.STOLEN) {
                alerts.add(equipmentAlert(Kind.EQUIPMENT_STOLEN, Severity.HIGH, equipment,
                        "Equipamento " + equipment.serialNumber() + " roubado.", stolenSince.get(equipment.id())));
            }
            if (isOverdue(equipment, today)) {
                alerts.add(equipmentAlert(Kind.MAINTENANCE_OVERDUE, Severity.MEDIUM, equipment,
                        "Manutenção do equipamento " + equipment.serialNumber() + " atrasada desde "
                                + DATE.format(equipment.nextMaintenanceDate()) + ".",
                        equipment.nextMaintenanceDate().atStartOfDay(OperationsService.CITY_ZONE).toInstant()));
            }
        }
        return alerts;
    }

    /** Regra 3: agendada, vencida e não desativada (a consulta já traz só candidatos, mas um pode ser só falha). */
    private static boolean isOverdue(EquipmentAlertView equipment, LocalDate today) {
        return equipment.maintenanceNeed() == MaintenanceNeed.SCHEDULED && equipment.nextMaintenanceDate() != null
                && equipment.nextMaintenanceDate().isBefore(today)
                && equipment.status() != EquipmentStatus.DEACTIVATED;
    }

    private Map<UUID, Instant> lastEntryInto(EquipmentStatus status, List<EquipmentAlertView> candidates) {
        List<UUID> ids = candidates.stream().filter(candidate -> candidate.status() == status)
                .map(EquipmentAlertView::id).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return statusHistoryRepository.findLastEntryInto(status, ids).stream()
                .collect(Collectors.toMap(StatusSinceView::equipmentId, StatusSinceView::since));
    }

    private static AlertDtoResponse equipmentAlert(Kind kind, Severity severity, EquipmentAlertView equipment,
                                                   String message, Instant since) {
        return new AlertDtoResponse(kind, severity, message, equipment.region(), equipment.id(), equipment.type(),
                since);
    }
}
