package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma mudança de situação. Guarda só o id do equipamento (sem relacionamento JPA): o histórico
 * é lido por consulta própria e nunca navegado a partir do equipamento.
 */
@Entity
@Table(name = "equipment_status_history")
public class EquipmentStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "equipment_id", nullable = false, updatable = false)
    private UUID equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, updatable = false)
    private EquipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, updatable = false)
    private EquipmentStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, length = 50, updatable = false)
    private String changedBy;

    @Column(length = 500, updatable = false)
    private String note;

    protected EquipmentStatusHistoryEntity() {
    }

    public EquipmentStatusHistoryEntity(UUID equipmentId, EquipmentStatus fromStatus, EquipmentStatus toStatus,
                                        Instant changedAt, String changedBy, String note) {
        this.equipmentId = equipmentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public EquipmentStatus getFromStatus() {
        return fromStatus;
    }

    public EquipmentStatus getToStatus() {
        return toStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getNote() {
        return note;
    }
}
