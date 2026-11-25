package com.company.GymVerse.tenantAdmin.response.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Revenue source breakdown (NEW_MEMBERSHIP, RENEWAL, OTHER) within FinanceOverviewResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSourceBreakdownResponse {

    private BigDecimal newMembershipRevenue;
    private BigDecimal renewalRevenue;
    private BigDecimal otherRevenue;
    private BigDecimal totalRevenue;
}
