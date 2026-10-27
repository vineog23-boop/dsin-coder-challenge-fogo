package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.equipment.domain.CameraResolution;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.equipment.persistence.CameraEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentRepository;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryRepository;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
import br.com.dsin.challengefogo.equipment.persistence.ScooterEntity;
import br.com.dsin.challengefogo.equipment.persistence.TruckScaleEntity;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Cria ~150 equipamentos em Presidente Prudente com ~6 meses de histórico, só com o banco vazio.
 * {@code Random(42)}: os dados são os mesmos a cada subida do zero (as datas são relativas ao dia da subida).
 *
 * <p><b>Padrão plantado para a Parte 3 (previsão) encontrar:</b>
 * <ul>
 *   <li>Taxa base de falha: {@value #BASE_FAILURES_PER_MONTH} por equipamento por mês.</li>
 *   <li>Região: Norte ×{@value #NORTH_FACTOR}, Leste ×{@value #EAST_FACTOR}; demais ×1.</li>
 *   <li>Tipo: radar e câmera ×{@value #EXPOSED_TYPE_FACTOR} (ficam na rua, expostos a chuva e vandalismo).</li>
 *   <li>Horário: {@value #NIGHT_SHARE} das falhas entre 18h e 23h (horário local); o resto em qualquer hora.</li>
 *   <li>Reparo: {@value #REPAIR_BACKLOG_SHARE} das falhas esperam de 3 a 15 dias na fila; as demais, até 24 h.</li>
 *   <li>Preventiva: {@value #MISSED_PREVENTIVE_SHARE} das que venceram no último mês não foram feitas (manutenção atrasada).</li>
 * </ul>
 * Cada falha segue o ciclo OPERATIONAL → MALFUNCTION → UNDER_MAINTENANCE → manutenção corretiva → OPERATIONAL;
 * equipamentos com intervalo recebem manutenção preventiva periódica. Ciclos que não terminaram até hoje
 * deixam o equipamento em falha ou em manutenção, como estaria na vida real.
 */
/* Antes do seed de agentes, que usa as viaturas criadas aqui. */
@Component
@Profile("demo")
@Order(1)
public class DemoEquipmentSeeder implements ApplicationRunner {

    static final long SEED = 42L;
    static final double BASE_FAILURES_PER_MONTH = 0.25;
    static final double NORTH_FACTOR = 2.5;
    static final double EAST_FACTOR = 1.8;
    static final double EXPOSED_TYPE_FACTOR = 1.3;
    static final double NIGHT_SHARE = 0.6;
    static final double REPAIR_BACKLOG_SHARE = 0.35;
    static final double MISSED_PREVENTIVE_SHARE = 0.15;

    private static final ZoneId CITY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String OPERATOR = "gestor";

    /** Quantidade por tipo; soma 150. */
    private static final Map<EquipmentType, Integer> QUANTITIES = quantities();

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusHistoryRepository statusHistoryRepository;
    private final EquipmentMaintenanceRepository maintenanceRepository;
    private final Clock clock;

    public DemoEquipmentSeeder(EquipmentRepository equipmentRepository,
                               EquipmentStatusHistoryRepository statusHistoryRepository,
                               EquipmentMaintenanceRepository maintenanceRepository, Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (equipmentRepository.count() > 0) {
            return;
        }
        Random random = new Random(SEED);
        // Meia-noite local de hoje: o mesmo dia gera exatamente os mesmos instantes.
        Instant now = LocalDate.now(clock.withZone(CITY_ZONE)).atStartOfDay(CITY_ZONE).toInstant();
        Set<String> plates = new HashSet<>();
        int sequence = 0;

        for (Map.Entry<EquipmentType, Integer> entry : QUANTITIES.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                sequence++;
                EquipmentEntity equipment = equipmentRepository.save(
                        newEquipment(entry.getKey(), sequence, random, plates));
                simulateHistory(equipment, random, now, sequence);
                equipmentRepository.save(equipment);
            }
        }
    }

    private void simulateHistory(EquipmentEntity equipment, Random random, Instant now, int sequence) {
        // Totens mais recentes ainda não foram instalados: entram no mapa como "não instalado".
        if (equipment.getType() == EquipmentType.PARKING_KIOSK && sequence % 3 == 0) {
            return;
        }
        Instant installedAt = atLocalHour(now.minus(150 + random.nextInt(36), ChronoUnit.DAYS), 9);
        changeStatus(equipment, EquipmentStatus.OPERATIONAL, installedAt, "Instalação concluída.");
        if (equipment.getMaintenanceIntervalDays() != null) {
            LocalDate firstPreventive = LocalDate.ofInstant(installedAt, CITY_ZONE)
                    .plusDays(equipment.getMaintenanceIntervalDays());
            equipment.update(equipment.getSerialNumber(), equipment.getBrand(), equipment.getModel(),
                    equipment.getAgency(), MaintenanceNeed.SCHEDULED, firstPreventive,
                    equipment.getMaintenanceIntervalDays(), equipment.getLocation());
        }

        double monthlyRate = BASE_FAILURES_PER_MONTH * regionFactor(equipment.getLocation().getRegion())
                * typeFactor(equipment.getType());
        Instant cursor = installedAt;
        Instant nextFailure = cursor.plus(exponentialDays(random, monthlyRate));
        Instant nextPreventive = nextPreventive(equipment);

        while (true) {
            if (nextPreventive != null && nextPreventive.isBefore(nextFailure)) {
                if (!nextPreventive.isBefore(now)) {
                    break;
                }
                // Parte das preventivas do último mês fica esquecida: vira manutenção atrasada na central.
                if (nextPreventive.isAfter(now.minus(30, ChronoUnit.DAYS)) && random.nextDouble() < MISSED_PREVENTIVE_SHARE) {
                    break;
                }
                registerMaintenance(equipment, MaintenanceKind.PREVENTIVE, nextPreventive, "Revisão periódica.");
                nextPreventive = nextPreventive(equipment);
                continue;
            }
            if (!nextFailure.isBefore(now)) {
                break;
            }
            Instant failedAt = atLocalHour(nextFailure, failureHour(random));
            if (!failedAt.isBefore(now)) {
                break;
            }
            changeStatus(equipment, EquipmentStatus.MALFUNCTION, failedAt, "Falha detectada.");

            // Um quarto das falhas espera na fila de reparo: é o que deixa parte do parque parado hoje.
            Instant repairStart = random.nextDouble() < REPAIR_BACKLOG_SHARE
                    ? failedAt.plus(3 + random.nextInt(13), ChronoUnit.DAYS)
                    : failedAt.plus(2 + random.nextInt(22), ChronoUnit.HOURS);
            if (!repairStart.isBefore(now)) {
                break;
            }
            changeStatus(equipment, EquipmentStatus.UNDER_MAINTENANCE, repairStart, "Equipe a caminho.");

            Instant repairedAt = repairStart.plus(4 + random.nextInt(68), ChronoUnit.HOURS);
            if (!repairedAt.isBefore(now)) {
                break;
            }
            registerMaintenance(equipment, MaintenanceKind.CORRECTIVE, repairedAt, "Reparo após falha.");

            cursor = repairedAt;
            nextFailure = cursor.plus(exponentialDays(random, monthlyRate));
            nextPreventive = nextPreventive(equipment);
        }

        // Alguns casos raros, para todas as situações aparecerem no painel.
        if (equipment.getStatus() == EquipmentStatus.OPERATIONAL) {
            if (equipment.getType() == EquipmentType.ELECTRIC_SCOOTER && sequence % 7 == 0) {
                changeStatus(equipment, EquipmentStatus.STOLEN, now.minus(3, ChronoUnit.DAYS), "Boletim de ocorrência.");
            } else if (sequence % 50 == 0) {
                changeStatus(equipment, EquipmentStatus.DEACTIVATED, now.minus(10, ChronoUnit.DAYS),
                        "Retirado de operação.");
            }
        }
    }

    private void changeStatus(EquipmentEntity equipment, EquipmentStatus status, Instant at, String note) {
        EquipmentStatus previous = equipment.changeStatus(status);
        statusHistoryRepository.save(new EquipmentStatusHistoryEntity(equipment.getId(), previous, status, at,
                OPERATOR, note));
    }

    /** Mesma regra 5 do service, com a data do passado em vez do relógio atual. */
    private void registerMaintenance(EquipmentEntity equipment, MaintenanceKind kind, Instant at, String description) {
        EquipmentStatus previous = equipment.getStatus();
        if (equipment.registerMaintenance(LocalDate.ofInstant(at, CITY_ZONE))) {
            statusHistoryRepository.save(new EquipmentStatusHistoryEntity(equipment.getId(), previous,
                    equipment.getStatus(), at, OPERATOR, "Manutenção registrada."));
        }
        maintenanceRepository.save(new EquipmentMaintenanceEntity(equipment.getId(), kind, at, description, OPERATOR));
    }

    private static Instant nextPreventive(EquipmentEntity equipment) {
        if (equipment.getMaintenanceNeed() != MaintenanceNeed.SCHEDULED || equipment.getNextMaintenanceDate() == null) {
            return null;
        }
        return equipment.getNextMaintenanceDate().atTime(10, 0).atZone(CITY_ZONE).toInstant();
    }

    private static EquipmentEntity newEquipment(EquipmentType type, int sequence, Random random, Set<String> plates) {
        Location location = PrudenteGeo.randomLocation(random);
        String serial = serialPrefix(type) + "-" + String.format("%05d", 10_000 + sequence * 37);
        EquipmentStatus status = EquipmentStatus.NOT_INSTALLED;
        MaintenanceNeed need = MaintenanceNeed.NONE;

        return switch (type) {
            case RADAR -> {
                RadarModality modality = PrudenteGeo.pick(random, List.of(RadarModality.values()));
                yield new RadarEntity(serial, pick(random, "Perkons", "Velsis", "Splice"), "RV-" + (100 + random.nextInt(4) * 100),
                        Agency.MUNICIPAL_TRAFFIC, status, need, null, 90, location, modality,
                        modality == RadarModality.SPEED ? PrudenteGeo.pick(random, List.of(40, 50, 60, 70)) : null,
                        1 + random.nextInt(4));
            }
            case SURVEILLANCE_CAMERA -> new CameraEntity(serial, pick(random, "Intelbras", "Hikvision", "Axis"),
                    "CAM-" + (2 + random.nextInt(6)) + "00",
                    random.nextInt(3) == 0 ? Agency.MUNICIPAL_GUARD : Agency.MUNICIPAL_TRAFFIC, status, need, null,
                    180, location, random.nextInt(4) > 0, PrudenteGeo.pick(random, List.of(CameraResolution.values())),
                    random.nextInt(5) > 0, random.nextBoolean());
            case TRUCK_SCALE -> new TruckScaleEntity(serial, pick(random, "Toledo", "Filizola"), "BR-" + (60 + random.nextInt(4) * 10),
                    Agency.MUNICIPAL_TRAFFIC, status, need, null, 60, location,
                    BigDecimal.valueOf(40 + random.nextInt(5) * 10), random.nextBoolean());
            case ELECTRIC_SCOOTER -> {
                boolean shared = random.nextInt(3) > 0;
                yield new ScooterEntity(serial, pick(random, "Xiaomi", "Segway"), "S-" + (1 + random.nextInt(4)),
                        shared ? Agency.PRIVATE_OPERATOR : Agency.MUNICIPAL_TRAFFIC, status, need, null, null, location,
                        shared ? ScooterOwnership.SHARED : ScooterOwnership.PUBLIC,
                        shared ? pick(random, "Yellow", "Grow", "Jet") : null, 10 + random.nextInt(91));
            }
            case AUTOMATED_GATE, PARKING_KIOSK, OTHER -> new EquipmentEntity(type, serial,
                    pick(random, "Digicon", "Wps", "Nepos"), "EQ-" + (1 + random.nextInt(9)), Agency.MUNICIPAL_TRAFFIC,
                    status, need, null, type == EquipmentType.OTHER ? null : 120, location);
            default -> new VehicleEntity(type, serial, vehicleBrand(type, random), "V-" + (1 + random.nextInt(9)),
                    vehicleAgency(type), status, need, null, 30, location, uniquePlate(random, plates),
                    passengers(type), type == EquipmentType.PARKING_ENFORCEMENT_VEHICLE);
        };
    }

    /** Placa Mercosul (ABC1D23) única dentro da execução. */
    private static String uniquePlate(Random random, Set<String> plates) {
        String plate;
        do {
            plate = "" + letter(random) + letter(random) + letter(random) + random.nextInt(10) + letter(random)
                    + random.nextInt(10) + random.nextInt(10);
        } while (!plates.add(plate));
        return plate;
    }

    private static char letter(Random random) {
        return (char) ('A' + random.nextInt(26));
    }

    private static Agency vehicleAgency(EquipmentType type) {
        return switch (type) {
            case MUNICIPAL_GUARD_VEHICLE -> Agency.MUNICIPAL_GUARD;
            case MILITARY_POLICE_VEHICLE -> Agency.MILITARY_POLICE;
            case PARKING_ENFORCEMENT_VEHICLE, TRAFFIC_AGENT_MOTORCYCLE -> Agency.MUNICIPAL_TRAFFIC;
            default -> Agency.PRIVATE_OPERATOR;
        };
    }

    private static String vehicleBrand(EquipmentType type, Random random) {
        return switch (type) {
            case MOTO_TAXI, TRAFFIC_AGENT_MOTORCYCLE -> pick(random, "Honda", "Yamaha");
            case SCHOOL_TRANSPORT -> pick(random, "Mercedes-Benz", "Volkswagen");
            default -> pick(random, "Chevrolet", "Fiat", "Toyota", "Renault");
        };
    }

    private static Integer passengers(EquipmentType type) {
        return switch (type) {
            case MOTO_TAXI, TRAFFIC_AGENT_MOTORCYCLE -> 1;
            case SCHOOL_TRANSPORT -> 20;
            case PARKING_ENFORCEMENT_VEHICLE -> 0;
            default -> 4;
        };
    }

    private static double regionFactor(Region region) {
        return switch (region) {
            case NORTH -> NORTH_FACTOR;
            case EAST -> EAST_FACTOR;
            default -> 1.0;
        };
    }

    private static double typeFactor(EquipmentType type) {
        return type == EquipmentType.RADAR || type == EquipmentType.SURVEILLANCE_CAMERA ? EXPOSED_TYPE_FACTOR : 1.0;
    }

    private static int failureHour(Random random) {
        return random.nextDouble() < NIGHT_SHARE ? 18 + random.nextInt(6) : random.nextInt(24);
    }

    /** Intervalo até a próxima falha num processo de Poisson com a taxa mensal dada. */
    private static Duration exponentialDays(Random random, double monthlyRate) {
        double days = -Math.log(1 - random.nextDouble()) * 30 / monthlyRate;
        return Duration.ofMinutes((long) (days * 24 * 60));
    }

    private static Instant atLocalHour(Instant instant, int hour) {
        LocalDateTime local = LocalDateTime.ofInstant(instant, CITY_ZONE).withHour(hour).withMinute(0);
        return local.atZone(CITY_ZONE).toInstant();
    }

    private static String serialPrefix(EquipmentType type) {
        return switch (type) {
            case RADAR -> "RAD";
            case SURVEILLANCE_CAMERA -> "CAM";
            case TRUCK_SCALE -> "BAL";
            case ELECTRIC_SCOOTER -> "PAT";
            case AUTOMATED_GATE -> "CAN";
            case PARKING_KIOSK -> "TOT";
            case OTHER -> "OUT";
            default -> "VEI";
        };
    }

    @SafeVarargs
    private static <T> T pick(Random random, T... values) {
        return values[random.nextInt(values.length)];
    }

    private static Map<EquipmentType, Integer> quantities() {
        Map<EquipmentType, Integer> quantities = new LinkedHashMap<>();
        quantities.put(EquipmentType.RADAR, 25);
        quantities.put(EquipmentType.SURVEILLANCE_CAMERA, 30);
        quantities.put(EquipmentType.TRUCK_SCALE, 6);
        quantities.put(EquipmentType.TAXI, 12);
        quantities.put(EquipmentType.MOTO_TAXI, 8);
        quantities.put(EquipmentType.SCHOOL_TRANSPORT, 8);
        quantities.put(EquipmentType.PARKING_ENFORCEMENT_VEHICLE, 4);
        quantities.put(EquipmentType.TRAFFIC_AGENT_MOTORCYCLE, 10);
        quantities.put(EquipmentType.MUNICIPAL_GUARD_VEHICLE, 8);
        quantities.put(EquipmentType.MILITARY_POLICE_VEHICLE, 10);
        quantities.put(EquipmentType.ELECTRIC_SCOOTER, 15);
        quantities.put(EquipmentType.AUTOMATED_GATE, 6);
        quantities.put(EquipmentType.PARKING_KIOSK, 6);
        quantities.put(EquipmentType.OTHER, 2);
        return quantities;
    }
}
