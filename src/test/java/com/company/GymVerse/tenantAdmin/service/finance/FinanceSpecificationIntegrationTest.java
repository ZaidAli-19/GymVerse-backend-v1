package com.company.GymVerse.tenantAdmin.service.finance;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.finance.CreateExpenseRequest;
import com.company.GymVerse.tenantAdmin.request.finance.FetchExpenseTransactionsRequest;
import com.company.GymVerse.tenantAdmin.request.finance.FetchFinanceOverviewRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.finance.FinanceOverviewResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.6: Finance Overview, Expense Ledger & V2 MySQL Integration Test")
class FinanceSpecificationIntegrationTest {

    private static final String TENANT_DB = "gym_db_finance_test_001";

    @Autowired
    private TenantDatabaseService tenantDatabaseService;

    @Autowired
    private TenantFlywayService tenantFlywayService;

    @Autowired
    private TenantDataSourceCache tenantDataSourceCache;

    @Autowired
    private TenantRoutingBridge routingBridge;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private RevenueRepository revenueRepository;

    @Autowired
    private CreateExpenseService createExpenseService;

    @Autowired
    private FetchExpenseTransactionsService fetchExpenseTransactionsService;

    @Autowired
    private FetchFinanceOverviewService fetchFinanceOverviewService;

    @BeforeEach
    void setUp() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
    }

    @Test
    @DisplayName("Executes CREATE_EXPENSE, ExpenseSpecification ledger queries, and FETCH_FINANCE_OVERVIEW SQL aggregations against MySQL")
    void endToEndFinanceFlowAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            TenantUser admin = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(9001L).firstName("Admin").lastName("Owner")
                    .email("admin@gym.com").role(TenantRole.TENANT_ADMIN).active(true).build());

            // Seed nullable member_membership_id OTHER revenue + CASH/ONLINE entries
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(null)
                    .amount(new BigDecimal("400.00"))
                    .paymentMode(PaymentMode.CASH)
                    .revenueType(RevenueType.OTHER)
                    .notes("Protein bar & merchandise")
                    .receivedAt(LocalDate.of(2026, 11, 24).atTime(11, 0).toInstant(ZoneOffset.UTC))
                    .recordedByAdmin(admin)
                    .build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(null)
                    .amount(new BigDecimal("600.00"))
                    .paymentMode(PaymentMode.ONLINE)
                    .revenueType(RevenueType.RENEWAL)
                    .notes("Renewal intake")
                    .receivedAt(LocalDate.of(2026, 11, 10).atTime(15, 0).toInstant(ZoneOffset.UTC))
                    .recordedByAdmin(admin)
                    .build());

            // 1. Create expenses via CREATE_EXPENSE (returns 201 Created)
            TenantAdminResponse exp1 = createExpenseService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.CREATE_EXPENSE)
                    .createExpenseRequest(CreateExpenseRequest.builder()
                            .title("November Rent")
                            .category("RENT")
                            .amount(new BigDecimal("300.00"))
                            .description("Main floor lease")
                            .expenseDate(LocalDate.of(2026, 11, 2))
                            .build())
                    .build());
            assertThat(exp1.getStatus()).isEqualTo(201);

            TenantAdminResponse exp2 = createExpenseService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.CREATE_EXPENSE)
                    .createExpenseRequest(CreateExpenseRequest.builder()
                            .title("Electric Meter")
                            .category("ELECTRICITY")
                            .amount(new BigDecimal("100.00"))
                            .description("Peak AC usage")
                            .expenseDate(LocalDate.of(2026, 11, 24))
                            .build())
                    .build());
            assertThat(exp2.getStatus()).isEqualTo(201);

            // 2. Query ledger via ExpenseSpecification
            TenantAdminResponse filteredLedger = fetchExpenseTransactionsService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_EXPENSE_TRANSACTIONS)
                    .fetchExpenseTransactionsRequest(FetchExpenseTransactionsRequest.builder()
                            .startDate(LocalDate.of(2026, 11, 1))
                            .endDate(LocalDate.of(2026, 11, 30))
                            .category("ELECTRICITY")
                            .search("ac usage")
                            .sortBy("amount")
                            .sortDirection("desc")
                            .build())
                    .build());
            assertThat(filteredLedger.getExpenseTransactionsPageResponse().totalElements()).isEqualTo(1L);
            assertThat(filteredLedger.getExpenseTransactionsPageResponse().content().get(0).getTitle())
                    .isEqualTo("Electric Meter");

            // 3. Monthly Finance Overview aggregation in MySQL
            TenantAdminResponse monthlyRes = fetchFinanceOverviewService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_FINANCE_OVERVIEW)
                    .fetchFinanceOverviewRequest(FetchFinanceOverviewRequest.builder()
                            .periodType(FinancePeriodType.MONTHLY)
                            .referenceDate(LocalDate.of(2026, 11, 25))
                            .build())
                    .build());
            FinanceOverviewResponse monthly = monthlyRes.getFinanceOverviewResponse();
            assertThat(monthly.getTotalRevenue()).isEqualByComparingTo("1000.00");
            assertThat(monthly.getTotalExpense()).isEqualByComparingTo("400.00");
            assertThat(monthly.getNetProfit()).isEqualByComparingTo("600.00");
            assertThat(monthly.getBudgetUsedPercentage()).isEqualByComparingTo("40.00");
            assertThat(monthly.getCollectionBreakdown().getCashCollected()).isEqualByComparingTo("400.00");
            assertThat(monthly.getCollectionBreakdown().getOnlineCollected()).isEqualByComparingTo("600.00");
            assertThat(monthly.getRevenueBreakdown().getRenewalRevenue()).isEqualByComparingTo("600.00");
            assertThat(monthly.getRevenueBreakdown().getOtherRevenue()).isEqualByComparingTo("400.00");
            assertThat(monthly.getExpenseBreakdown()).hasSize(2);
        });
    }
}
