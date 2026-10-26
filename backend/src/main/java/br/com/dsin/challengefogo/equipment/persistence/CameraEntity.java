package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.CameraResolution;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "camera")
public class CameraEntity extends EquipmentEntity {

    @Column(name = "night_vision", nullable = false)
    private boolean nightVision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CameraResolution resolution;

    @Column(nullable = false)
    private boolean color;

    /** Câmera que gira e dá zoom (pan-tilt-zoom). */
    @Column(nullable = false)
    private boolean ptz;

    protected CameraEntity() {
    }

    public CameraEntity(String serialNumber, String brand, String model, Agency agency, EquipmentStatus status,
                        MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate, Integer maintenanceIntervalDays,
                        Location location, boolean nightVision, CameraResolution resolution, boolean color,
                        boolean ptz) {
        super(EquipmentType.SURVEILLANCE_CAMERA, serialNumber, brand, model, agency, status, maintenanceNeed,
                nextMaintenanceDate, maintenanceIntervalDays, location);
        this.nightVision = nightVision;
        this.resolution = resolution;
        this.color = color;
        this.ptz = ptz;
    }

    public boolean isNightVision() {
        return nightVision;
    }

    public CameraResolution getResolution() {
        return resolution;
    }

    public boolean isColor() {
        return color;
    }

    public boolean isPtz() {
        return ptz;
    }
}
