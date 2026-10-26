package br.com.dsin.challengefogo.equipment.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EquipmentStatusHistoryRepository extends JpaRepository<EquipmentStatusHistoryEntity, UUID> {

    Page<EquipmentStatusHistoryEntity> findByEquipmentIdOrderByChangedAtDesc(UUID equipmentId, Pageable pageable);
}
