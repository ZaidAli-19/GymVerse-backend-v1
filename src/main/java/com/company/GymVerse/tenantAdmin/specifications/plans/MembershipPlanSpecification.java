package com.company.GymVerse.tenantAdmin.specifications.plans;

import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Spring Data JPA Specification builder for querying tenant membership plans.
 */
public final class MembershipPlanSpecification {

    private MembershipPlanSpecification() {
    }

    /**
     * Composes dynamic filters for tier name, active discount offers, active status,
     * trial flag, and case-insensitive partial text search across name, description, and facilities.
     */
    public static Specification<MembershipPlan> withFilters(
            final String tier,
            final Boolean hasOffer,
            final Boolean active,
            final Boolean isTrial,
            final String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tier != null && !tier.isBlank() && !"ALL".equalsIgnoreCase(tier.trim())) {
                Join<MembershipPlan, PlanTier> tierJoin = root.join("planTier", JoinType.LEFT);
                predicates.add(cb.equal(cb.upper(tierJoin.get("name")), tier.trim().toUpperCase()));
            }

            if (Boolean.TRUE.equals(hasOffer)) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("discountType")),
                        cb.isNotNull(root.get("discountValue")),
                        cb.greaterThan(root.get("discountValue"), BigDecimal.ZERO)
                ));
            } else if (Boolean.FALSE.equals(hasOffer)) {
                predicates.add(cb.or(
                        cb.isNull(root.get("discountType")),
                        cb.isNull(root.get("discountValue")),
                        cb.lessThanOrEqualTo(root.get("discountValue"), BigDecimal.ZERO)
                ));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (isTrial != null) {
                predicates.add(cb.equal(root.get("trial"), isTrial));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<MembershipPlan, GymFacility> facilityJoin = root.join("facilities", JoinType.LEFT);
                if (query != null) {
                    query.distinct(true);
                }
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(facilityJoin.get("name"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
