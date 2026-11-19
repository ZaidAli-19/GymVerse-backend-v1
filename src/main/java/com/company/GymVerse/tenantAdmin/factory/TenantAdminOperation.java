package com.company.GymVerse.tenantAdmin.factory;

import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;

/**
 * Strategy interface representing an executable tenant-scoped administration operation.
 */
public interface TenantAdminOperation {

    /**
     * Identifies the request type discriminator handled by this operation bean.
     *
     * @return the associated {@link TenantAdminRequestType}
     */
    TenantAdminRequestType getRequestType();

    /**
     * Executes the domain logic for the tenant administration operation.
     *
     * @param request the tenant admin request envelope
     * @return populated {@link TenantAdminResponse}
     */
    TenantAdminResponse execute(TenantAdminRequest request);
}
