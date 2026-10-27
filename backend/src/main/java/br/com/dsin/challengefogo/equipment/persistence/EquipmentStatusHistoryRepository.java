package br.com.dsin.challengefogo.equipment.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.com.dsin.challengefogo.equipment.domain.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EquipmentStatusHistoryRepository extends JpaRepository<EquipmentStatusHistoryEntity, UUID> {

    Page<EquipmentStatusHistoryEntity> findByEquipmentIdOrderByChangedAtDesc(UUID equipmentId, Pageable pageable);

    /** Quando cada equipamento entrou pela última vez na situação: o "desde" dos alertas. */
    @Query("select new br.com.dsin.challengefogo.equipment.persistence.StatusSinceView(h.equipmentId, max(h.changedAt)) "
            + "from EquipmentStatusHistoryEntity h where h.toStatus = :status and h.equipmentId in :equipmentIds "
            + "group by h.equipmentId")
    List<StatusSinceView> findLastEntryInto(EquipmentStatus status, Collection<UUID> equipmentIds);
}
