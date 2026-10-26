package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;

/** Filtros da listagem, vindos da query string; os nulos são ignorados. */
public record EquipmentFilter(
        EquipmentType type,
        EquipmentStatus status,
        Region region,
        String neighborhood,
        Agency agency,
        MaintenanceNeed maintenanceNeed,
        String search) {
}
