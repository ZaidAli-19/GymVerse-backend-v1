package com.company.GymVerse.tenantAdmin.service.finance;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Expense;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.ExpenseRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.finance.CreateExpenseRequest;
import com.company.GymVerse.tenantAdmin.request.finance.FetchExpenseTransactionsRequest;
import com.company.GymVerse.tenantAdmin.request.finance.FetchFinanceOverviewRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.finance.ExpenseTransactionResponse;
import com.company.GymVerse.tenantAdmin.response.finance.FinanceOverviewResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.6: Finance Analytics Suite & Expense Ledger Unit Tests")
class FinanceServiceTest {

    private Map<Long, TenantUser> usersById;
    private Map<Long, Expense> expensesById;
    private List<Revenue> revenues;

    private FetchFinanceOverviewService fetchFinanceOverviewService;
    private FetchExpenseTransactionsService fetchExpenseTransactionsService;
    private CreateExpenseService createExpenseService;

    @BeforeEach
    void setUp() {
        usersById = new LinkedHashMap<>();
        expensesById = new LinkedHashMap<>();
        revenues = new ArrayList<>();
        AtomicLong expenseSeq = new AtomicLong(1);

        TenantUser adminUser = TenantUser.builder()
                .id(1L).centralUserId(9001L).firstName("Admin").lastName("Owner")
                .email("admin@gym.com").role(TenantRole.TENANT_ADMIN).active(true).build();
        usersById.put(1L, adminUser);

        // Anchor date: Wednesday 2026-11-25
        // Week: 2026-11-23 .. 2026-11-29 | Month: 2026-11-01 .. 2026-11-30 | Year: 2026-01-01 .. 2026-12-31
        revenues.add(Revenue.builder()
                .amount(new BigDecimal("200.00"))
                .paymentMode(PaymentMode.CASH)
                .revenueType(RevenueType.NEW_MEMBERSHIP)
                .receivedAt(LocalDate.of(2026, 11, 24).atTime(10, 0).toInstant(ZoneOffset.UTC))
                .build());
        revenues.add(Revenue.builder()
                .amount(new BigDecimal("300.00"))
                .paymentMode(PaymentMode.ONLINE)
                .revenueType(RevenueType.RENEWAL)
                .receivedAt(LocalDate.of(2026, 11, 25).atTime(14, 0).toInstant(ZoneOffset.UTC))
                .build());
        revenues.add(Revenue.builder()
                .amount(new BigDecimal("500.00"))
                .paymentMode(PaymentMode.ONLINE)
                .revenueType(RevenueType.OTHER)
                .receivedAt(LocalDate.of(2026, 11, 5).atTime(9, 0).toInstant(ZoneOffset.UTC))
                .build());
        revenues.add(Revenue.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMode(PaymentMode.CASH)
                .revenueType(RevenueType.RENEWAL)
                .receivedAt(LocalDate.of(2026, 5, 10).atTime(12, 0).toInstant(ZoneOffset.UTC))
                .build());

        expensesById.put(expenseSeq.get(), Expense.builder()
                .id(expenseSeq.getAndIncrement())
                .title("Power Bill")
                .category(ExpenseCategory.ELECTRICITY)
                .amount(new BigDecimal("100.00"))
                .expenseDate(LocalDate.of(2026, 11, 24))
                .recordedByAdmin(adminUser)
                .build());
        expensesById.put(expenseSeq.get(), Expense.builder()
                .id(expenseSeq.getAndIncrement())
                .title("November Rent")
                .category(ExpenseCategory.RENT)
                .amount(new BigDecimal("300.00"))
                .expenseDate(LocalDate.of(2026, 11, 1))
                .recordedByAdmin(adminUser)
                .build());
        expensesById.put(expenseSeq.get(), Expense.builder()
                .id(expenseSeq.getAndIncrement())
                .title("Summer Equipment Repair")
                .category(ExpenseCategory.MACHINE_REPAIR)
                .amount(new BigDecimal("400.00"))
                .expenseDate(LocalDate.of(2026, 5, 15))
                .recordedByAdmin(adminUser)
                .build());

        RevenueRepository revenueRepo = (RevenueRepository) Proxy.newProxyInstance(
                RevenueRepository.class.getClassLoader(),
                new Class<?>[]{RevenueRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "sumAmountGroupedByPaymentModeBetween" -> {
                        Instant start = (Instant) args[0];
                        Instant end = (Instant) args[1];
                        Map<PaymentMode, BigDecimal> sums = new EnumMap<>(PaymentMode.class);
                        for (Revenue r : revenues) {
                            if (!r.getReceivedAt().isBefore(start) && !r.getReceivedAt().isAfter(end)) {
                                sums.merge(r.getPaymentMode(), r.getAmount(), BigDecimal::add);
                            }
                        }
                        yield sums.entrySet().stream()
                                .map(e -> new Object[]{e.getKey(), e.getValue()})
                                .toList();
                    }
                    case "sumAmountGroupedByRevenueTypeBetween" -> {
                        Instant start = (Instant) args[0];
                        Instant end = (Instant) args[1];
                        Map<RevenueType, BigDecimal> sums = new EnumMap<>(RevenueType.class);
                        for (Revenue r : revenues) {
                            if (!r.getReceivedAt().isBefore(start) && !r.getReceivedAt().isAfter(end)) {
                                sums.merge(r.getRevenueType(), r.getAmount(), BigDecimal::add);
                            }
                        }
                        yield sums.entrySet().stream()
                                .map(e -> new Object[]{e.getKey(), e.getValue()})
                                .toList();
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        ExpenseRepository expenseRepo = (ExpenseRepository) Proxy.newProxyInstance(
                ExpenseRepository.class.getClassLoader(),
                new Class<?>[]{ExpenseRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "sumAmountGroupedByCategoryBetween" -> {
                        LocalDate start = (LocalDate) args[0];
                        LocalDate end = (LocalDate) args[1];
                        Map<ExpenseCategory, BigDecimal> sums = new EnumMap<>(ExpenseCategory.class);
                        for (Expense e : expensesById.values()) {
                            if (!e.getExpenseDate().isBefore(start) && !e.getExpenseDate().isAfter(end)) {
                                sums.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
                            }
                        }
                        yield sums.entrySet().stream()
                                .map(e -> new Object[]{e.getKey(), e.getValue()})
                                .toList();
                    }
                    case "findAll" -> {
                        if (args != null && args.length == 2 && args[0] instanceof Specification<?> && args[1] instanceof Pageable pageable) {
                            List<Expense> all = new ArrayList<>(expensesById.values());
                            yield new PageImpl<>(all, pageable, all.size());
                        }
                        yield new ArrayList<>(expensesById.values());
                    }
                    case "save" -> {
                        Expense exp = (Expense) args[0];
                        if (exp.getId() == null) {
                            exp.setId(expenseSeq.getAndIncrement());
                        }
                        expensesById.put(exp.getId(), exp);
                        yield exp;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        TenantUserRepository tenantUserRepo = (TenantUserRepository) Proxy.newProxyInstance(
                TenantUserRepository.class.getClassLoader(),
                new Class<?>[]{TenantUserRepository.class},
                (p, m, args) -> "findByCentralUserId".equals(m.getName())
                        ? usersById.values().stream().filter(u -> u.getCentralUserId().equals(args[0])).findFirst()
                        : Optional.empty()
        );

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (p, m, args) -> false
        );
        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (p, m, args) -> false
        );

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L);
        jwtUtil.init();

        String token = jwtUtil.generateTenantAccessToken(TenantTokenClaims.builder()
                .userId(9001L).email("admin@gym.com").tenantId(10L).gymCode("IRON-01")
                .tenantDbName("gym_db_10").role(TenantRole.TENANT_ADMIN).adminAuthority(AdminAuthority.PRIMARY)
                .build());
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.addHeader("Authorization", "Bearer " + token);

        TenantAdminContextHelper contextHelper = new TenantAdminContextHelper(jwtUtil, httpReq, tenantUserRepo);
        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();

        fetchFinanceOverviewService = new FetchFinanceOverviewService(revenueRepo, expenseRepo, validations, mapper);
        fetchExpenseTransactionsService = new FetchExpenseTransactionsService(expenseRepo, validations, mapper);
        createExpenseService = new CreateExpenseService(expenseRepo, contextHelper, validations, mapper);

        TenantContext.setCurrentTenant("gym_db_10");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FETCH_FINANCE_OVERVIEW aggregates WEEKLY, MONTHLY, YEARLY metrics and guards zero-revenue divide")
    void fetchFinanceOverview_PeriodWindowsAndZeroRevenueGuard() {
        LocalDate ref = LocalDate.of(2026, 11, 25);

        // 1. WEEKLY: revenue=500 (200 cash NEW + 300 online RENEWAL), expense=100 -> net=400, budgetUsed=20.00%
        FinanceOverviewResponse weekly = fetchFinanceOverviewService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_FINANCE_OVERVIEW)
                .fetchFinanceOverviewRequest(FetchFinanceOverviewRequest.builder()
                        .periodType(FinancePeriodType.WEEKLY).referenceDate(ref).build())
                .build()).getFinanceOverviewResponse();

        assertThat(weekly.getTotalRevenue()).isEqualByComparingTo("500.00");
        assertThat(weekly.getTotalExpense()).isEqualByComparingTo("100.00");
        assertThat(weekly.getNetProfit()).isEqualByComparingTo("400.00");
        assertThat(weekly.getBudgetUsedPercentage()).isEqualByComparingTo("20.00");
        assertThat(weekly.getCollectionBreakdown().getCashCollected()).isEqualByComparingTo("200.00");
        assertThat(weekly.getCollectionBreakdown().getOnlineCollected()).isEqualByComparingTo("300.00");
        assertThat(weekly.getRevenueBreakdown().getNewMembershipRevenue()).isEqualByComparingTo("200.00");
        assertThat(weekly.getRevenueBreakdown().getRenewalRevenue()).isEqualByComparingTo("300.00");
        assertThat(weekly.getRevenueBreakdown().getOtherRevenue()).isEqualByComparingTo("0.00");
        assertThat(weekly.getExpenseBreakdown()).hasSize(1);
        assertThat(weekly.getExpenseBreakdown().get(0).getPercentageOfExpense()).isEqualByComparingTo("100.00");

        // 2. MONTHLY: revenue=1000, expense=400 (300 RENT 75% + 100 ELECTRICITY 25%) -> net=600, budgetUsed=40.00%
        FinanceOverviewResponse monthly = fetchFinanceOverviewService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_FINANCE_OVERVIEW)
                .fetchFinanceOverviewRequest(FetchFinanceOverviewRequest.builder()
                        .periodType(FinancePeriodType.MONTHLY).referenceDate(ref).build())
                .build()).getFinanceOverviewResponse();

        assertThat(monthly.getTotalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(monthly.getTotalExpense()).isEqualByComparingTo("400.00");
        assertThat(monthly.getNetProfit()).isEqualByComparingTo("600.00");
        assertThat(monthly.getBudgetUsedPercentage()).isEqualByComparingTo("40.00");
        assertThat(monthly.getExpenseBreakdown()).hasSize(2);
        assertThat(monthly.getExpenseBreakdown().get(0).getCategory()).isEqualTo(ExpenseCategory.RENT);
        assertThat(monthly.getExpenseBreakdown().get(0).getPercentageOfExpense()).isEqualByComparingTo("75.00");
        assertThat(monthly.getExpenseBreakdown().get(1).getCategory()).isEqualTo(ExpenseCategory.ELECTRICITY);
        assertThat(monthly.getExpenseBreakdown().get(1).getPercentageOfExpense()).isEqualByComparingTo("25.00");

        // 3. YEARLY: revenue=2000, expense=800 -> net=1200, budgetUsed=40.00%
        FinanceOverviewResponse yearly = fetchFinanceOverviewService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_FINANCE_OVERVIEW)
                .fetchFinanceOverviewRequest(FetchFinanceOverviewRequest.builder()
                        .periodType(FinancePeriodType.YEARLY).referenceDate(ref).build())
                .build()).getFinanceOverviewResponse();
        assertThat(yearly.getTotalRevenue()).isEqualByComparingTo("2000.00");
        assertThat(yearly.getTotalExpense()).isEqualByComparingTo("800.00");
        assertThat(yearly.getNetProfit()).isEqualByComparingTo("1200.00");

