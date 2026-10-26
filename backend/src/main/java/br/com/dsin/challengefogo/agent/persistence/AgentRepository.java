package br.com.dsin.challengefogo.agent.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID>, JpaSpecificationExecutor<AgentEntity> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

    /** Traz a viatura junto: a listagem mostra a placa de cada agente sem uma consulta por linha. */
    @Override
    @EntityGraph(attributePaths = "vehicle")
    Page<AgentEntity> findAll(Specification<AgentEntity> spec, Pageable pageable);
}
