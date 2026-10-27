package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.time.LocalDate;
import java.util.UUID;

/** Equipamento candidato a alerta (falha, roubo ou manutenção atrasada). */
public record EquipmentAlertView(UUID id, EquipmentType type, String serialNumber, EquipmentStatus status,
                                 MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate, Region region) {
}
