package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.MaintenanceKind;
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

/** Manutenção realizada; registro imutável, como o histórico de situação. */
@Entity
@Table(name = "equipment_maintenance")
public class EquipmentMaintenanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "equipment_id", nullable = false, updatable = false)
    private UUID equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private MaintenanceKind kind;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    @Column(name = "performed_by", nullable = false, length = 50, updatable = false)
    private String performedBy;

    protected EquipmentMaintenanceEntity() {
    }

    public EquipmentMaintenanceEntity(UUID equipmentId, MaintenanceKind kind, Instant performedAt,
                                      String description, String performedBy) {
        this.equipmentId = equipmentId;
        this.kind = kind;
        this.performedAt = performedAt;
        this.description = description;
        this.performedBy = performedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEquipmentId() {
        return equipmentId;
    }

    public MaintenanceKind getKind() {
        return kind;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getDescription() {
        return description;
    }

    public String getPerformedBy() {
        return performedBy;
    }
}
