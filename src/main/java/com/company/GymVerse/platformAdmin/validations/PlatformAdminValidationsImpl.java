package com.company.GymVerse.platformAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.request.SaasPlanFilterRequest;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation of domain invariant and permission checks for platform administration.
 * Provides unified, reusable pagination and whitelist-guarded sorting validations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminValidationsImpl implements PlatformAdminValidations {

    private static final List<String> ALLOWED_PLAN_SORT_FIELDS = List.of(
            "id", "name", "price", "billingCycle", "maxMembers", "maxTrainers"
    );

    private static final List<String> ALLOWED_TENANT_SORT_FIELDS = List.of(
            "id", "gymName", "gymCode", "city", "status", "registeredAt"
    );

    private final SaasSubscriptionPlanRepository planRepository;

    @Override
    public void validateCreatePlan(PlatformAdminRequest request) {
        if (request == null) {
            throw new BadRequestException("Plan creation request cannot be null");
        }

        // Validate plan name
        String name = request.getName();
        if (name == null || name.trim().isBlank()) {
            throw new BadRequestException("Plan name cannot be blank");
        }
        name = name.trim();
        if (name.length() > 100) {
            throw new BadRequestException("Plan name must not exceed 100 characters");
        }

        // Check for duplicate plan name (case-insensitive)
        if (planRepository.existsByNameIgnoreCase(name)) {
            log.warn("Plan creation rejected: Duplicate plan name '{}'", name);
            throw new ConflictException("A plan with name '" + name + "' already exists");
        }

        // Validate price
        BigDecimal price = request.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price must be a non-negative decimal value");
        }
        if (price.scale() > 2) {
            throw new BadRequestException("Price cannot have more than 2 decimal places");
        }

        // Validate billing cycle
        if (request.getBillingCycle() == null) {
            throw new BadRequestException("Billing cycle is required");
        }

        // Validate capacity limits
        Integer maxMembers = request.getMaxMembers();
        Integer maxTrainers = request.getMaxTrainers();
        if (maxMembers == null || maxMembers < 1 || maxTrainers == null || maxTrainers < 0) {
            throw new BadRequestException("Maximum members must be at least 1 and trainers must be non-negative");
        }
    }

    @Override
    public void validateFetchPlans(PlatformAdminRequest request) {
        if (request == null) {
            return;
        }

        // 1. Validate common pagination and sorting at root
        validatePagination(request.getPage(), request.getSize(), request.getSortBy(), request.getSortDirection(),
                ALLOWED_PLAN_SORT_FIELDS);

        // 2. Validate planFilter criteria if provided
        SaasPlanFilterRequest filter = request.getPlanFilter();
        if (filter != null) {
            BigDecimal minPrice = filter.getMinPrice();
            BigDecimal maxPrice = filter.getMaxPrice();

            if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("minPrice must not be negative");
            }
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("maxPrice must not be negative");
            }
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                throw new BadRequestException("minPrice cannot be greater than maxPrice");
            }
        }
    }

    @Override
    public void validateFetchPlanById(PlatformAdminRequest request) {
        if (request == null || request.getPlanId() == null) {
            throw new BadRequestException("Plan ID is required");
        }
        if (request.getPlanId() <= 0) {
            throw new BadRequestException("Plan ID must be a positive number");
        }
    }

    @Override
    public void validateUpdatePlanAvailability(PlatformAdminRequest request) {
        if (request == null) {
            throw new BadRequestException("Request payload cannot be null");
        }

        Long planId = request.getPlanId() != null ? request.getPlanId()
                : (request.getPlanAvailability() != null ? request.getPlanAvailability().getPlanId() : null);

        if (planId == null) {
            throw new BadRequestException("Plan ID is required");
        }
        if (planId <= 0) {
            throw new BadRequestException("Plan ID must be a positive number");
        }

        Boolean active = request.getActive() != null ? request.getActive()
                : (request.getPlanAvailability() != null ? request.getPlanAvailability().getActive() : null);

        if (active == null) {
            throw new BadRequestException("Plan availability status must be specified as true or false");
        }
    }

    @Override
    public void validateFetchGymTenants(PlatformAdminRequest request) {
        if (request == null) {
            return;
        }

        // Validate common pagination and sorting at root
        validatePagination(request.getPage(), request.getSize(), request.getSortBy(), request.getSortDirection(),
                ALLOWED_TENANT_SORT_FIELDS);
    }

    /**
     * Reusable helper validating standard pagination boundaries and whitelist-guarded sorting parameters.
     */
    private void validatePagination(Integer page, Integer size, String sortBy, String sortDirection,
                                    List<String> allowedSortFields) {
        // Validate pagination boundaries
        if (page != null && page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size != null && (size < 1 || size > 100)) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        // Validate sort field whitelist
        if (sortBy != null && !sortBy.trim().isBlank()) {
            String trimmedSort = sortBy.trim();
            if (!allowedSortFields.contains(trimmedSort)) {
                log.warn("Invalid sort field rejected: '{}'", trimmedSort);
                throw new BadRequestException("Invalid sort field: '" + trimmedSort + "'. Allowed fields: " + allowedSortFields);
            }
        }

        // Validate sort direction
        if (sortDirection != null && !sortDirection.trim().isBlank()) {
            String trimmedDir = sortDirection.trim().toLowerCase();
            if (!"asc".equals(trimmedDir) && !"desc".equals(trimmedDir)) {
                throw new BadRequestException("Invalid sort direction: '" + sortDirection + "'. Allowed values: [asc, desc]");
            }
        }
    }
}
