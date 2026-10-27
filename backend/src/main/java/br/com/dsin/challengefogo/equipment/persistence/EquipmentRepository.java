package br.com.dsin.challengefogo.equipment.persistence;

import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import br.com.dsin.challengefogo.equipment.domain.EquipmentType;
import br.com.dsin.challengefogo.equipment.domain.MaintenanceNeed;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
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

    // ---------- central de operações (Parte 2): agregações feitas no banco ----------

    @Query("select new br.com.dsin.challengefogo.equipment.persistence.EquipmentStatusCount(e.status, count(e)) "
            + "from EquipmentEntity e group by e.status")
    List<EquipmentStatusCount> countByStatus();

    long countByMaintenanceNeed(MaintenanceNeed maintenanceNeed);

    /** Regra 3 da spec operations: agendada, vencida e não desativada. */
    @Query("select count(e) from EquipmentEntity e where e.maintenanceNeed = MaintenanceNeed.SCHEDULED "
            + "and e.nextMaintenanceDate < :today and e.status <> EquipmentStatus.DEACTIVATED")
    long countOverdueMaintenance(LocalDate today);

    @Query("select new br.com.dsin.challengefogo.shared.persistence.RegionCount(e.location.region, count(e)) "
            + "from EquipmentEntity e where e.status = :status group by e.location.region")
    List<RegionCount> countByRegionWithStatus(EquipmentStatus status);

    @Query("select new br.com.dsin.challengefogo.equipment.persistence.EquipmentMarkerView(e.id, e.type, "
            + "e.serialNumber, e.status, e.maintenanceNeed, e.location.latitude, e.location.longitude, "
            + "e.location.region) from EquipmentEntity e "
            + "where (:type is null or e.type = :type) and (:status is null or e.status = :status) "
            + "and (:region is null or e.location.region = :region) and (:agency is null or e.agency = :agency) "
            + "order by e.serialNumber")
    List<EquipmentMarkerView> findMarkers(EquipmentType type, EquipmentStatus status, Region region, Agency agency,
                                          Limit limit);

    /** Candidatos a alerta: com falha, roubados ou com manutenção atrasada (regras 3 e 5). */
    @Query("select new br.com.dsin.challengefogo.equipment.persistence.EquipmentAlertView(e.id, e.type, "
            + "e.serialNumber, e.status, e.maintenanceNeed, e.nextMaintenanceDate, e.location.region) "
            + "from EquipmentEntity e where e.status in (EquipmentStatus.MALFUNCTION, EquipmentStatus.STOLEN) "
            + "or (e.maintenanceNeed = MaintenanceNeed.SCHEDULED and e.nextMaintenanceDate < :today "
            + "and e.status <> EquipmentStatus.DEACTIVATED)")
    List<EquipmentAlertView> findAlertCandidates(LocalDate today);
}
