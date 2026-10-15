package com.company.GymVerse.platformAdmin.validations;

import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;

/**
 * Validation interface defining domain invariant and permission checks for platform administration.
 */
public interface PlatformAdminValidations {

    /**
     * Validates domain invariants and uniqueness for creating a SaaS subscription plan.
     *
     * @param request the platform admin request containing plan details
     */
    void validateCreatePlan(PlatformAdminRequest request);

    /**
     * Validates pagination boundaries, sort field whitelist, and price ranges for querying plans.
     *
     * @param request the platform admin request containing query parameters
     */
    void validateFetchPlans(PlatformAdminRequest request);

    /**
     * Validates that a plan ID is provided and is a positive non-zero number.
     *
     * @param request the platform admin request containing the plan ID
     */
    void validateFetchPlanById(PlatformAdminRequest request);

    /**
     * Validates plan ID and boolean active status for toggling plan availability.
     *
     * @param request the platform admin request containing planId and active flag
     */
    void validateUpdatePlanAvailability(PlatformAdminRequest request);

    /**
     * Validates pagination boundaries, sort field whitelist, and filter parameters for querying gym tenants.
     *
     * @param request the platform admin request containing query parameters
     */
    void validateFetchGymTenants(PlatformAdminRequest request);
}

