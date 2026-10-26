package br.com.dsin.challengefogo.equipment.application.dto;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentMaintenanceEntity;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusHistoryEntity;

import java.time.Instant;

/**
 * Evento da linha do tempo: mudança de situação ou manutenção, num formato único para o front
 * ordenar e exibir sem juntar duas listas. Os campos do outro tipo de evento vêm nulos.
 */
public record HistoryEventDtoResponse(
        EventType eventType,
        Instant occurredAt,
        String performedBy,
        EquipmentStatus fromStatus,
        EquipmentStatus toStatus,
        String note,
        MaintenanceKind maintenanceKind,
        String description) {

    public enum EventType {
        STATUS_CHANGE,
        MAINTENANCE
    }

    public HistoryEventDtoResponse(EquipmentStatusHistoryEntity change) {
        this(EventType.STATUS_CHANGE, change.getChangedAt(), change.getChangedBy(), change.getFromStatus(),
                change.getToStatus(), change.getNote(), null, null);
    }

    public HistoryEventDtoResponse(EquipmentMaintenanceEntity maintenance) {
        this(EventType.MAINTENANCE, maintenance.getPerformedAt(), maintenance.getPerformedBy(), null, null, null,
                maintenance.getKind(), maintenance.getDescription());
    }
}
