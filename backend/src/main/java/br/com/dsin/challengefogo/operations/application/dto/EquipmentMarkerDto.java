package br.com.dsin.challengefogo.operations.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMarkerView;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.math.BigDecimal;
import java.util.UUID;

public record EquipmentMarkerDto(UUID id, EquipmentType type, String serialNumber, EquipmentStatus status,
                                 MaintenanceNeed maintenanceNeed, BigDecimal latitude, BigDecimal longitude,
                                 Region region) {

    public EquipmentMarkerDto(EquipmentMarkerView view) {
        this(view.id(), view.type(), view.serialNumber(), view.status(), view.maintenanceNeed(), view.latitude(),
                view.longitude(), view.region());
    }
}
