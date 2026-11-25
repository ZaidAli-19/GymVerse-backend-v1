package com.company.GymVerse.tenantAdmin.specifications.finance;

import com.company.GymVerse.models.tenant.Expense;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Spring Data JPA Specification builder for querying the tenant expense ledger.
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {
    }

    /**
     * Composes dynamic filters for inclusive expenseDate range, ExpenseCategory,
     * and case-insensitive partial text search across title and description.
     */
    public static Specification<Expense> withFilters(
            final LocalDate startDate,
            final LocalDate endDate,
            final ExpenseCategory category,
            final String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
