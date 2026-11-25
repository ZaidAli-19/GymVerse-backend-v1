package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * Validation interface defining security context and constraint rules for tenant administration flows.
 */
public interface TenantAdminValidations {

    /**
     * Validates that the active request context is an authenticated tenant administrator session,
     * enforcing that TenantContext is bound, token is TENANT_ACCOUNT, and caller holds ROLE_TENANT_ADMIN.
     */
    void validateTenantAdminContext();

    /**
     * Validates domain invariants and non-null constraints for incoming TenantAdminRequest envelopes.
     *
     * @param request the tenant admin request envelope
     */
    void validateRequest(TenantAdminRequest request);

    /**
     * Validates pagination parameters and whitelist-guarded sorting, returning a constructed Pageable.
     */
    Pageable validateAndBuildPageable(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            String defaultSortBy,
            Set<String> allowedSortFields
    );

    /**
     * Validates pagination and sorting fields for FETCH_PLANS queries.
     */
    Pageable validateFetchPlans(FetchPlansRequest request);

    /**
     * Validates CreatePlanRequest fields, tier/theme constraints, facilities, pricing, and uniqueness.
     */
    void validateCreatePlan(CreatePlanRequest request);

    /**
     * Validates UpdatePlanAvailabilityRequest required fields.
     */
    void validateUpdatePlanAvailability(UpdatePlanAvailabilityRequest request);
}
