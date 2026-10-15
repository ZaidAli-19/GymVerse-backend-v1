package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.request.UpdatePlanAvailabilityRequest;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-5: SaaS Subscription Plan Availability Management Integration Tests")
class UpdatePlanAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private PlatformAdminFactory platformAdminFactory;

    private static final String PLAN_PREFIX = "PA5_TEST_";
    private SaasSubscriptionPlan activePlan;
    private SaasSubscriptionPlan inactivePlan;

    @BeforeEach
    void setUp() {
        cleanTestData();

        // Active plan for deactivation tests
        activePlan = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Summer Special")
                .description("Seasonal promotional tier")
                .price(new BigDecimal("49.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(50)
                .maxTrainers(2)
                .analyticsEnabled(false)
                .multiTrainerEnabled(false)
                .customBrandingEnabled(false)
                .active(true)
                .build());

        // Inactive plan for reactivation tests
        inactivePlan = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Winter Archive")
                .description("Legacy winter tier")
                .price(new BigDecimal("39.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(40)
                .maxTrainers(1)
                .analyticsEnabled(false)
                .multiTrainerEnabled(false)
                .customBrandingEnabled(false)
                .active(false)
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
    @DisplayName("AC-PA5.1: Successfully deactivate (retire) an active SaaS plan")
    void testSuccessfulPlanDeactivation() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(activePlan.getId())
                .active(false)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SaaS subscription plan availability updated to: INACTIVE")))
                .andExpect(jsonPath("$.planId", is(activePlan.getId().intValue())))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.saasPlan.active", is(false)))
                .andExpect(jsonPath("$.saasPlan.price", is(49.99)))
                .andExpect(jsonPath("$.saasPlan.maxMembers", is(50)));

        // Verify in database
        SaasSubscriptionPlan reloaded = planRepository.findById(activePlan.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();

        // Verify excluded from tenant-facing active catalog
        List<SaasSubscriptionPlan> activePlans = planRepository.findByActiveTrue();
        assertThat(activePlans).noneMatch(p -> p.getId().equals(activePlan.getId()));
    }

    @Test
    @DisplayName("AC-PA5.2: Successfully reactivate an inactive SaaS plan")
    void testSuccessfulPlanReactivation() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(inactivePlan.getId())
                .active(true)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SaaS subscription plan availability updated to: ACTIVE")))
                .andExpect(jsonPath("$.planId", is(inactivePlan.getId().intValue())))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.saasPlan.active", is(true)));

        // Verify in database
        SaasSubscriptionPlan reloaded = planRepository.findById(inactivePlan.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isTrue();

        // Verify included in tenant-facing active catalog
        List<SaasSubscriptionPlan> activePlans = planRepository.findByActiveTrue();
        assertThat(activePlans).anyMatch(p -> p.getId().equals(inactivePlan.getId()));
    }

    @Test
    @DisplayName("AC-PA5.3: Critical Immutability Invariant - only active flag is modified")
    void testAttributeImmutabilityGuarantee() throws Exception {
        SaasSubscriptionPlan complexPlan = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_PREFIX + "Immutable Tier")
                .description("Rigorous immutability test tier")
                .price(new BigDecimal("99.00"))
                .billingCycle(BillingCycle.ANNUAL)
                .maxMembers(500)
                .maxTrainers(12)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(true)
                .active(true)
                .build());

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(complexPlan.getId())
                .active(false)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Reload from database and strictly verify every property
        SaasSubscriptionPlan reloaded = planRepository.findById(complexPlan.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getName()).isEqualTo(PLAN_PREFIX + "Immutable Tier");
        assertThat(reloaded.getDescription()).isEqualTo("Rigorous immutability test tier");
        assertThat(reloaded.getPrice()).isEqualByComparingTo(new BigDecimal("99.00"));
        assertThat(reloaded.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
        assertThat(reloaded.getMaxMembers()).isEqualTo(500);
        assertThat(reloaded.getMaxTrainers()).isEqualTo(12);
        assertThat(reloaded.isAnalyticsEnabled()).isTrue();
        assertThat(reloaded.isMultiTrainerEnabled()).isTrue();
        assertThat(reloaded.isCustomBrandingEnabled()).isTrue();
    }

    @Test
    @DisplayName("AC-PA5.4: Non-existent plan ID returns HTTP 404 Not Found")
    void testNonExistentPlanReturns404() throws Exception {
        long nonExistentId = 888888L;

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(nonExistentId)
                .active(false)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("SaasSubscriptionPlan not found with id: " + nonExistentId)));
    }

    @Test
    @DisplayName("Validation: Null active status returns HTTP 400 Bad Request")
    void testNullActiveStatusReturns400() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(activePlan.getId())
                .active(null)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Plan availability status must be specified as true or false")));
    }

    @Test
    @DisplayName("Validation: Missing or non-positive plan ID returns HTTP 400 Bad Request")
    void testInvalidPlanIdReturns400() throws Exception {
        // Missing plan ID
        PlatformAdminRequest nullIdRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(null)
                .active(true)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullIdRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Plan ID is required")));

        // Negative plan ID
        PlatformAdminRequest negIdRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(-2L)
                .active(true)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negIdRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Plan ID must be a positive number")));
    }

    @Test
    @DisplayName("Factory Dispatching: PlatformAdminFactory resolves UpdatePlanAvailabilityService")
    void testFactoryDispatching() {
        PlatformAdminFactoryOperation operation = platformAdminFactory.getOperation(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY);
        assertThat(operation).isNotNull();
        assertThat(operation.getRequestType()).isEqualTo(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY);

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planId(activePlan.getId())
                .active(false)
                .build();

        PlatformAdminResponse response = operation.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("SaaS subscription plan availability updated to: INACTIVE");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    @DisplayName("Nested payload support: Using planAvailability helper object")
    void testNestedPlanAvailabilityPayload() throws Exception {
        UpdatePlanAvailabilityRequest nestedPayload = UpdatePlanAvailabilityRequest.builder()
                .planId(activePlan.getId())
                .active(false)
                .build();

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .planAvailability(nestedPayload)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.message", is("SaaS subscription plan availability updated to: INACTIVE")));
    }
}
