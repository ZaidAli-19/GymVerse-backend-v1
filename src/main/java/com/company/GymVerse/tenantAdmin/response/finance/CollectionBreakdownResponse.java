package com.company.GymVerse.tenantAdmin.response.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cash vs online payment collection totals within FinanceOverviewResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionBreakdownResponse {

    private BigDecimal cashCollected;
    private BigDecimal onlineCollected;
    private BigDecimal totalCollected;
}
