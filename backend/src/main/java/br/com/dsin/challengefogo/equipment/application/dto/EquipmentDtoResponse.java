package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.CameraEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.RadarEntity;
import br.com.dsin.challengefogo.equipment.persistence.ScooterEntity;
import br.com.dsin.challengefogo.equipment.persistence.TruckScaleEntity;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import br.com.dsin.challengefogo.shared.application.dto.LocationDto;
import br.com.dsin.challengefogo.shared.domain.Agency;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Equipamento com o bloco de características do seu tipo; os demais blocos vêm nulos. */
public record EquipmentDtoResponse(
        UUID id,
        EquipmentType type,
        String serialNumber,
        String brand,
        String model,
        Agency agency,
        EquipmentStatus status,
        MaintenanceNeed maintenanceNeed,
        LocalDate nextMaintenanceDate,
        Integer maintenanceIntervalDays,
        LocationDto location,
        RadarDto radar,
        CameraDto camera,
        TruckScaleDto truckScale,
        VehicleDto vehicle,
        ScooterDto scooter,
        Instant createdAt,
        Instant updatedAt) {

    public EquipmentDtoResponse(EquipmentEntity equipment) {
        this(equipment.getId(), equipment.getType(), equipment.getSerialNumber(), equipment.getBrand(),
                equipment.getModel(), equipment.getAgency(), equipment.getStatus(), equipment.getMaintenanceNeed(),
                equipment.getNextMaintenanceDate(), equipment.getMaintenanceIntervalDays(),
                equipment.getLocation() != null ? new LocationDto(equipment.getLocation()) : null,
                equipment instanceof RadarEntity radar ? new RadarDto(radar) : null,
                equipment instanceof CameraEntity camera ? new CameraDto(camera) : null,
                equipment instanceof TruckScaleEntity scale ? new TruckScaleDto(scale) : null,
                equipment instanceof VehicleEntity vehicle ? new VehicleDto(vehicle) : null,
                equipment instanceof ScooterEntity scooter ? new ScooterDto(scooter) : null,
                equipment.getCreatedAt(), equipment.getUpdatedAt());
    }
}
