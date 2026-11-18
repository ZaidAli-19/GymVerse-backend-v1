package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.tenantAdmin.dto.TenantProvisioningCommand;

/**
 * Validation interface defining domain invariant and constraint checks for tenant administration.
 */
public interface TenantAdminValidations {

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
