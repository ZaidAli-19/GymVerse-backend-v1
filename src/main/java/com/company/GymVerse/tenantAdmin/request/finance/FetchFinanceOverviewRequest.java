package com.company.GymVerse.tenantAdmin.request.finance;

import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for FETCH_FINANCE_OVERVIEW operations with WEEKLY, MONTHLY, or YEARLY period toggles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchFinanceOverviewRequest {

    /**
     * Calendar aggregation period (defaults to MONTHLY when omitted).
     */
    private FinancePeriodType periodType;

    /**
     * Optional anchor date for resolving the calendar window (defaults to today in UTC).
     */
    private LocalDate referenceDate;
}
