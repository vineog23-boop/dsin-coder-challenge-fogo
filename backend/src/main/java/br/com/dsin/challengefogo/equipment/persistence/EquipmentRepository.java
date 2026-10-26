package br.com.dsin.challengefogo.equipment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, UUID>,
        JpaSpecificationExecutor<EquipmentEntity> {

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, UUID id);

    // A placa é da subclasse: o método derivado do repositório da base não a enxerga.
    @Query("select count(v) > 0 from VehicleEntity v where v.licensePlate = :licensePlate")
    boolean existsByLicensePlate(String licensePlate);

    @Query("select count(v) > 0 from VehicleEntity v where v.licensePlate = :licensePlate and v.id <> :id")
    boolean existsByLicensePlateAndIdNot(String licensePlate, UUID id);
}
