package com.company.GymVerse.tenantAdmin.response.plans;

import com.company.GymVerse.models.tenant.enums.PlanTheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-tier plan count entry for the Plans mini-dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierCountResponse {

    private String tierName;
    private PlanTheme themeKey;
    private long activePlanCount;
    private long totalPlanCount;
}
