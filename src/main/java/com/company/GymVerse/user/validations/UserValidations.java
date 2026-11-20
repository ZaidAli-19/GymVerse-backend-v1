package com.company.GymVerse.user.validations;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.user.request.UserRequest;

/**
 * Validation interface defining domain invariant and constraint checks for central master user operations.
 */
public interface UserValidations {

    /**
     * Validates domain invariants and required fields for a REGISTER_GYM request payload.
     *
     * @param request the user request containing the registration payload
     */
    void validateRegisterGym(UserRequest request);

    /**
     * Validates format and ensures uniqueness of a custom gym code against registered tenants.
     *
     * @param customGymCode the custom gym code to evaluate
     */
    void validateCustomGymCode(String customGymCode);

    /**
     * Validates that the active request context is a fully authenticated master user account,
     * rejecting any tenant-scoped tokens or unauthenticated callers.
     */
    void validateMasterContext();

    /**
     * Validates domain invariants and required fields for a FETCH_GYM_BY_CODE request payload.
     *
     * @param request the user request containing the gym discovery payload
     */
    void validateFetchGymByCode(UserRequest request);

    /**
     * Validates that a gym tenant is ACTIVE and in good standing with its SaaS subscription to accept discovery.
     *
     * @param gymTenant          the target gym tenant
     * @param subscriptionStatus current SaaS subscription status
     */
    void validateGymActiveForJoin(GymTenant gymTenant, GymSubscriptionStatus subscriptionStatus);
}
