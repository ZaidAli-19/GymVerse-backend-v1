package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.2: Plans JPA Specification & V2 MySQL Integration Test")
class PlansSpecificationIntegrationTest {

    private static final String TENANT_DB = "gym_db_plans_test_001";

    @Autowired
    private TenantDatabaseService tenantDatabaseService;

    @Autowired
    private TenantFlywayService tenantFlywayService;

    @Autowired
    private TenantDataSourceCache tenantDataSourceCache;

    @Autowired
    private TenantRoutingBridge routingBridge;

    @Autowired
    private CreatePlanService createPlanService;

    @Autowired
    private FetchPlansService fetchPlansService;

    @Autowired
    private FetchPlansSummaryService fetchPlansSummaryService;

    @Autowired
    private FetchPlanFilterOptionsService fetchPlanFilterOptionsService;

    @BeforeEach
    void setUp() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
    }

    @Test
    @DisplayName("Executes CREATE_PLAN, FETCH_PLANS JPA Specification filters, summary, and filter options against MySQL")
    void endToEndPlansFlowAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            createPlanService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                    .createPlanRequest(CreatePlanRequest.builder()
                            .name("Elite Summer Offer")
                            .description("Full access")
                            .tier("ELITE")
                            .facilities(List.of("Weightlifting", "Cardio"))
                            .basePrice(new BigDecimal("100.00"))
                            .discountType(DiscountType.PERCENTAGE)
                            .discountValue(new BigDecimal("15"))
                            .durationDays(30)
                            .build())
                    .build());

            createPlanService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                    .createPlanRequest(CreatePlanRequest.builder()
                            .name("Spartan Trial Pass")
                            .tier("SPARTAN")
                            .customTheme(PlanTheme.OCEAN_BLUE)
                            .facilities(List.of("CrossFit", "Cardio"))
                            .basePrice(new BigDecimal("20.00"))
                            .durationDays(7)
                            .isTrial(true)
                            .build())
                    .build());

            // 1. Filter by tier=ELITE + hasOffer=true
            TenantAdminResponse offerRes = fetchPlansService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLANS)
                    .fetchPlansRequest(FetchPlansRequest.builder()
                            .tier("ELITE")
                            .hasOffer(true)
                            .sortBy("tier")
                            .build())
                    .build());
            assertThat(offerRes.getPlansPageResponse().totalElements()).isEqualTo(1L);
            assertThat(offerRes.getPlansPageResponse().content().get(0).getThemeKey()).isEqualTo(PlanTheme.GOLDEN);

            // 2. Search by facility "CrossFit"
            TenantAdminResponse searchRes = fetchPlansService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLANS)
                    .fetchPlansRequest(FetchPlansRequest.builder().search("crossfit").build())
                    .build());
            assertThat(searchRes.getPlansPageResponse().totalElements()).isEqualTo(1L);
            assertThat(searchRes.getPlansPageResponse().content().get(0).getName()).isEqualTo("Spartan Trial Pass");

            // 3. Summary & Filter Options
            TenantAdminResponse summaryRes = fetchPlansSummaryService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLANS_SUMMARY)
                    .build());
            assertThat(summaryRes.getPlansSummaryResponse().getTotalActivePlans()).isEqualTo(2L);

            TenantAdminResponse optionsRes = fetchPlanFilterOptionsService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLAN_FILTER_OPTIONS)
                    .build());
            assertThat(optionsRes.getPlanFilterOptionsResponse().getTiers())
                    .extracting("name")
                    .containsExactlyInAnyOrder("ELITE", "LEGENDARY", "SPARTAN");
            assertThat(optionsRes.getPlanFilterOptionsResponse().getFacilities())
                    .containsExactly("Cardio", "CrossFit", "Weightlifting");
        });
    }
}
