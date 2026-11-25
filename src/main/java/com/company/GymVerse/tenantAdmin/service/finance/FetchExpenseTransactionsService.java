package com.company.GymVerse.tenantAdmin.service.finance;

import com.company.GymVerse.models.tenant.Expense;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.repositories.tenant.ExpenseRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.FinancePeriodHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.finance.FetchExpenseTransactionsRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.specifications.finance.ExpenseSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Orchestrates paginated, sorted, and JPA Specification-filtered expense transaction ledger queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchExpenseTransactionsService implements TenantAdminOperation {

    private final ExpenseRepository expenseRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_EXPENSE_TRANSACTIONS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchExpenseTransactionsRequest filter = request.getFetchExpenseTransactionsRequest();
        Pageable pageable = validations.validateFetchExpenseTransactions(filter);
        ExpenseCategory categoryFilter = validations.parseExpenseCategoryFilter(filter != null ? filter.getCategory() : null);

        LocalDate startDate = filter != null ? filter.getStartDate() : null;
        LocalDate endDate = filter != null ? filter.getEndDate() : null;
        if (filter != null && filter.getPeriodType() != null && startDate == null && endDate == null) {
            FinancePeriodHelper.PeriodWindow window = FinancePeriodHelper.resolveWindow(filter.getPeriodType(), null);
            startDate = window.startDate();
            endDate = window.endDate();
        }

        Specification<Expense> spec = ExpenseSpecification.withFilters(
                startDate,
                endDate,
                categoryFilter,
                filter != null ? filter.getSearch() : null
        );

        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        return mapper.toPagedExpensesResponse("Expense transactions retrieved successfully", page);
    }
}
