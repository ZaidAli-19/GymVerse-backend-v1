package com.company.GymVerse.tenantAdmin.response.finance;

import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive Finance analytics overview returned by FETCH_FINANCE_OVERVIEW.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceOverviewResponse {

    private FinancePeriodType periodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private BigDecimal budgetUsedPercentage;
    private List<ExpenseCategoryBreakdownResponse> expenseBreakdown;
    private CollectionBreakdownResponse collectionBreakdown;
    private RevenueSourceBreakdownResponse revenueBreakdown;
}
