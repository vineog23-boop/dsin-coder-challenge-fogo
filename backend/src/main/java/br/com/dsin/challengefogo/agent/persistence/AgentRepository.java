package br.com.dsin.challengefogo.agent.persistence;

import br.com.dsin.challengefogo.agent.domain.AgentStatus;
import br.com.dsin.challengefogo.shared.domain.Agency;
import br.com.dsin.challengefogo.shared.domain.Region;
import br.com.dsin.challengefogo.shared.persistence.RegionCount;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID>, JpaSpecificationExecutor<AgentEntity> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

    /** Traz a viatura junto: a listagem mostra a placa de cada agente sem uma consulta por linha. */
    @Override
    @EntityGraph(attributePaths = "vehicle")
    Page<AgentEntity> findAll(Specification<AgentEntity> spec, Pageable pageable);

    // ---------- central de operações (Parte 2) ----------

    @Query("select new br.com.dsin.challengefogo.agent.persistence.AgentStatusCount(a.status, count(a)) "
            + "from AgentEntity a group by a.status")
    List<AgentStatusCount> countByStatus();

    @Query("select new br.com.dsin.challengefogo.shared.persistence.RegionCount(a.baseLocation.region, count(a)) "
            + "from AgentEntity a where a.status in :statuses group by a.baseLocation.region")
    List<RegionCount> countByRegionWithStatusIn(Collection<AgentStatus> statuses);

    @Query("select new br.com.dsin.challengefogo.agent.persistence.AgentMarkerView(a.id, a.name, a.agentType, "
            + "a.status, a.currentLatitude, a.currentLongitude, a.positionUpdatedAt, a.baseLocation.latitude, "
            + "a.baseLocation.longitude, a.baseLocation.region, v.licensePlate) "
            + "from AgentEntity a left join a.vehicle v where a.status in :statuses "
            + "and (:region is null or a.baseLocation.region = :region) and (:agency is null or a.agency = :agency) "
            + "order by a.name")
    List<AgentMarkerView> findMarkers(Collection<AgentStatus> statuses, Region region, Agency agency, Limit limit);

    /** Agentes que a telemetria simulada move. */
    List<AgentEntity> findByStatusIn(Collection<AgentStatus> statuses);
}
