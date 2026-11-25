package com.company.GymVerse.tenantAdmin.request.finance;

import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Paginated, sorted, and filtered query request for FETCH_EXPENSE_TRANSACTIONS operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchExpenseTransactionsRequest {

    /**
     * Optional period toggle (WEEKLY, MONTHLY, YEARLY) used when explicit startDate/endDate are omitted.
     */
    private FinancePeriodType periodType;

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Optional category filter: ALL or an ExpenseCategory constant.
     */
    private String category;

    /**
     * Optional case-insensitive search across expense title and description.
     */
    private String search;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
