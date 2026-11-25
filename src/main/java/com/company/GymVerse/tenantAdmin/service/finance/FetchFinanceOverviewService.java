package com.company.GymVerse.tenantAdmin.service.finance;

import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.repositories.tenant.ExpenseRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.FinancePeriodHelper;
import com.company.GymVerse.tenantAdmin.helper.MembershipCalculationHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.finance.FetchFinanceOverviewRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.finance.CollectionBreakdownResponse;
import com.company.GymVerse.tenantAdmin.response.finance.ExpenseCategoryBreakdownResponse;
import com.company.GymVerse.tenantAdmin.response.finance.FinanceOverviewResponse;
import com.company.GymVerse.tenantAdmin.response.finance.RevenueSourceBreakdownResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates WEEKLY / MONTHLY / YEARLY financial analytics: revenue, expense, net profit,
 * budget-used percentage, expense category breakdown, cash/online collections, and revenue sources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchFinanceOverviewService implements TenantAdminOperation {

    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_FINANCE_OVERVIEW;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_FINANCE_OVERVIEW operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchFinanceOverviewRequest payload = request.getFetchFinanceOverviewRequest();
        FinancePeriodType periodType = (payload != null && payload.getPeriodType() != null)
                ? payload.getPeriodType()
                : FinancePeriodType.MONTHLY;
        FinancePeriodHelper.PeriodWindow window = FinancePeriodHelper.resolveWindow(
                periodType,
                payload != null ? payload.getReferenceDate() : null
        );

        Map<PaymentMode, BigDecimal> paymentSums = aggregateByEnum(
                revenueRepository.sumAmountGroupedByPaymentModeBetween(window.startInstant(), window.endInstant()),
                PaymentMode.class
        );
        Map<RevenueType, BigDecimal> revenueTypeSums = aggregateByEnum(
                revenueRepository.sumAmountGroupedByRevenueTypeBetween(window.startInstant(), window.endInstant()),
                RevenueType.class
        );
        Map<ExpenseCategory, BigDecimal> expenseCategorySums = aggregateByEnum(
                expenseRepository.sumAmountGroupedByCategoryBetween(window.startDate(), window.endDate()),
                ExpenseCategory.class
        );

        BigDecimal cashCollected = scale(paymentSums.getOrDefault(PaymentMode.CASH, BigDecimal.ZERO));
        BigDecimal onlineCollected = scale(paymentSums.getOrDefault(PaymentMode.ONLINE, BigDecimal.ZERO));
        BigDecimal totalRevenue = scale(cashCollected.add(onlineCollected));

        BigDecimal newMembershipRevenue = scale(revenueTypeSums.getOrDefault(RevenueType.NEW_MEMBERSHIP, BigDecimal.ZERO));
        BigDecimal renewalRevenue = scale(revenueTypeSums.getOrDefault(RevenueType.RENEWAL, BigDecimal.ZERO));
        BigDecimal otherRevenue = scale(revenueTypeSums.getOrDefault(RevenueType.OTHER, BigDecimal.ZERO));
        BigDecimal revenueBySourceTotal = scale(newMembershipRevenue.add(renewalRevenue).add(otherRevenue));

        BigDecimal totalExpense = scale(expenseCategorySums.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal netProfit = scale(totalRevenue.subtract(totalExpense));
        BigDecimal budgetUsedPercentage = MembershipCalculationHelper.safePercentage(totalExpense, totalRevenue);

        List<ExpenseCategoryBreakdownResponse> expenseBreakdown = expenseCategorySums.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.<Map.Entry<ExpenseCategory, BigDecimal>, BigDecimal>comparing(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(e -> e.getKey().name()))
                .map(entry -> {
                    BigDecimal amount = scale(entry.getValue());
                    return ExpenseCategoryBreakdownResponse.builder()
                            .category(entry.getKey())
                            .amount(amount)
                            .percentageOfExpense(MembershipCalculationHelper.safePercentage(amount, totalExpense))
                            .build();
                })
                .toList();

        FinanceOverviewResponse overview = FinanceOverviewResponse.builder()
                .periodType(periodType)
                .startDate(window.startDate())
                .endDate(window.endDate())
                .totalRevenue(totalRevenue)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .budgetUsedPercentage(budgetUsedPercentage)
                .expenseBreakdown(expenseBreakdown)
                .collectionBreakdown(CollectionBreakdownResponse.builder()
                        .cashCollected(cashCollected)
                        .onlineCollected(onlineCollected)
                        .totalCollected(totalRevenue)
                        .build())
                .revenueBreakdown(RevenueSourceBreakdownResponse.builder()
                        .newMembershipRevenue(newMembershipRevenue)
                        .renewalRevenue(renewalRevenue)
                        .otherRevenue(otherRevenue)
                        .totalRevenue(revenueBySourceTotal)
                        .build())
                .build();

        return mapper.toFinanceOverviewAdminResponse("Finance overview retrieved successfully", overview);
    }

    private <E extends Enum<E>> Map<E, BigDecimal> aggregateByEnum(
            final List<Object[]> rows,
            final Class<E> enumClass
    ) {
        Map<E, BigDecimal> map = new EnumMap<>(enumClass);
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && enumClass.isInstance(row[0])) {
                E key = enumClass.cast(row[0]);
                BigDecimal val = toBigDecimal(row[1]);
                map.merge(key, val, BigDecimal::add);
            }
        }
        return map;
    }

    private BigDecimal toBigDecimal(final Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return new BigDecimal(raw.toString());
    }

    private BigDecimal scale(final BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
