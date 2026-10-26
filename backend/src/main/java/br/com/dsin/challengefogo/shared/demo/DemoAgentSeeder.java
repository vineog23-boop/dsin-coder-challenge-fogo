package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.agent.domain.AgentType;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentRepository;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusHistoryEntity;
import br.com.dsin.challengefogo.agent.persistence.AgentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cria 40 agentes fictícios em Presidente Prudente com ~6 meses de histórico de situação, só com a tabela vazia.
 * Roda depois do seed de equipamentos ({@code @Order(2)}) porque vincula viaturas criadas por ele.
 *
 * <p><b>Padrão plantado para a Parte 3:</b> agentes com base no Norte e no Leste ficam livres por menos tempo
 * entre uma ocorrência e outra (1–4 h, contra 2–8 h), ou seja, atendem mais ocorrências por turno,
 * coerente com as regiões de mais falhas de equipamento. Turnos alternam livre/ocorrência até a saída
 * ({@value #END_OF_SHIFT_CHANCE} por intervalo livre); folga de 10–20 h; afastamento raro, de 5 a 15 dias.
 * Toda viatura vinculada respeita a regra 2 (viatura de agente, mesmo órgão, nem desativada nem roubada)
 * e só agente em serviço fica com viatura (regra 5).
 */
@Component
@Profile("demo")
@Order(2)
public class DemoAgentSeeder implements ApplicationRunner {

    private static final long SEED = 42L;
    private static final ZoneId CITY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String OPERATOR = "gestor";
    private static final double END_OF_SHIFT_CHANCE = 0.4;
    private static final double LEAVE_CHANCE = 0.01;

    private static final Map<AgentType, Integer> QUANTITIES = new EnumMap<>(Map.of(
            AgentType.TRAFFIC_AGENT, 14, AgentType.MUNICIPAL_GUARD, 12, AgentType.MILITARY_POLICE, 14));
    private static final Map<AgentType, String> REGISTRATION_PREFIX = new EnumMap<>(Map.of(
            AgentType.TRAFFIC_AGENT, "AT", AgentType.MUNICIPAL_GUARD, "GM", AgentType.MILITARY_POLICE, "PM"));

    private static final List<String> FIRST_NAMES = List.of("Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio",
            "Gabriela", "Henrique", "Isabela", "João", "Karina", "Lucas", "Marina", "Nelson", "Olívia", "Paulo",
            "Renata", "Sérgio", "Tatiane", "Vitor");
    private static final List<String> LAST_NAMES = List.of("Almeida", "Barbosa", "Cardoso", "Duarte", "Esteves",
            "Fonseca", "Gomes", "Lacerda", "Monteiro", "Nogueira", "Pacheco", "Queiroz", "Rezende", "Siqueira",
            "Teixeira", "Vasconcelos");

    private final AgentRepository agentRepository;
    private final AgentStatusHistoryRepository statusHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final Clock clock;

    public DemoAgentSeeder(AgentRepository agentRepository, AgentStatusHistoryRepository statusHistoryRepository,
                           EquipmentRepository equipmentRepository, Clock clock) {
        this.agentRepository = agentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.equipmentRepository = equipmentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (agentRepository.count() > 0) {
            return;
        }
        Random random = new Random(SEED);
        Instant now = LocalDate.now(clock.withZone(CITY_ZONE)).atStartOfDay(CITY_ZONE).toInstant();
        Map<AgentType, List<VehicleEntity>> vehicles = usableVehiclesByAgentType();
        int sequence = 0;

        for (AgentType type : AgentType.values()) {
            for (int i = 0; i < QUANTITIES.get(type); i++) {
                sequence++;
                Location base = PrudenteGeo.randomLocation(random);
                AgentEntity agent = agentRepository.save(new AgentEntity(name(random),
                        REGISTRATION_PREFIX.get(type) + "-" + (1000 + sequence), type, AgentStatus.OFF_DUTY, base,
                        null));
                simulateHistory(agent, random, now);
                assignVehicleAndPosition(agent, random, now, vehicles.get(type));
                agentRepository.save(agent);
            }
        }
    }

    /** Turnos: entra em serviço, alterna entre livre e ocorrência e sai; às vezes um afastamento de dias. */
    private void simulateHistory(AgentEntity agent, Random random, Instant now) {
        boolean busyRegion = agent.getBaseLocation().getRegion() == Region.NORTH
                || agent.getBaseLocation().getRegion() == Region.EAST;
        Instant at = now.minus(180, ChronoUnit.DAYS).plus(random.nextInt(48), ChronoUnit.HOURS);

        while (at.isBefore(now)) {
            AgentStatus next = switch (agent.getStatus()) {
                case OFF_DUTY -> random.nextDouble() < LEAVE_CHANCE ? AgentStatus.ON_LEAVE : AgentStatus.AVAILABLE;
                case ON_LEAVE, ON_OCCURRENCE -> AgentStatus.AVAILABLE;
                case AVAILABLE -> random.nextDouble() < END_OF_SHIFT_CHANCE ? AgentStatus.OFF_DUTY
                        : AgentStatus.ON_OCCURRENCE;
            };
            AgentStatus previous = agent.changeStatus(next);
            statusHistoryRepository.save(new AgentStatusHistoryEntity(agent.getId(), previous, next, at, OPERATOR,
                    null));
            at = at.plus(switch (next) {
                case ON_OCCURRENCE -> 1 + random.nextInt(4);
                case AVAILABLE -> busyRegion ? 1 + random.nextInt(4) : 2 + random.nextInt(7);
                case OFF_DUTY -> 10 + random.nextInt(11);
                case ON_LEAVE -> 24 * (5 + random.nextInt(11));
            }, ChronoUnit.HOURS);
        }
    }

    /** Em serviço: ~60% com viatura compatível e todos com posição perto da base. */
    private static void assignVehicleAndPosition(AgentEntity agent, Random random, Instant now,
                                                 List<VehicleEntity> compatible) {
        if (!agent.getStatus().isOnDuty()) {
            return;
        }
        if (!compatible.isEmpty() && random.nextDouble() < 0.6) {
            agent.update(agent.getName(), agent.getRegistrationNumber(), agent.getAgentType(),
                    agent.getBaseLocation(), PrudenteGeo.pick(random, compatible));
        }
        agent.updatePosition(near(agent.getBaseLocation().getLatitude(), random),
                near(agent.getBaseLocation().getLongitude(), random),
                now.minus(5 + random.nextInt(55), ChronoUnit.MINUTES));
    }

    private Map<AgentType, List<VehicleEntity>> usableVehiclesByAgentType() {
        Map<AgentType, List<VehicleEntity>> byType = new EnumMap<>(AgentType.class);
        for (AgentType type : AgentType.values()) {
            byType.put(type, new ArrayList<>());
        }
        equipmentRepository.findAll().stream()
                .filter(VehicleEntity.class::isInstance)
                .map(VehicleEntity.class::cast)
                .filter(vehicle -> vehicle.getStatus() != EquipmentStatus.DEACTIVATED
                        && vehicle.getStatus() != EquipmentStatus.STOLEN)
                .sorted(Comparator.comparing(VehicleEntity::getSerialNumber))
                .forEach(vehicle -> {
                    for (AgentType type : AgentType.values()) {
                        if (vehicle.getType() == type.vehicleType() && vehicle.getAgency() == type.agency()) {
                            byType.get(type).add(vehicle);
                        }
                    }
                });
        return byType;
    }

    private static String name(Random random) {
        return PrudenteGeo.pick(random, FIRST_NAMES) + " " + PrudenteGeo.pick(random, LAST_NAMES);
    }

    private static BigDecimal near(BigDecimal coordinate, Random random) {
        return coordinate.add(BigDecimal.valueOf((random.nextDouble() * 2 - 1) * 0.008))
                .setScale(6, RoundingMode.HALF_UP);
    }
}
