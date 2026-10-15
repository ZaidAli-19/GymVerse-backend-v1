package com.company.GymVerse.platformAdmin.specifications;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic SaaS subscription plan querying.
 */
public class SaasPlanSpecification {

    private SaasPlanSpecification() {
        // Utility class
    }

    /**
     * Builds a composable Specification combining filters for active status, billing cycle,
     * case-insensitive partial name match, and price ranges.
     *
     * @param active       optional active flag filter
     * @param billingCycle optional billing cycle enum filter
     * @param name         optional case-insensitive partial name query
     * @param minPrice     optional minimum plan price boundary
     * @param maxPrice     optional maximum plan price boundary
     * @return JPA Specification combining all non-null predicates with AND
     */
    public static Specification<SaasSubscriptionPlan> withFilters(
            Boolean active,
            BillingCycle billingCycle,
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (billingCycle != null) {
                predicates.add(cb.equal(root.get("billingCycle"), billingCycle));
            }

            if (name != null && !name.trim().isBlank()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
