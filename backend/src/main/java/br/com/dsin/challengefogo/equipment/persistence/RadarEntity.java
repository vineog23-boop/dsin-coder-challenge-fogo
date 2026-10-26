package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.equipment.domain.RadarModality;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "radar")
public class RadarEntity extends EquipmentEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RadarModality modality;

    /** Obrigatório só na modalidade {@code SPEED}; o banco também confere. */
    @Column(name = "speed_limit_kmh")
    private Integer speedLimitKmh;

    private Integer lanes;

    protected RadarEntity() {
    }

    public RadarEntity(String serialNumber, String brand, String model, Agency agency, EquipmentStatus status,
                       MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate, Integer maintenanceIntervalDays,
                       Location location, RadarModality modality, Integer speedLimitKmh, Integer lanes) {
        super(EquipmentType.RADAR, serialNumber, brand, model, agency, status, maintenanceNeed, nextMaintenanceDate,
                maintenanceIntervalDays, location);
        this.modality = modality;
        this.speedLimitKmh = speedLimitKmh;
        this.lanes = lanes;
    }

    public void updateCharacteristics(RadarModality modality, Integer speedLimitKmh, Integer lanes) {
        this.modality = modality;
        this.speedLimitKmh = speedLimitKmh;
        this.lanes = lanes;
    }

    public RadarModality getModality() {
        return modality;
    }

    public Integer getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    public Integer getLanes() {
        return lanes;
    }
}
