package br.com.dsin.challengefogo.equipment.application;

import br.com.dsin.challengefogo.equipment.application.dto.EquipmentFilter;
import br.com.dsin.challengefogo.equipment.persistence.EquipmentEntity;
import br.com.dsin.challengefogo.equipment.persistence.VehicleEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Monta a consulta da listagem a partir dos filtros; filtro nulo ou em branco não restringe nada. */
final class EquipmentSpecifications {

    private EquipmentSpecifications() {
    }

    static Specification<EquipmentEntity> from(EquipmentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.agency() != null) {
                predicates.add(cb.equal(root.get("agency"), filter.agency()));
            }
            if (filter.maintenanceNeed() != null) {
                predicates.add(cb.equal(root.get("maintenanceNeed"), filter.maintenanceNeed()));
            }
            if (filter.region() != null) {
                predicates.add(cb.equal(root.get("location").get("region"), filter.region()));
            }
            if (hasText(filter.neighborhood())) {
                predicates.add(cb.equal(cb.lower(root.get("location").get("neighborhood")),
                        filter.neighborhood().trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(filter.search())) {
                String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";

                // A placa está na subclasse: subconsulta em vez de join, para não excluir quem não é veículo.
                Subquery<VehicleEntity> plate = query.subquery(VehicleEntity.class);
                Root<VehicleEntity> vehicle = plate.from(VehicleEntity.class);
                plate.select(vehicle).where(cb.equal(vehicle.get("id"), root.get("id")),
                        cb.like(cb.lower(vehicle.get("licensePlate")), pattern, '\\'));

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("serialNumber")), pattern, '\\'),
                        cb.like(cb.lower(root.get("brand")), pattern, '\\'),
                        cb.like(cb.lower(root.get("model")), pattern, '\\'),
                        cb.exists(plate)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Busca literal: {@code %} e {@code _} digitados pelo usuário não viram curinga. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
