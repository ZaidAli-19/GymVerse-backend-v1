package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.exceptions.BadRequestException;
import com.company.GymVerse.exceptions.ResourceNotFoundException;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.response.SaasPlanResponse;
import com.company.GymVerse.platformAdmin.service.GetSaasPlanByIdService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-4: Single SaaS Subscription Plan Retrieval Integration Tests")
class GetSaasPlanByIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private PlatformAdminFactory platformAdminFactory;

    @Autowired
    private GetSaasPlanByIdService getSaasPlanByIdService;

    private static final String PLAN_NAME = "PA4_TEST_Growth_Tier";
    private SaasSubscriptionPlan savedPlan;

    @BeforeEach
    void setUp() {
        cleanTestData();

        savedPlan = planRepository.save(SaasSubscriptionPlan.builder()
                .name(PLAN_NAME)
                .description("Mid-market gym growth tier with analytics")
                .price(new BigDecimal("79.50"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(300)
                .maxTrainers(8)
                .analyticsEnabled(true)
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
        planRepository.findByNameIgnoreCase(PLAN_NAME).ifPresent(planRepository::delete);
    }

    @Test
    @DisplayName("AC-PA4.1: Successfully retrieve existing plan by ID via unified gateway")
    void testSuccessfulPlanLookupById() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(savedPlan.getId())
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("SaaS subscription plan retrieved successfully")))
                .andExpect(jsonPath("$.planId", is(savedPlan.getId().intValue())))
                .andExpect(jsonPath("$.id", is(savedPlan.getId().intValue())))
                .andExpect(jsonPath("$.name", is(PLAN_NAME)))
                .andExpect(jsonPath("$.price", is(79.50)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.saasPlan.id", is(savedPlan.getId().intValue())))
                .andExpect(jsonPath("$.saasPlan.name", is(PLAN_NAME)))
                .andExpect(jsonPath("$.saasPlan.price", is(79.50)))
                .andExpect(jsonPath("$.saasPlan.billingCycle", is("MONTHLY")))
                .andExpect(jsonPath("$.saasPlan.maxMembers", is(300)))
                .andExpect(jsonPath("$.saasPlan.maxTrainers", is(8)))
                .andExpect(jsonPath("$.saasPlan.analyticsEnabled", is(true)))
                .andExpect(jsonPath("$.saasPlan.multiTrainerEnabled", is(true)))
                .andExpect(jsonPath("$.saasPlan.customBrandingEnabled", is(false)))
                .andExpect(jsonPath("$.saasPlan.active", is(true)));
    }

    @Test
    @DisplayName("AC-PA4.2: Non-existent plan ID returns HTTP 404 Not Found")
    void testNonExistentPlanIdReturns404() throws Exception {
        long nonExistentId = 999999L;

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(nonExistentId)
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
    @DisplayName("AC-PA4.3: Missing or null planId returns HTTP 400 Bad Request")
    void testMissingPlanIdReturns400() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(null)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Plan ID is required")));
    }

    @Test
    @DisplayName("AC-PA4.3: Non-positive planId returns HTTP 400 Bad Request")
    void testNonPositivePlanIdReturns400() throws Exception {
        // Negative ID
        PlatformAdminRequest negativeRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(-5L)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Plan ID must be a positive number")));

        // Zero ID
        PlatformAdminRequest zeroRequest = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(0L)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Plan ID must be a positive number")));
    }

    @Test
    @DisplayName("Factory Dispatching: PlatformAdminFactory resolves GetSaasPlanByIdService")
    void testFactoryResolution() {
        PlatformAdminFactoryOperation operation = platformAdminFactory.getOperation(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID);
        assertThat(operation).isNotNull();
        assertThat(operation.getRequestType()).isEqualTo(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID);

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID)
                .planId(savedPlan.getId())
                .build();

        PlatformAdminResponse response = operation.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedPlan.getId());
        assertThat(response.getName()).isEqualTo(PLAN_NAME);
        assertThat(response.getSaasPlan()).isNotNull();
        assertThat(response.getSaasPlan().getName()).isEqualTo(PLAN_NAME);
    }

    @Test
    @DisplayName("Direct Service Method: getPlanById returns SaasPlanResponse or throws ResourceNotFoundException")
    void testDirectServiceMethod() {
        // Successful direct lookup
        SaasPlanResponse response = getSaasPlanByIdService.getPlanById(savedPlan.getId());
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedPlan.getId());
        assertThat(response.getName()).isEqualTo(PLAN_NAME);
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("79.50"));

        // Direct lookup of non-existent ID
        assertThatThrownBy(() -> getSaasPlanByIdService.getPlanById(888888L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SaasSubscriptionPlan not found with id: 888888");

        // Direct lookup of invalid ID
        assertThatThrownBy(() -> getSaasPlanByIdService.getPlanById(-1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Plan ID must be a positive number");
    }
}
