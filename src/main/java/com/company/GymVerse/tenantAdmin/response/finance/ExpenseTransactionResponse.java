package com.company.GymVerse.tenantAdmin.response.finance;

import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Individual operational expense item returned in FETCH_EXPENSE_TRANSACTIONS and CREATE_EXPENSE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseTransactionResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private ExpenseCategory category;
    private LocalDate expenseDate;
    private Long recordedByAdminId;
    private String recordedByAdminName;
}
