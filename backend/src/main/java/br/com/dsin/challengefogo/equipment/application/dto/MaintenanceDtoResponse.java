package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceEntity;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceDtoResponse(UUID id, MaintenanceKind kind, Instant performedAt, String description,
                                     String performedBy) {

    public MaintenanceDtoResponse(EquipmentMaintenanceEntity maintenance) {
        this(maintenance.getId(), maintenance.getKind(), maintenance.getPerformedAt(), maintenance.getDescription(),
                maintenance.getPerformedBy());
    }
}
