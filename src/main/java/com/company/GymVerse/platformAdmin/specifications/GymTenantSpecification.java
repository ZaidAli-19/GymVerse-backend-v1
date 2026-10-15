package com.company.GymVerse.platformAdmin.specifications;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic GymTenant querying.
 */
public class GymTenantSpecification {

    private GymTenantSpecification() {
        // Utility class
    }

    /**
     * Builds a composable Specification combining filters for lifecycle status,
     * case-insensitive partial city match, case-insensitive partial gym name match,
     * and case-insensitive prefix gym code match.
     *
     * @param status  optional GymStatus enum filter
     * @param city    optional case-insensitive partial city query
     * @param gymName optional case-insensitive partial gym name query
     * @param gymCode optional case-insensitive prefix gym code query
     * @return JPA Specification combining all non-null predicates with AND
     */
    public static Specification<GymTenant> withFilters(
            GymStatus status,
            String city,
            String gymName,
            String gymCode) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (city != null && !city.trim().isBlank()) {
                String pattern = "%" + city.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("city")), pattern));
            }

            if (gymName != null && !gymName.trim().isBlank()) {
                String pattern = "%" + gymName.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("gymName")), pattern));
            }

            if (gymCode != null && !gymCode.trim().isBlank()) {
                String pattern = gymCode.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("gymCode")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
