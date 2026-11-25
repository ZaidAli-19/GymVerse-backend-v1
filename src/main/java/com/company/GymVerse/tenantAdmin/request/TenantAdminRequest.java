package com.company.GymVerse.tenantAdmin.request;

import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.subscription.FetchTenantSubscriptionRequest;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request envelope for all tenant-scoped administrative operations on /api/tenant-admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminRequest {

    @JsonAlias({"tenantAdminRequestType", "requestType"})
    private TenantAdminRequestType tenantAdminRequestType;

    @JsonAlias({"fetchTenantSubscriptionRequest", "fetchTenantSubscription"})
    private FetchTenantSubscriptionRequest fetchTenantSubscriptionRequest;
}
