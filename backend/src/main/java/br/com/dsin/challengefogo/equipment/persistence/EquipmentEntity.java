package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Equipamento IoT. Herança {@code JOINED}: as características de cada grupo de tipos ficam numa
 * tabela filha (subclasses). É concreta porque cancela, totem e "outro" não têm características
 * próprias e vivem só na tabela base. Sem discriminador: vários tipos compartilham a tabela
 * {@code vehicle}, então o tipo de negócio fica no campo {@code type}.
 */
@Entity
@Table(name = "equipment")
@Inheritance(strategy = InheritanceType.JOINED)
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private EquipmentType type;

    @Column(name = "serial_number", nullable = false, unique = true, length = 60)
    private String serialNumber;

    @Column(nullable = false, length = 60)
    private String brand;

    @Column(nullable = false, length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_need", nullable = false, length = 20)
    private MaintenanceNeed maintenanceNeed;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays;

    @Embedded
    private Location location;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EquipmentEntity() {
    }

    public EquipmentEntity(EquipmentType type, String serialNumber, String brand, String model, Agency agency,
                           EquipmentStatus status, MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate,
                           Integer maintenanceIntervalDays, Location location) {
        this.type = type;
        this.serialNumber = serialNumber;
        this.brand = brand;
        this.model = model;
        this.agency = agency;
        this.status = status;
        this.maintenanceNeed = maintenanceNeed;
        this.nextMaintenanceDate = nextMaintenanceDate;
        this.maintenanceIntervalDays = maintenanceIntervalDays;
        this.location = location;
    }

    public UUID getId() {
        return id;
    }

    public EquipmentType getType() {
        return type;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Agency getAgency() {
        return agency;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public MaintenanceNeed getMaintenanceNeed() {
        return maintenanceNeed;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public Integer getMaintenanceIntervalDays() {
        return maintenanceIntervalDays;
    }

    public Location getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
