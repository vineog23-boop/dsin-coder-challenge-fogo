package br.com.dsin.challengefogo.equipment.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EquipmentMaintenanceRepository extends JpaRepository<EquipmentMaintenanceEntity, UUID> {

    Page<EquipmentMaintenanceEntity> findByEquipmentIdOrderByPerformedAtDesc(UUID equipmentId, Pageable pageable);
}
