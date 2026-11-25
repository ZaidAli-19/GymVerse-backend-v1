package com.company.GymVerse.tenantAdmin.specifications.trainers;

import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.Trainer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Spring Data JPA Specification builder for querying the tenant trainer address book.
 */
public final class TrainerSpecification {

    private TrainerSpecification() {
    }

    /**
     * Composes dynamic filters for active status, specialization, and case-insensitive search
     * across trainer user identity fields and specialization.
     */
    public static Specification<Trainer> withFilters(
            final Boolean active,
            final String specialization,
            final String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (specialization != null && !specialization.isBlank()) {
                String specPattern = "%" + specialization.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("specialization"), "")), specPattern));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<Trainer, TenantUser> userJoin = root.join("user", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(userJoin.get("firstName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("lastName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("phoneNumber"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("specialization"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
