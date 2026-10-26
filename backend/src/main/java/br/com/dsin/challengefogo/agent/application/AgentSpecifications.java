package br.com.dsin.challengefogo.agent.application;

import br.com.dsin.challengefogo.agent.application.dto.AgentFilter;
import br.com.dsin.challengefogo.agent.persistence.AgentEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Consulta da listagem de agentes; filtro nulo ou em branco não restringe nada. */
final class AgentSpecifications {

    private AgentSpecifications() {
    }

    static Specification<AgentEntity> from(AgentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.agentType() != null) {
                predicates.add(cb.equal(root.get("agentType"), filter.agentType()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.agency() != null) {
                predicates.add(cb.equal(root.get("agency"), filter.agency()));
            }
            if (filter.region() != null) {
                predicates.add(cb.equal(root.get("baseLocation").get("region"), filter.region()));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("registrationNumber")), pattern, '\\')));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Busca literal: {@code %} e {@code _} digitados pelo usuário não viram curinga. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
