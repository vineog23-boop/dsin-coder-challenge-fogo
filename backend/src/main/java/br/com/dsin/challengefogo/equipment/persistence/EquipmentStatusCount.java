package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;

public record EquipmentStatusCount(EquipmentStatus status, long total) {
}
