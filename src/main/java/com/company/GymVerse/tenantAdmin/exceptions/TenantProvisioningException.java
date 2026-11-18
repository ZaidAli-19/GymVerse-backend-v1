package com.company.GymVerse.tenantAdmin.exceptions;

/**
 * Exception thrown when dynamic tenant provisioning or database initialization encounters an unrecoverable error.
 */
public class TenantProvisioningException extends RuntimeException {

    public TenantProvisioningException(String message) {
        super(message);
    }

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
