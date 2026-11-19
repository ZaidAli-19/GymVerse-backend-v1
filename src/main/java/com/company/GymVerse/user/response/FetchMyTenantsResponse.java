package com.company.GymVerse.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for FETCH_MY_TENANTS operation containing the user's tenant directory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchMyTenantsResponse {

    private List<UserTenantSummaryResponse> tenants;
    private int totalTenants;
}
