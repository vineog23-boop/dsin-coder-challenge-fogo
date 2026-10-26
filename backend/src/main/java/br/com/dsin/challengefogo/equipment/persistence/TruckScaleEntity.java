package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "truck_scale")
public class TruckScaleEntity extends EquipmentEntity {

    @Column(name = "max_capacity_tons", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxCapacityTons;

    /** Pesa com o caminhão em movimento, sem parar no posto. */
    @Column(name = "weigh_in_motion", nullable = false)
    private boolean weighInMotion;

    protected TruckScaleEntity() {
    }

    public TruckScaleEntity(String serialNumber, String brand, String model, Agency agency, EquipmentStatus status,
                            MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate,
                            Integer maintenanceIntervalDays, Location location, BigDecimal maxCapacityTons,
                            boolean weighInMotion) {
        super(EquipmentType.TRUCK_SCALE, serialNumber, brand, model, agency, status, maintenanceNeed,
                nextMaintenanceDate, maintenanceIntervalDays, location);
        this.maxCapacityTons = maxCapacityTons;
        this.weighInMotion = weighInMotion;
    }

    public void updateCharacteristics(BigDecimal maxCapacityTons, boolean weighInMotion) {
        this.maxCapacityTons = maxCapacityTons;
        this.weighInMotion = weighInMotion;
    }

    public BigDecimal getMaxCapacityTons() {
        return maxCapacityTons;
    }

    public boolean isWeighInMotion() {
        return weighInMotion;
    }
}
