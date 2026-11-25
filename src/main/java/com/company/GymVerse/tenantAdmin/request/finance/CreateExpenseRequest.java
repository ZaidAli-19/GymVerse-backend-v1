package com.company.GymVerse.tenantAdmin.request.finance;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for logging a new gym operational expense via CREATE_EXPENSE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateExpenseRequest {

    private String title;
    private String category;
    private String customCategory;
    private BigDecimal amount;

    @JsonAlias({"description", "notes", "note"})
    private String description;

    private LocalDate expenseDate;
}
