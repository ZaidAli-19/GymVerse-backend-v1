package com.company.GymVerse.tenantAdmin.specifications.members;

import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Spring Data JPA Specification builder for querying tenant join requests.
 */
public final class JoinRequestSpecification {

    private JoinRequestSpecification() {
    }

    /**
     * Composes dynamic filters for join request status and case-insensitive applicant search.
     */
    public static Specification<JoinRequest> withFilters(
            final JoinRequestStatus status,
            final String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("phoneNumber"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
