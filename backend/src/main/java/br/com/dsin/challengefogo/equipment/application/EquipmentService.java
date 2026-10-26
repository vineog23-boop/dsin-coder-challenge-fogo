package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.EquipmentFilter;
import br.com.dsin.challengefogo.equipment.application.dto.HistoryEventDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoRequest;
import br.com.dsin.challengefogo.equipment.application.dto.MaintenanceDtoResponse;
import br.com.dsin.challengefogo.equipment.application.dto.StatusChangeDtoRequest;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
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
import br.com.dsin.challengefogo.shared.domain.BusinessRuleException;
import br.com.dsin.challengefogo.shared.domain.ConflictException;
import br.com.dsin.challengefogo.shared.domain.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class EquipmentService {

    /** Fuso da cidade: converte o instante da manutenção na data usada para agendar a próxima. */
    private static final ZoneId CITY_ZONE = ZoneId.of("America/Sao_Paulo");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusHistoryRepository statusHistoryRepository;
    private final EquipmentMaintenanceRepository maintenanceRepository;
    private final Clock clock;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            EquipmentStatusHistoryRepository statusHistoryRepository,
                            EquipmentMaintenanceRepository maintenanceRepository, Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.clock = clock;
    }

    @Transactional
    public EquipmentDtoResponse create(EquipmentDtoRequest request) {
        EquipmentTypeValidator.validate(request);
        if (equipmentRepository.existsBySerialNumber(request.serialNumber())) {
            throw duplicatedSerialNumber(request.serialNumber());
        }
        if (request.vehicle() != null && equipmentRepository.existsByLicensePlate(request.vehicle().licensePlate())) {
            throw duplicatedLicensePlate(request.vehicle().licensePlate());
        }

        EquipmentEntity equipment = equipmentRepository.save(newEquipment(request));

        return new EquipmentDtoResponse(equipment);
    }

    @Transactional(readOnly = true)
    public EquipmentDtoResponse findById(UUID id) {
        return new EquipmentDtoResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<EquipmentDtoResponse> search(EquipmentFilter filter, Pageable pageable) {
        return equipmentRepository.findAll(EquipmentSpecifications.from(filter), pageable)
                .map(EquipmentDtoResponse::new);
    }

    @Transactional
    public EquipmentDtoResponse update(UUID id, EquipmentDtoRequest request) {
        EquipmentEntity equipment = find(id);
        if (request.type() != equipment.getType()) {
            throw new BusinessRuleException("O tipo do equipamento não pode ser alterado.");
        }
        if (request.status() != null && request.status() != equipment.getStatus()) {
            throw new BusinessRuleException("A situação muda pela rota própria, que grava o histórico.");
        }
        EquipmentTypeValidator.validate(request);
        if (equipmentRepository.existsBySerialNumberAndIdNot(request.serialNumber(), id)) {
            throw duplicatedSerialNumber(request.serialNumber());
        }
        if (request.vehicle() != null
                && equipmentRepository.existsByLicensePlateAndIdNot(request.vehicle().licensePlate(), id)) {
            throw duplicatedLicensePlate(request.vehicle().licensePlate());
        }

        equipment.update(request.serialNumber(), request.brand(), request.model(), request.agency(),
                request.maintenanceNeedOrDefault(), request.nextMaintenanceDate(), request.maintenanceIntervalDays(),
                request.location().toLocation());
        updateCharacteristics(equipment, request);
        // flush para o updatedAt da resposta já refletir a alteração
        equipmentRepository.saveAndFlush(equipment);

        return new EquipmentDtoResponse(equipment);
    }

    @Transactional
    public EquipmentDtoResponse changeStatus(UUID id, StatusChangeDtoRequest request, String username) {
        EquipmentEntity equipment = find(id);

        EquipmentStatus previous = equipment.changeStatus(request.status());
        statusHistoryRepository.save(new EquipmentStatusHistoryEntity(id, previous, request.status(), clock.instant(),
                username, request.note()));
        equipmentRepository.saveAndFlush(equipment);

        return new EquipmentDtoResponse(equipment);
    }

    @Transactional
    public MaintenanceDtoResponse registerMaintenance(UUID id, MaintenanceDtoRequest request, String username) {
        EquipmentEntity equipment = find(id);
        EquipmentStatus previous = equipment.getStatus();

        boolean backToOperation = equipment.registerMaintenance(LocalDate.ofInstant(request.performedAt(), CITY_ZONE));
        if (backToOperation) {
            statusHistoryRepository.save(new EquipmentStatusHistoryEntity(id, previous, equipment.getStatus(),
                    clock.instant(), username, "Manutenção registrada."));
        }
        EquipmentMaintenanceEntity maintenance = maintenanceRepository.save(new EquipmentMaintenanceEntity(id,
                request.kind(), request.performedAt(), request.description(), username));
        equipmentRepository.saveAndFlush(equipment);

        return new MaintenanceDtoResponse(maintenance);
    }

    /**
     * Junta situações e manutenções numa só linha do tempo paginada. Cada fonte já vem ordenada; basta
     * ler de cada uma até o fim da página pedida, intercalar e recortar.
     */
    @Transactional(readOnly = true)
    public Page<HistoryEventDtoResponse> history(UUID id, Pageable pageable) {
        if (!equipmentRepository.existsById(id)) {
            throw new EquipmentNotFoundException(id);
        }
        Pageable upToRequestedPage = PageRequest.of(0, (int) (pageable.getOffset() + pageable.getPageSize()));
        Page<EquipmentStatusHistoryEntity> changes =
                statusHistoryRepository.findByEquipmentIdOrderByChangedAtDesc(id, upToRequestedPage);
        Page<EquipmentMaintenanceEntity> maintenances =
                maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(id, upToRequestedPage);

        List<HistoryEventDtoResponse> events = Stream.concat(
                        changes.getContent().stream().map(HistoryEventDtoResponse::new),
                        maintenances.getContent().stream().map(HistoryEventDtoResponse::new))
                .sorted(Comparator.comparing(HistoryEventDtoResponse::occurredAt).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return new PageImpl<>(events, pageable, changes.getTotalElements() + maintenances.getTotalElements());
    }

    private EquipmentEntity find(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException(id));
    }

    private static EquipmentEntity newEquipment(EquipmentDtoRequest r) {
        EquipmentStatus status = r.status() != null ? r.status() : EquipmentStatus.NOT_INSTALLED;
        MaintenanceNeed need = r.maintenanceNeedOrDefault();
        Location location = r.location().toLocation();

        return switch (r.type().block()) {
            case RADAR -> new RadarEntity(r.serialNumber(), r.brand(), r.model(), r.agency(), status, need,
                    r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location, r.radar().modality(),
                    r.radar().speedLimitKmh(), r.radar().lanes());
            case CAMERA -> new CameraEntity(r.serialNumber(), r.brand(), r.model(), r.agency(), status, need,
                    r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location, r.camera().nightVision(),
                    r.camera().resolution(), r.camera().color(), r.camera().ptz());
            case TRUCK_SCALE -> new TruckScaleEntity(r.serialNumber(), r.brand(), r.model(), r.agency(), status, need,
                    r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location, r.truckScale().maxCapacityTons(),
                    r.truckScale().weighInMotion());
            case VEHICLE -> new VehicleEntity(r.type(), r.serialNumber(), r.brand(), r.model(), r.agency(), status,
                    need, r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location, r.vehicle().licensePlate(),
                    r.vehicle().passengerCapacity(), r.vehicle().autonomous());
            case SCOOTER -> new ScooterEntity(r.serialNumber(), r.brand(), r.model(), r.agency(), status, need,
                    r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location, r.scooter().ownership(),
                    r.scooter().operatorName(), r.scooter().batteryLevel());
            case NONE -> new EquipmentEntity(r.type(), r.serialNumber(), r.brand(), r.model(), r.agency(), status,
                    need, r.nextMaintenanceDate(), r.maintenanceIntervalDays(), location);
        };
    }

    private static void updateCharacteristics(EquipmentEntity equipment, EquipmentDtoRequest r) {
        switch (equipment) {
            case RadarEntity radar ->
                    radar.updateCharacteristics(r.radar().modality(), r.radar().speedLimitKmh(), r.radar().lanes());
            case CameraEntity camera -> camera.updateCharacteristics(r.camera().nightVision(),
                    r.camera().resolution(), r.camera().color(), r.camera().ptz());
            case TruckScaleEntity scale ->
                    scale.updateCharacteristics(r.truckScale().maxCapacityTons(), r.truckScale().weighInMotion());
            case VehicleEntity vehicle -> vehicle.updateCharacteristics(r.vehicle().licensePlate(),
                    r.vehicle().passengerCapacity(), r.vehicle().autonomous());
            case ScooterEntity scooter -> scooter.updateCharacteristics(r.scooter().ownership(),
                    r.scooter().operatorName(), r.scooter().batteryLevel());
            default -> {
                // tipo sem características próprias
            }
        }
    }

    private static ConflictException duplicatedSerialNumber(String serialNumber) {
        return new ConflictException("Já existe um equipamento com o número de série " + serialNumber + ".");
    }

    private static ConflictException duplicatedLicensePlate(String licensePlate) {
        return new ConflictException("Já existe um veículo com a placa " + licensePlate + ".");
    }
}
