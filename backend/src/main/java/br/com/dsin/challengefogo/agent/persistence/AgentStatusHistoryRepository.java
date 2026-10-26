package br.com.dsin.challengefogo.agent.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentStatusHistoryRepository extends JpaRepository<AgentStatusHistoryEntity, UUID> {

    Page<AgentStatusHistoryEntity> findByAgentIdOrderByChangedAtDesc(UUID agentId, Pageable pageable);
}
