package com.company.GymVerse.tenantAdmin.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unified executive Admin Dashboard snapshot returned by FETCH_ADMIN_DASHBOARD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalMembers;
    private long liveMembers;
    private BigDecimal totalRevenue;
    private BigDecimal activeRate;
    private List<DashboardTierBreakdownItem> activeByTier;
    private DashboardMembershipCard membershipCard;
    private DashboardRevenueCardResponse revenueCard;
    private DashboardExpiryCardResponse membershipExpiryCard;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMembershipCard {
        private long totalMembers;
        private List<DashboardTierBreakdownItem> tierBreakdown;
    }
}
