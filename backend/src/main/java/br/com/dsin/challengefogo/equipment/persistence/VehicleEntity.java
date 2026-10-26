package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Veículo monitorado (táxi, viatura, transporte escolar...). Vários tipos compartilham esta tabela,
 * por isso recebe o tipo no construtor. A localização é a da base (garagem ou ponto).
 */
@Entity
@Table(name = "vehicle")
public class VehicleEntity extends EquipmentEntity {

    /** Placa sem hífen, padrão Mercosul ou antigo; única entre todos os veículos. */
    @Column(name = "license_plate", nullable = false, unique = true, length = 7)
    private String licensePlate;

    @Column(name = "passenger_capacity")
    private Integer passengerCapacity;

    @Column(nullable = false)
    private boolean autonomous;

    protected VehicleEntity() {
    }

    public VehicleEntity(EquipmentType type, String serialNumber, String brand, String model, Agency agency,
                         EquipmentStatus status, MaintenanceNeed maintenanceNeed, LocalDate nextMaintenanceDate,
                         Integer maintenanceIntervalDays, Location location, String licensePlate,
                         Integer passengerCapacity, boolean autonomous) {
        super(type, serialNumber, brand, model, agency, status, maintenanceNeed, nextMaintenanceDate,
                maintenanceIntervalDays, location);
        this.licensePlate = licensePlate;
        this.passengerCapacity = passengerCapacity;
        this.autonomous = autonomous;
    }

    public void updateCharacteristics(String licensePlate, Integer passengerCapacity, boolean autonomous) {
        this.licensePlate = licensePlate;
        this.passengerCapacity = passengerCapacity;
        this.autonomous = autonomous;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public Integer getPassengerCapacity() {
        return passengerCapacity;
    }

    public boolean isAutonomous() {
        return autonomous;
    }
}
