package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-2: SaaS Subscription Plan Creation Integration Tests")
class CreateSaasPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private PlatformAdminFactory platformAdminFactory;

    private static final String TEST_PLAN_NAME = "Enterprise Gold";
    private static final String DUPLICATE_TEST_PLAN = "Starter Tier";
    private static final String FACTORY_DIRECT_TEST_PLAN = "Factory Direct Tier";

    @AfterEach
    void tearDown() {
        List<String> namesToClean = List.of(
                TEST_PLAN_NAME,
                DUPLICATE_TEST_PLAN,
                "starter tier",
                "Budget Tier",
                FACTORY_DIRECT_TEST_PLAN
        );
        for (String name : namesToClean) {
            planRepository.findByNameIgnoreCase(name).ifPresent(planRepository::delete);
        }
    }

    @Test
    @DisplayName("AC-PA2.1: Successful SaaS Plan Creation via single /api/platform-admin endpoint persists to central DB and returns 200 OK")
    void testSuccessfulPlanCreation() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name(TEST_PLAN_NAME)
                .description("Full tier gym plan with unlimited members and custom branding")
                .price(new BigDecimal("199.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(1000)
                .maxTrainers(25)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(true)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is(TEST_PLAN_NAME)))
                .andExpect(jsonPath("$.price", is(199.99)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.message", is("SaaS subscription plan created successfully")))
                .andExpect(jsonPath("$.saasPlan.id", notNullValue()))
                .andExpect(jsonPath("$.saasPlan.name", is(TEST_PLAN_NAME)))
                .andExpect(jsonPath("$.saasPlan.description", is("Full tier gym plan with unlimited members and custom branding")))
                .andExpect(jsonPath("$.saasPlan.price", is(199.99)))
                .andExpect(jsonPath("$.saasPlan.billingCycle", is("MONTHLY")))
                .andExpect(jsonPath("$.saasPlan.maxMembers", is(1000)))
                .andExpect(jsonPath("$.saasPlan.maxTrainers", is(25)))
                .andExpect(jsonPath("$.saasPlan.analyticsEnabled", is(true)))
                .andExpect(jsonPath("$.saasPlan.multiTrainerEnabled", is(true)))
                .andExpect(jsonPath("$.saasPlan.customBrandingEnabled", is(true)))
                .andExpect(jsonPath("$.saasPlan.active", is(true)));

        // Verify entity persisted to live central database
        Optional<SaasSubscriptionPlan> persistedOpt = planRepository.findByNameIgnoreCase(TEST_PLAN_NAME);
        assertThat(persistedOpt).isPresent();

        SaasSubscriptionPlan persisted = persistedOpt.get();
        assertThat(persisted.getName()).isEqualTo(TEST_PLAN_NAME);
        assertThat(persisted.getPrice().compareTo(new BigDecimal("199.99"))).isZero();
        assertThat(persisted.getPrice().scale()).isEqualTo(2);
        assertThat(persisted.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(persisted.getMaxMembers()).isEqualTo(1000);
        assertThat(persisted.getMaxTrainers()).isEqualTo(25);
        assertThat(persisted.isAnalyticsEnabled()).isTrue();
        assertThat(persisted.isMultiTrainerEnabled()).isTrue();
        assertThat(persisted.isCustomBrandingEnabled()).isTrue();
        assertThat(persisted.isActive()).isTrue();
    }

    @Test
    @DisplayName("AC-PA2.2: Duplicate Plan Name Rejection throws ConflictException and returns 409 Conflict")
    void testDuplicatePlanNameReturnsConflict() throws Exception {
        // Pre-insert an existing plan
        SaasSubscriptionPlan existing = SaasSubscriptionPlan.builder()
                .name(DUPLICATE_TEST_PLAN)
                .description("Initial starter tier")
                .price(new BigDecimal("49.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(100)
                .maxTrainers(5)
                .active(true)
                .build();
        planRepository.save(existing);

        // Attempt creating another plan with differing case
        PlatformAdminRequest duplicateRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("starter tier")
                .description("Duplicate tier attempt")
                .price(new BigDecimal("59.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(150)
                .maxTrainers(8)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("A plan with name 'starter tier' already exists")))
                .andExpect(jsonPath("$.path", is("/api/platform-admin")));
    }

    @Test
    @DisplayName("AC-PA2.3: Monetary Precision & Range Validation rejects negative price with 400 Bad Request")
    void testNegativePriceReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("Budget Tier")
                .price(new BigDecimal("-10.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Price must be a non-negative decimal value")));
    }

    @Test
    @DisplayName("AC-PA2.3: Scale Validation rejects price with more than 2 decimal places with 400 Bad Request")
    void testExcessiveScalePriceReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("Budget Tier")
                .price(new BigDecimal("19.999"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Price cannot have more than 2 decimal places")));
    }

    @Test
    @DisplayName("AC-PA2.4: Capacity Limits Validation rejects zero members with 400 Bad Request")
    void testZeroMembersReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("Budget Tier")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(0)
                .maxTrainers(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Maximum members must be at least 1 and trainers must be non-negative")));
    }

    @Test
    @DisplayName("AC-PA2.4: Capacity Limits Validation rejects negative trainers with 400 Bad Request")
    void testNegativeTrainersReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("Budget Tier")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(-1)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Maximum members must be at least 1 and trainers must be non-negative")));
    }

    @Test
    @DisplayName("Validation fails on blank plan name with 400 Bad Request")
    void testBlankPlanNameReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name("   ")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Missing PlatformAdminRequestType returns 400 Bad Request")
    void testMissingRequestTypeReturnsBadRequest() throws Exception {
        PlatformAdminRequest invalidRequest = PlatformAdminRequest.builder()
                .name("Missing Type Tier")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("PlatformAdminRequestType must not be null")));
    }

    @Test
    @DisplayName("PlatformAdminFactory directly resolves operation and executes creation returning PlatformAdminResponse")
    void testDirectFactoryExecution() {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.CREATE_SAAS_PLAN)
                .name(FACTORY_DIRECT_TEST_PLAN)
                .description("Created directly via module factory dispatch")
                .price(new BigDecimal("249.50"))
                .billingCycle(BillingCycle.ANNUAL)
                .maxMembers(500)
                .maxTrainers(10)
                .analyticsEnabled(true)
                .build();

        PlatformAdminFactoryOperation operation = platformAdminFactory.getOperation(PlatformAdminRequestType.CREATE_SAAS_PLAN);
        assertThat(operation).isNotNull();

        PlatformAdminResponse response = operation.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo(FACTORY_DIRECT_TEST_PLAN);
        assertThat(response.getPrice().compareTo(new BigDecimal("249.50"))).isZero();
        assertThat(response.getActive()).isTrue();
        assertThat(response.getMessage()).isEqualTo("SaaS subscription plan created successfully");
        assertThat(response.getSaasPlan()).isNotNull();
        assertThat(response.getSaasPlan().getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);

        // Verify entity in repository
        Optional<SaasSubscriptionPlan> persistedOpt = planRepository.findByNameIgnoreCase(FACTORY_DIRECT_TEST_PLAN);
        assertThat(persistedOpt).isPresent();
    }
}
