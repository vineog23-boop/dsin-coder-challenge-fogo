package br.com.dsin.challengefogo.agent.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;

import java.util.UUID;

/** Resumo da viatura no agente: o suficiente para exibir e linkar, sem trazer o equipamento todo. */
public record VehicleSummaryDto(UUID id, EquipmentType type, String serialNumber, String licensePlate,
                                EquipmentStatus status) {

    public VehicleSummaryDto(VehicleEntity vehicle) {
        this(vehicle.getId(), vehicle.getType(), vehicle.getSerialNumber(), vehicle.getLicensePlate(),
                vehicle.getStatus());
    }
}
