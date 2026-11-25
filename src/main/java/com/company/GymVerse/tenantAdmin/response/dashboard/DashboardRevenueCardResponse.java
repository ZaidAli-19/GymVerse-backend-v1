package com.company.GymVerse.tenantAdmin.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Executive revenue summary card returned in AdminDashboardResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRevenueCardResponse {

    private BigDecimal totalRevenue;
    private BigDecimal newMembershipRevenue;
    private BigDecimal renewalRevenue;
    private BigDecimal otherRevenue;
}
