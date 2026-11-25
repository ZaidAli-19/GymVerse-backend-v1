package com.company.GymVerse.tenantAdmin.response.finance;

import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Expense category share row within FinanceOverviewResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryBreakdownResponse {

    private ExpenseCategory category;
    private BigDecimal amount;
    private BigDecimal percentageOfExpense;
}
