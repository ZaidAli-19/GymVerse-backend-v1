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
}
