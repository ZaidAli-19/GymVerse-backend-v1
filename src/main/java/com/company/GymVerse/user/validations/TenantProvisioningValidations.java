package com.company.GymVerse.user.validations;

import com.company.GymVerse.user.dto.TenantProvisioningCommand;

/**
 * Validation interface defining domain invariant and constraint checks for tenant provisioning.
 */
public interface TenantProvisioningValidations {

    /**
     * Validates domain invariants and required fields for tenant provisioning.
     *
     * @param command the tenant provisioning command containing user and gym details
     */
    void validateProvisioningCommand(TenantProvisioningCommand command);

    /**
     * Validates format and ensures uniqueness of a custom gym code against registered tenants.
     *
     * @param customGymCode the custom gym code to evaluate
     */
    void validateCustomGymCode(String customGymCode);
}
