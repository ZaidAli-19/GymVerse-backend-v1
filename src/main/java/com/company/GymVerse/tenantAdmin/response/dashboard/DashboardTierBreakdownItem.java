package com.company.GymVerse.tenantAdmin.response.dashboard;

import com.company.GymVerse.models.tenant.enums.PlanTheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tier count entry used in AdminDashboardResponse active-by-tier and membership cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTierBreakdownItem {

    private String tierName;
    private PlanTheme themeKey;
    private long count;
}
