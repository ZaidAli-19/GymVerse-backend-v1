package com.company.GymVerse.tenantAdmin.response.plans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Plans mini-dashboard payload returned by FETCH_PLANS_SUMMARY.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlansSummaryResponse {

    private long totalActivePlans;
    private long totalPlans;
    private List<TierCountResponse> tierBreakdown;
}