        // 4. Zero-revenue divide guard (future year with 0 revenue & 0 expense -> budgetUsed=0.00)
        FinanceOverviewResponse zeroRev = fetchFinanceOverviewService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_FINANCE_OVERVIEW)
                .fetchFinanceOverviewRequest(FetchFinanceOverviewRequest.builder()
                        .periodType(FinancePeriodType.MONTHLY)
                        .referenceDate(LocalDate.of(2027, 1, 15))
                        .build())
                .build()).getFinanceOverviewResponse();
        assertThat(zeroRev.getTotalRevenue()).isEqualByComparingTo("0.00");
        assertThat(zeroRev.getBudgetUsedPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CREATE_EXPENSE persists default/custom OTHER categories with notes and returns 201 Created")
    void createExpense_SuccessAndGuards() {
        TenantAdminResponse createdStandard = createExpenseService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_EXPENSE)
                .createExpenseRequest(CreateExpenseRequest.builder()
                        .title("Staff Bonus")
                        .category("STAFF_SALARY")
                        .amount(new BigDecimal("250.50"))
                        .description("Holiday shift bonus")
                        .expenseDate(LocalDate.of(2026, 11, 25))
                        .build())
                .build());

        assertThat(createdStandard.getStatus()).isEqualTo(201);
        ExpenseTransactionResponse item = createdStandard.getExpenseTransactionResponse();
        assertThat(item.getCategory()).isEqualTo(ExpenseCategory.STAFF_SALARY);
        assertThat(item.getAmount()).isEqualByComparingTo("250.50");
        assertThat(item.getRecordedByAdminId()).isEqualTo(1L);
        assertThat(item.getRecordedByAdminName()).isEqualTo("Admin Owner");

        TenantAdminResponse createdCustom = createExpenseService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_EXPENSE)
                .createExpenseRequest(CreateExpenseRequest.builder()
                        .title("Cleaning Supplies")
                        .category("OTHER")
                        .customCategory("Sanitation")
                        .amount(new BigDecimal("45.00"))
                        .description("Disinfectant wipes")
                        .build())
                .build());
        assertThat(createdCustom.getStatus()).isEqualTo(201);
        assertThat(createdCustom.getExpenseTransactionResponse().getCategory()).isEqualTo(ExpenseCategory.OTHER);
        assertThat(createdCustom.getExpenseTransactionResponse().getDescription())
                .isEqualTo("[Sanitation] Disinfectant wipes");

        assertThatThrownBy(() -> createExpenseService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_EXPENSE)
                .createExpenseRequest(CreateExpenseRequest.builder()
                        .title("Bad Amount").category("RENT").amount(BigDecimal.ZERO).build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("FETCH_EXPENSE_TRANSACTIONS validates category filter, date bounds, and whitelisted sort fields")
    void fetchExpenseTransactions_ValidationAndPaging() {
        TenantAdminResponse paged = fetchExpenseTransactionsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS)
                .fetchExpenseTransactionsRequest(FetchExpenseTransactionsRequest.builder()
                        .periodType(FinancePeriodType.MONTHLY)
                        .category("ALL")
                        .sortBy("amount")
                        .sortDirection("desc")
                        .build())
                .build());

        assertThat(paged.getExpenseTransactionsPageResponse().totalElements()).isEqualTo(3L);

        assertThatThrownBy(() -> fetchExpenseTransactionsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS)
                .fetchExpenseTransactionsRequest(FetchExpenseTransactionsRequest.builder()
                        .category("NOT_A_CATEGORY").build())
                .build()))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> fetchExpenseTransactionsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS)
                .fetchExpenseTransactionsRequest(FetchExpenseTransactionsRequest.builder()
                        .startDate(LocalDate.of(2026, 11, 20))
                        .endDate(LocalDate.of(2026, 11, 10))
                        .build())
                .build()))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> fetchExpenseTransactionsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS)
                .fetchExpenseTransactionsRequest(FetchExpenseTransactionsRequest.builder()
                        .sortBy("recordedByAdmin").build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }
}
