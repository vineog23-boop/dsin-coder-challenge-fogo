package br.com.dsin.challengefogo.operations.application;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.persistence.AgentMarkerView;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMarkerView;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.operations.application.dto.AgentMarkerDto;
import br.com.dsin.challengefogo.operations.application.dto.EquipmentMarkerDto;
import br.com.dsin.challengefogo.operations.application.dto.MapDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.MapFilter;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse.AgentSummary;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse.EquipmentSummary;
import br.com.dsin.challengefogo.operations.application.dto.SummaryDtoResponse.RegionSummary;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Leituras da central: tudo derivado da Parte 1, sem tabela própria. */
@Service
public class OperationsService {

    /** Teto por camada do mapa (exceção aprovada à paginação: um mapa paginado não faz sentido). */
    static final int MARKER_LIMIT = 2_000;
    static final ZoneId CITY_ZONE = ZoneId.of("America/Sao_Paulo");
    /** Janela da "manutenção próxima": vence de hoje até daqui a uma semana. */
    static final int UPCOMING_MAINTENANCE_DAYS = 7;
    /** Agentes em atividade: disponíveis + em ocorrência (regra 4). */
    static final Set<AgentStatus> ACTIVE_AGENTS = Collections.unmodifiableSet(
            EnumSet.of(AgentStatus.AVAILABLE, AgentStatus.ON_OCCURRENCE));

    private final EquipmentRepository equipmentRepository;
    private final AgentRepository agentRepository;
    private final Clock clock;

    public OperationsService(EquipmentRepository equipmentRepository, AgentRepository agentRepository, Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.agentRepository = agentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SummaryDtoResponse summary() {
        LocalDate today = LocalDate.now(clock.withZone(CITY_ZONE));

        Map<EquipmentStatus, Long> equipmentsByStatus = zeros(EquipmentStatus.class);
        equipmentRepository.countByStatus().forEach(count -> equipmentsByStatus.put(count.status(), count.total()));
        Map<AgentStatus, Long> agentsByStatus = zeros(AgentStatus.class);
        agentRepository.countByStatus().forEach(count -> agentsByStatus.put(count.status(), count.total()));

        Map<Region, Long> malfunctionsByRegion = byRegion(
                equipmentRepository.countByRegionWithStatus(EquipmentStatus.MALFUNCTION));
        Map<Region, Long> activeAgentsByRegion = byRegion(agentRepository.countByRegionWithStatusIn(ACTIVE_AGENTS));
        List<RegionSummary> regions = Arrays.stream(Region.values())
                .map(region -> new RegionSummary(region, malfunctionsByRegion.getOrDefault(region, 0L),
                        activeAgentsByRegion.getOrDefault(region, 0L)))
                .toList();

        return new SummaryDtoResponse(
                new EquipmentSummary(sum(equipmentsByStatus), equipmentsByStatus,
                        equipmentRepository.countByMaintenanceNeed(MaintenanceNeed.IMMEDIATE),
                        equipmentRepository.countOverdueMaintenance(today),
                        equipmentRepository.countUpcomingMaintenance(today,
                                today.plusDays(UPCOMING_MAINTENANCE_DAYS))),
                new AgentSummary(sum(agentsByStatus),
                        ACTIVE_AGENTS.stream().mapToLong(agentsByStatus::get).sum(), agentsByStatus),
                regions,
                clock.instant());
    }

    /**
     * Pede um ponto a mais que o teto: se ele vier, a camada foi cortada e a resposta avisa ({@code truncated}).
     */
    @Transactional(readOnly = true)
    public MapDtoResponse map(MapFilter filter) {
        Limit limit = Limit.of(MARKER_LIMIT + 1);
        Set<AgentStatus> agentStatuses = filter.agentStatus() != null ? Set.of(filter.agentStatus()) : ACTIVE_AGENTS;
        String neighborhood = normalize(filter.neighborhood());

        List<EquipmentMarkerView> equipments = equipmentRepository.findMarkers(filter.equipmentType(),
                filter.equipmentStatus(), filter.region(), filter.agency(), neighborhood, limit);
        List<AgentMarkerView> agents = agentRepository.findMarkers(agentStatuses, filter.region(), filter.agency(),
                neighborhood, limit);

        boolean truncated = equipments.size() > MARKER_LIMIT || agents.size() > MARKER_LIMIT;
        return new MapDtoResponse(
                equipments.stream().limit(MARKER_LIMIT).map(EquipmentMarkerDto::new).toList(),
                agents.stream().limit(MARKER_LIMIT).map(AgentMarkerDto::new).toList(),
                truncated);
    }

    /** Bairros que existem nos dados (equipamentos e bases de agentes), para o filtro do mapa não ser no escuro. */
    @Transactional(readOnly = true)
    public List<String> neighborhoods() {
        Set<String> all = new TreeSet<>(equipmentRepository.findNeighborhoods());
        all.addAll(agentRepository.findBaseNeighborhoods());
        return List.copyOf(all);
    }

    /** O bairro é comparado sem diferenciar maiúsculas, como no filtro da lista de equipamentos. */
    private static String normalize(String neighborhood) {
        return neighborhood == null || neighborhood.isBlank() ? null : neighborhood.trim().toLowerCase(Locale.ROOT);
    }

    private static <E extends Enum<E>> Map<E, Long> zeros(Class<E> type) {
        Map<E, Long> map = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            map.put(value, 0L);
        }
        return map;
    }

    private static Map<Region, Long> byRegion(List<RegionCount> counts) {
        Map<Region, Long> map = new EnumMap<>(Region.class);
        counts.forEach(count -> map.put(count.region(), count.total()));
        return map;
    }

    private static long sum(Map<?, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }
}
