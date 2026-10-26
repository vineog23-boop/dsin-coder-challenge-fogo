package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.ScooterOwnership;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "scooter")
public class ScooterEntity extends EquipmentEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScooterOwnership ownership;

    /** Operadora do patinete compartilhado; obrigatória quando {@code SHARED}. */
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    /** Carga da bateria, de 0 a 100. */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    protected ScooterEntity() {
    }

    public ScooterEntity(String serialNumber, String brand, String model, Agency agency, EquipmentStatus status,
                         MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate,
                         Integer maintenanceIntervalDays, Location location, ScooterOwnership ownership,
                         String operatorName, Integer batteryLevel) {
        super(EquipmentType.ELECTRIC_SCOOTER, serialNumber, brand, model, agency, status, maintenanceNeed,
                nextMaintenanceDate, maintenanceIntervalDays, location);
        this.ownership = ownership;
        this.operatorName = operatorName;
        this.batteryLevel = batteryLevel;
    }

    public void updateCharacteristics(ScooterOwnership ownership, String operatorName, Integer batteryLevel) {
        this.ownership = ownership;
        this.operatorName = operatorName;
        this.batteryLevel = batteryLevel;
    }

    public ScooterOwnership getOwnership() {
        return ownership;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }
}
