package com.company.GymVerse.tenantAdmin.request.subscription;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload request for FETCH_TENANT_SUBSCRIPTION operations.
 * Tenant identity is automatically resolved from authenticated token claims.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchTenantSubscriptionRequest {

    /**
     * Optional flag indicating whether historical invoices/subscriptions should be included.
     */
    private Boolean includeHistory;
}
