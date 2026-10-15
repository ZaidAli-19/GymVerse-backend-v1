package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.request.SaasPlanFilterRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-3: SaaS Subscription Plans Catalog Retrieval Integration Tests")
class GetAllSaasPlansIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private PlatformAdminFactory platformAdminFactory;

    private static final String PLAN_PREFIX = "PA3_TEST_";

    private SaasSubscriptionPlan planA;
    private SaasSubscriptionPlan planB;
    private SaasSubscriptionPlan planC;
    private SaasSubscriptionPlan planD;

    @BeforeEach
    void setUp() {
        cleanTestData();

        // Plan A: Monthly Pro, Active, 49.99
        planA = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Monthly Pro")
                .description("Professional monthly tier")
                .price(new BigDecimal("49.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(100)
                .maxTrainers(5)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(false)
                .active(true)
                .build());

        // Plan B: Annual Pro, Active, 499.99
        planB = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Annual Pro")
                .description("Professional annual tier")
                .price(new BigDecimal("499.99"))
                .billingCycle(BillingCycle.ANNUAL)
                .maxMembers(200)
                .maxTrainers(10)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(true)
                .active(true)
                .build());

        // Plan C: Monthly Basic, Inactive, 29.99
        planC = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Monthly Basic")
                .description("Basic legacy tier")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .analyticsEnabled(false)
                .multiTrainerEnabled(false)
                .customBrandingEnabled(false)
                .active(false)
                .build());

        // Plan D: Quarterly Starter, Active, 89.99
        planD = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Quarterly Starter")
                .description("Quarterly starter plan")
                .price(new BigDecimal("89.99"))
                .billingCycle(BillingCycle.QUARTERLY)
                .maxMembers(75)
                .maxTrainers(3)
                .analyticsEnabled(false)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(false)
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        List<SaasSubscriptionPlan> testPlans = planRepository.findAll().stream()
                .filter(p -> p.getName() != null && p.getName().startsWith(PLAN_PREFIX))
                .toList();
        planRepository.deleteAll(testPlans);
    }

    @Test
    @DisplayName("AC-PA3.1: Dynamic filtering by active status and billing cycle")
    void testDynamicFilterByActiveAndBillingCycle() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name(PLAN_PREFIX)
                .active(true)
                .billingCycle(BillingCycle.MONTHLY)
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Monthly Pro")))
                .andExpect(jsonPath("$.plans[0].active", is(true)))
                .andExpect(jsonPath("$.plans[0].billingCycle", is("MONTHLY")));
    }

    @Test
    @DisplayName("AC-PA3.2: Partial name search and price range filtering")
    void testPartialNameSearchAndPriceRangeFilter() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name("pro")
                .minPrice(new BigDecimal("40.00"))
                .maxPrice(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Monthly Pro")))
                .andExpect(jsonPath("$.plans[0].price", is(49.99)));
    }

    @Test
    @DisplayName("AC-PA3.3: Whitelist-guarded sorting by price descending")
    void testWhitelistGuardedSortingPriceDesc() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name(PLAN_PREFIX)
                .sortBy("price")
                .sortDirection("desc")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", hasSize(4)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Annual Pro")))
                .andExpect(jsonPath("$.plans[1].name", is(PLAN_PREFIX + "Quarterly Starter")))
                .andExpect(jsonPath("$.plans[2].name", is(PLAN_PREFIX + "Monthly Pro")))
                .andExpect(jsonPath("$.plans[3].name", is(PLAN_PREFIX + "Monthly Basic")));
    }

    @Test
    @DisplayName("AC-PA3.3: Invalid sort property rejected with HTTP 400")
    void testInvalidSortFieldRejected() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .sortBy("secretField")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field: 'secretField'")));
    }

    @Test
    @DisplayName("AC-PA3.4: Validated pagination boundaries")
    void testPaginationBoundaries() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name(PLAN_PREFIX)
                .sortBy("name")
                .sortDirection("asc")
                .page(0)
                .size(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalRecords", is(4)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.plans", hasSize(2)));
    }

    @Test
    @DisplayName("AC-PA3.4: Invalid pagination values rejected with HTTP 400")
    void testInvalidPaginationRejected() throws Exception {
        // Negative page index
        PlatformAdminRequest negPage = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .page(-1)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negPage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")));

        // Zero page size
        PlatformAdminRequest zeroSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .size(0)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));

        // Size > 100
        PlatformAdminRequest excessSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .size(101)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));
    }

    @Test
    @DisplayName("AC-PA3.4: minPrice greater than maxPrice rejected with HTTP 400")
    void testMinPriceGreaterThanMaxPriceRejected() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .minPrice(new BigDecimal("100.00"))
                .maxPrice(new BigDecimal("50.00"))
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("minPrice cannot be greater than maxPrice")));
    }

    @Test
    @DisplayName("AC-PA3.5: Empty results structure when no plans match filters")
    void testEmptyResultsStructure() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name("NON_EXISTENT_NAME_XYZ")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", is(empty())))
                .andExpect(jsonPath("$.totalRecords", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @DisplayName("Direct Factory Resolution: PlatformAdminFactory resolves GetAllSaasPlansService")
    void testDirectFactoryResolution() {
        PlatformAdminFactoryOperation operation = platformAdminFactory.getOperation(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS);
        assertThat(operation).isNotNull();
        assertThat(operation.getRequestType()).isEqualTo(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS);

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .name(PLAN_PREFIX)
                .page(0)
                .size(5)
                .build();

        PlatformAdminResponse response = operation.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.getPlans()).hasSize(4);
        assertThat(response.getTotalRecords()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Nested planFilter object support: PlatformAdminRequest with planFilter")
    void testNestedPlanFilterSupport() throws Exception {
        SaasPlanFilterRequest filter = SaasPlanFilterRequest.builder()
                .name(PLAN_PREFIX + "Annual")
                .active(true)
                .billingCycle(BillingCycle.ANNUAL)
                .build();

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .planFilter(filter)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Annual Pro")));
    }
}
