package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.math.BigDecimal;
import java.util.UUID;

/** Só o que o marcador do mapa usa: lido da tabela base, sem os JOINs das tabelas filhas. */
public record EquipmentMarkerView(UUID id, EquipmentType type, String serialNumber, EquipmentStatus status,
                                  MaintenanceNeed maintenanceNeed, BigDecimal latitude, BigDecimal longitude,
                                  Region region) {
}
