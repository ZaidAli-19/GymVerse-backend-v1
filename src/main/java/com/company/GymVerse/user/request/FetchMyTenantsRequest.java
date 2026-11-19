package com.company.GymVerse.user.request;

import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for FETCH_MY_TENANTS operation.
 * Supports optional filtering by tenant status or user role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchMyTenantsRequest {

    /**
     * Optional filter by gym tenant operational status (e.g. ACTIVE, SUSPENDED).
     */
    private GymStatus gymStatus;

    /**
     * Optional filter by caller's role within the gym (e.g. TENANT_ADMIN, MEMBER).
     */
    private TenantRole role;
}
