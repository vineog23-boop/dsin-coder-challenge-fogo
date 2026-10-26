package br.com.dsin.challengefogo.equipment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, UUID> {
}
