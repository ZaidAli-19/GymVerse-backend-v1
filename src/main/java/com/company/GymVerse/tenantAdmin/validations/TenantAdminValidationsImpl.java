package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.exceptions.common.ForbiddenException;
import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Concrete implementation of {@link TenantAdminValidations} enforcing tenant boundary,
 * role authorization, pagination whitelists, and domain invariants.
 */
@Component
@RequiredArgsConstructor
public class TenantAdminValidationsImpl implements TenantAdminValidations {

    private static final Set<String> ALLOWED_PLAN_SORT_FIELDS = Set.of(
            "id", "name", "basePrice", "finalPrice", "durationDays", "active", "tier", "planTier.name"
    );
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final MembershipPlanRepository membershipPlanRepository;
    private final PlanTierRepository planTierRepository;

    @Override
    public void validateTenantAdminContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()))) {
            throw new BadCredentialsException("Full authentication is required to access tenant administration endpoints");
        }

        if (TenantContext.getCurrentTenant() == null) {
            throw new ForbiddenException("Tenant-scoped admin token required; master account tokens cannot access tenant administration endpoints");
        }

        boolean hasTenantAdminRole = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TENANT_ADMIN".equalsIgnoreCase(a.getAuthority()));

        if (!hasTenantAdminRole) {
            throw new ForbiddenException("Access denied: caller does not possess ROLE_TENANT_ADMIN authority");
        }
    }

    @Override
    public void validateRequest(final TenantAdminRequest request) {
        if (request == null) {
            throw new BadRequestException("TenantAdminRequest payload must not be null");
        }
        if (request.getTenantAdminRequestType() == null) {
            throw new BadRequestException("tenantAdminRequestType must not be null");
        }
    }

    @Override
    public Pageable validateAndBuildPageable(
            final Integer page,
            final Integer size,
            final String sortBy,
            final String sortDirection,
            final String defaultSortBy,
            final Set<String> allowedSortFields
    ) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;

        if (resolvedPage < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        if (resolvedSize < 1 || resolvedSize > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }

        String resolvedSortBy = (sortBy != null && !sortBy.trim().isBlank())
                ? sortBy.trim()
                : defaultSortBy;

        if (allowedSortFields != null && !allowedSortFields.contains(resolvedSortBy)) {
            throw new BadRequestException("Invalid sortBy field: '" + resolvedSortBy + "'. Allowed fields: " + allowedSortFields);
        }

        String resolvedDir = (sortDirection != null && !sortDirection.trim().isBlank())
                ? sortDirection.trim()
                : "asc";

        if (!"asc".equalsIgnoreCase(resolvedDir) && !"desc".equalsIgnoreCase(resolvedDir)) {
            throw new BadRequestException("sortDirection must be 'asc' or 'desc'");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortBy));
    }

    @Override
    public Pageable validateFetchPlans(final FetchPlansRequest request) {
        if (request == null) {
            return validateAndBuildPageable(null, null, null, null, "id", ALLOWED_PLAN_SORT_FIELDS);
        }
        String sortField = "tier".equalsIgnoreCase(request.getSortBy()) ? "planTier.name" : request.getSortBy();
        return validateAndBuildPageable(
                request.getPage(),
                request.getSize(),
                sortField,
                request.getSortDirection(),
                "id",
                ALLOWED_PLAN_SORT_FIELDS
        );
    }

    @Override
    public void validateCreatePlan(final CreatePlanRequest request) {
        if (request == null) {
            throw new BadRequestException("createPlanRequest payload must not be null");
        }
        validatePlanName(request.getName());
        validateTierAndTheme(request.getTier(), request.getCustomTheme());
        validateFacilities(request.getFacilities());
        validatePricingAndDiscount(request.getBasePrice(), request.getDiscountType(), request.getDiscountValue());

        if (request.getDurationDays() == null || request.getDurationDays() < 1) {
            throw new BadRequestException("durationDays must be at least 1");
        }
    }

    @Override
    public void validateUpdatePlanAvailability(final UpdatePlanAvailabilityRequest request) {
        if (request == null) {
            throw new BadRequestException("updatePlanAvailabilityRequest payload must not be null");
        }
        if (request.getPlanId() == null || request.getPlanId() <= 0) {
            throw new BadRequestException("planId must be a positive identifier");
        }
        if (request.getActive() == null) {
            throw new BadRequestException("active status flag must not be null");
        }
    }

    private void validatePlanName(final String name) {
        if (name == null || name.trim().isBlank()) {
            throw new BadRequestException("Plan name must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new BadRequestException("Plan name must not exceed 100 characters");
        }
        if (membershipPlanRepository.existsByNameIgnoreCase(trimmed)) {
            throw new ConflictException("A membership plan with name '" + trimmed + "' already exists");
        }
    }

    private void validateTierAndTheme(final String tier, final PlanTheme customTheme) {
        if (tier == null || tier.trim().isBlank()) {
            throw new BadRequestException("Plan tier must not be blank");
        }
        String normalizedTier = tier.trim().toUpperCase();
        if (normalizedTier.length() > 50) {
            throw new BadRequestException("Tier name must not exceed 50 characters");
        }

        boolean isDefaultTier = PlanTheme.resolveDefaultTierTheme(normalizedTier).isPresent();
        if (isDefaultTier) {
            return;
        }

        boolean tierAlreadyExists = planTierRepository.existsByNameIgnoreCase(normalizedTier);
        if (!tierAlreadyExists) {
            if (customTheme == null) {
                throw new BadRequestException("customTheme is required when creating a new custom tier");
            }
            if (!customTheme.isCustomTheme()) {
                throw new BadRequestException("Theme '" + customTheme + "' is reserved for default tiers; choose one of the 20 custom themes");
            }
        } else if (customTheme != null && !customTheme.isCustomTheme()) {
            throw new BadRequestException("Theme '" + customTheme + "' is reserved for default tiers");
        }
    }

    private void validateFacilities(final java.util.List<String> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            throw new BadRequestException("At least one facility must be specified for a membership plan");
        }
        boolean hasValidFacility = facilities.stream().anyMatch(f -> f != null && !f.trim().isBlank());
        if (!hasValidFacility) {
            throw new BadRequestException("Facilities list must contain at least one non-blank facility name");
        }
        boolean exceedsLength = facilities.stream()
                .filter(f -> f != null && !f.trim().isBlank())
                .anyMatch(f -> f.trim().length() > 100);
        if (exceedsLength) {
            throw new BadRequestException("Each facility name must not exceed 100 characters");
        }
    }

    private void validatePricingAndDiscount(
            final BigDecimal basePrice,
            final DiscountType discountType,
            final BigDecimal discountValue
    ) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("basePrice must be a non-negative decimal amount");
        }
        if (basePrice.scale() > 2) {
            throw new BadRequestException("basePrice cannot have more than 2 decimal places");
        }
        if (discountType == null && discountValue != null) {
            throw new BadRequestException("discountType is required when discountValue is provided");
        }
        if (discountType != null) {
            if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("discountValue must be greater than 0 when discountType is specified");
            }
            if (discountValue.scale() > 2) {
                throw new BadRequestException("discountValue cannot have more than 2 decimal places");
            }
            if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(ONE_HUNDRED) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100");
            }
            if (discountType == DiscountType.FLAT && discountValue.compareTo(basePrice) > 0) {
                throw new BadRequestException("Flat discount cannot exceed basePrice");
            }
        }
    }
}
