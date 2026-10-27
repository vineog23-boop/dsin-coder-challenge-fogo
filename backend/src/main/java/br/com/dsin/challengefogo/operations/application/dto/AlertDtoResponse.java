package br.com.dsin.challengefogo.operations.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.time.Instant;
import java.util.UUID;

/**
 * Alerta calculado na hora (regra 5): não é gravado nem "lido". {@code equipmentId}, {@code equipmentType}
 * e {@code since} vêm nulos quando o alerta é da região, não de um equipamento.
 */
public record AlertDtoResponse(Kind kind, Severity severity, String message, Region region, UUID equipmentId,
                               EquipmentType equipmentType, Instant since) {

    public enum Kind {
        EQUIPMENT_MALFUNCTION,
        EQUIPMENT_STOLEN,
        REGION_WITHOUT_ACTIVE_AGENT,
        MAINTENANCE_OVERDUE
    }

    /** A ordem das constantes é a ordem de exibição: alta primeiro. */
    public enum Severity {
        HIGH,
        MEDIUM
    }
}
