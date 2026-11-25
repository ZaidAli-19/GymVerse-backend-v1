package com.company.GymVerse.tenantAdmin.response;

import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard unified response wrapper for tenant-scoped operations on /api/tenant-admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantAdminResponse {

    private String message;
    private int status;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private TenantSubscriptionDetailsResponse tenantSubscriptionDetailsResponse;
}
