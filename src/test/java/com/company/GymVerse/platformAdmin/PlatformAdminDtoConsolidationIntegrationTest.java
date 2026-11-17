package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.platformAdmin.request.GymTenantFilterRequest;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.request.SaasPlanFilterRequest;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.repositories.central.GymTenantRepository;
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
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-7: Platform Admin Request DTO Consolidation and Pagination Standardization Integration Tests")
class PlatformAdminDtoConsolidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private GymTenantRepository tenantRepository;

    private static final String PLAN_PREFIX = "PA7_PLAN_";
    private static final String TENANT_PREFIX = "PA7_TENANT_";

    @BeforeEach
    void setUp() {
        cleanTestData();

        // Seed 3 SaaS Subscription Plans
        planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Basic Tier")
                .description("Basic tier")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .analyticsEnabled(false)
                .multiTrainerEnabled(false)
                .customBrandingEnabled(false)
                .active(true)
                .build());

        planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Pro Tier")
                .description("Pro tier")
                .price(new BigDecimal("99.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(150)
                .maxTrainers(5)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(false)
                .active(true)
                .build());

        planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Enterprise Tier")
                .description("Enterprise tier")
                .price(new BigDecimal("299.99"))
                .billingCycle(BillingCycle.ANNUAL)
                .maxMembers(500)
                .maxTrainers(20)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(true)
                .active(false)
                .build());

        // Seed 3 Gym Tenants
        Instant now = Instant.now();
        tenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Alpha Gym")
                .contactEmail("alpha@test.com")
                .phoneNumber("+1-512-555-0191")
                .address("100 Alpha Blvd")
                .city("Dallas")
                .gymCode("PA7ALPH")
                .dbName("gym_db_pa7_1")
                .status(GymStatus.ACTIVE)
                .registeredAt(now.minusSeconds(300))
                .build());

        tenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Beta Fitness")
                .contactEmail("beta@test.com")
                .phoneNumber("+1-512-555-0192")
                .address("200 Beta St")
                .city("Austin")
                .gymCode("PA7BETA")
                .dbName("gym_db_pa7_2")
                .status(GymStatus.ACTIVE)
                .registeredAt(now.minusSeconds(200))
                .build());

        tenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Gamma Performance")
                .contactEmail("gamma@test.com")
                .phoneNumber("+1-512-555-0193")
                .address("300 Gamma Rd")
                .city("Austin")
                .gymCode("PA7GAMM")
                .dbName("gym_db_pa7_3")
                .status(GymStatus.INACTIVE)
                .registeredAt(now.minusSeconds(100))
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

        List<GymTenant> testTenants = tenantRepository.findAll().stream()
                .filter(t -> t.getGymName() != null && t.getGymName().startsWith(TENANT_PREFIX))
                .toList();
        tenantRepository.deleteAll(testTenants);
    }

    @Test
    @DisplayName("AC-PA7.1: Centralized root pagination and sorting applies to FETCH_ALL_SAAS_PLANS")
    void testRootPaginationAppliesToFetchAllPlans() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .planFilter(SaasPlanFilterRequest.builder()
                        .name(PLAN_PREFIX)
                        .build())
                .page(0)
                .size(2)
                .sortBy("price")
                .sortDirection("desc")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalRecords", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.plans", hasSize(2)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Enterprise Tier")))
                .andExpect(jsonPath("$.plans[1].name", is(PLAN_PREFIX + "Pro Tier")));
    }

    @Test
    @DisplayName("AC-PA7.1: Centralized root pagination and sorting applies to FETCH_ALL_GYM_TENANTS")
    void testRootPaginationAppliesToFetchAllTenants() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .tenantFilter(GymTenantFilterRequest.builder()
                        .gymName(TENANT_PREFIX)
                        .build())
                .page(0)
                .size(2)
                .sortBy("gymName")
                .sortDirection("asc")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalRecords", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.tenants", hasSize(2)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Alpha Gym")))
                .andExpect(jsonPath("$.tenants[1].gymName", is(TENANT_PREFIX + "Beta Fitness")));
    }

    @Test
    @DisplayName("AC-PA7.2: Encapsulated planFilter criteria filtering")
    void testPlanFilterCriteria() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .planFilter(SaasPlanFilterRequest.builder()
                        .name(PLAN_PREFIX)
                        .active(true)
                        .billingCycle(BillingCycle.MONTHLY)
                        .minPrice(new BigDecimal("50.00"))
                        .maxPrice(new BigDecimal("150.00"))
                        .build())
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name", is(PLAN_PREFIX + "Pro Tier")));
    }

    @Test
    @DisplayName("AC-PA7.3: Encapsulated tenantFilter criteria filtering")
    void testTenantFilterCriteria() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .tenantFilter(GymTenantFilterRequest.builder()
                        .gymName(TENANT_PREFIX)
                        .status(GymStatus.ACTIVE)
                        .city("Austin")
                        .gymCode("PA7")
                        .build())
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Beta Fitness")))
                .andExpect(jsonPath("$.tenants[0].city", is("Austin")));
    }

    @Test
    @DisplayName("AC-PA7.6: Reusable pagination boundary validation on plans")
    void testInvalidPaginationOnPlans() throws Exception {
        PlatformAdminRequest invalidPage = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .page(-1)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")));

        PlatformAdminRequest invalidSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .size(101)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));
    }

    @Test
    @DisplayName("AC-PA7.6: Reusable pagination boundary validation on tenants")
    void testInvalidPaginationOnTenants() throws Exception {
        PlatformAdminRequest invalidPage = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .page(-5)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")));

        PlatformAdminRequest invalidSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .size(0)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));
    }

    @Test
    @DisplayName("AC-PA7.6: Invalid sort property rejected with allowed fields list")
    void testInvalidSortPropertyRejected() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .sortBy("unauthorizedColumn")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field: 'unauthorizedColumn'")))
                .andExpect(jsonPath("$.message", containsString("[id, name, price, billingCycle, maxMembers, maxTrainers]")));
    }

    @Test
    @DisplayName("Null filter objects cleanly execute with default pagination")
    void testNullFilterObjectsExecuteWithDefaults() throws Exception {
        PlatformAdminRequest planRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(10)));

        PlatformAdminRequest tenantRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(10)));
    }
}
