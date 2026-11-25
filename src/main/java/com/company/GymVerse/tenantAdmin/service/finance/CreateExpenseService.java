package com.company.GymVerse.tenantAdmin.service.finance;

import com.company.GymVerse.models.tenant.Expense;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.repositories.tenant.ExpenseRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.finance.CreateExpenseRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.finance.ExpenseTransactionResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Orchestrates recording a new operational gym expense entry with audit trail to the acting admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateExpenseService implements TenantAdminOperation {

    private final ExpenseRepository expenseRepository;
    private final TenantAdminContextHelper contextHelper;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.CREATE_EXPENSE;
    }

    @Override
    @Transactional(transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing CREATE_EXPENSE operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        CreateExpenseRequest payload = request.getCreateExpenseRequest();
        ExpenseCategory resolvedCategory = validations.validateAndResolveCreateExpense(payload);
        TenantUser adminUser = contextHelper.resolveCurrentAdminUser();

        String resolvedDescription = resolveDescription(payload, resolvedCategory);
        LocalDate expenseDate = payload.getExpenseDate() != null ? payload.getExpenseDate() : LocalDate.now();

        Expense saved = expenseRepository.save(Expense.builder()
                .title(payload.getTitle().trim())
                .description(resolvedDescription)
                .amount(payload.getAmount().setScale(2, RoundingMode.HALF_UP))
                .category(resolvedCategory)
                .expenseDate(expenseDate)
                .recordedByAdmin(adminUser)
                .build());

        log.info("Created expense id={} category={} amount={} date={}",
                saved.getId(), saved.getCategory(), saved.getAmount(), saved.getExpenseDate());

        ExpenseTransactionResponse item = mapper.toExpenseTransaction(saved);
        return mapper.toExpenseTransactionAdminResponse("Expense recorded successfully", 201, item);
    }

    private String resolveDescription(final CreateExpenseRequest payload, final ExpenseCategory resolvedCategory) {
        String customLabel = null;
        if (payload.getCustomCategory() != null && !payload.getCustomCategory().isBlank()) {
            customLabel = payload.getCustomCategory().trim();
        } else if (resolvedCategory == ExpenseCategory.OTHER
                && payload.getCategory() != null
                && !"OTHER".equalsIgnoreCase(payload.getCategory().trim())) {
            customLabel = payload.getCategory().trim();
        }

        String rawNotes = payload.getDescription() != null && !payload.getDescription().isBlank()
                ? payload.getDescription().trim()
                : null;

        if (customLabel == null) {
            return rawNotes;
        }
        return rawNotes != null ? "[" + customLabel + "] " + rawNotes : "[" + customLabel + "]";
    }
}
