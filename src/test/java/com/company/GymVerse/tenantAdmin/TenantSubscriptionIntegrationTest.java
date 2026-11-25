package com.company.GymVerse.tenantAdmin;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.tenantAdmin.request.subscription.FetchTenantSubscriptionRequest;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 4.6: Subscription Insights & Entitlements Management (/api/tenant-admin) Integration Tests")
class TenantSubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private GymSubscriptionRepository gymSubscriptionRepository;

    private String validTenantAdminToken;
    private String memberTenantToken;
    private String masterToken;
    private GymTenant gymTenant;
    private SaasSubscriptionPlan plan;
    private GymSubscription trialSubscription;
    private GymSubscription activeSubscription;

    @BeforeEach
    void setUp() {
        gymTenant = GymTenant.builder()
                .id(101L)
                .gymName("Gold Standard Gym")
                .gymCode("GOLD-01")
                .dbName("gymverse_tenant_gold_101")
                .status(GymStatus.ACTIVE)
                .build();

        plan = SaasSubscriptionPlan.builder()
                .id(1L)
                .name("Growth Tier")
                .description("All-inclusive growth plan for expanding clubs")
                .price(new BigDecimal("149.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(500)
                .maxTrainers(15)
                .analyticsEnabled(true)
                .multiTrainerEnabled(true)
                .customBrandingEnabled(true)
                .active(true)
                .build();

        LocalDate today = LocalDate.now();
        trialSubscription = GymSubscription.builder()
                .id(901L)
                .gymTenant(gymTenant)
                .plan(plan)
                .pricePaid(BigDecimal.ZERO)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(9))
                .status(GymSubscriptionStatus.TRIAL)
                .subscribedAt(Instant.now().minusSeconds(86400 * 5))
                .build();

        activeSubscription = GymSubscription.builder()
                .id(902L)
                .gymTenant(gymTenant)
                .plan(plan)
                .pricePaid(new BigDecimal("149.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(today)
                .endDate(today.plusDays(30))
                .status(GymSubscriptionStatus.ACTIVE)
                .subscribedAt(Instant.now())
                .build();

        TenantTokenClaims adminClaims = TenantTokenClaims.builder()
                .userId(2001L)
                .email("admin@goldgym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gymverse_tenant_gold_101")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();
        validTenantAdminToken = "Bearer " + jwtUtil.generateTenantAccessToken(adminClaims);

        TenantTokenClaims memberClaims = TenantTokenClaims.builder()
                .userId(3001L)
                .email("member@goldgym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gymverse_tenant_gold_101")
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .build();
        memberTenantToken = "Bearer " + jwtUtil.generateTenantAccessToken(memberClaims);

        com.company.GymVerse.models.central.UserAccount masterUser = com.company.GymVerse.models.central.UserAccount.builder()
                .id(1001L)
                .email("master@gymverse.com")
                .firstName("Master")
                .lastName("User")
                .build();
        masterToken = "Bearer " + jwtUtil.generateMasterAccessToken(masterUser);
    }

    @Test
    @DisplayName("Successfully retrieves trial subscription details and feature entitlements")
    void fetchTenantSubscription_Success_Trial() throws Exception {
        when(gymSubscriptionRepository.findFirstByGymTenantIdOrderBySubscribedAtDesc(101L))
                .thenReturn(Optional.of(trialSubscription));

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .fetchTenantSubscriptionRequest(FetchTenantSubscriptionRequest.builder().build())
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", validTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Tenant subscription details retrieved successfully"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.subscriptionId").value(901))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.tenantId").value(101))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.gymCode").value("GOLD-01"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.gymName").value("Gold Standard Gym"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.planId").value(1))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.planName").value("Growth Tier"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.planPrice").value(149.99))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.status").value("TRIAL"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.isTrial").value(true))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.daysRemaining").value(9))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.maxMembers").value(500))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.maxTrainers").value(15))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.analyticsEnabled").value(true))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.multiTrainerEnabled").value(true))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.customBrandingEnabled").value(true));
    }

    @Test
    @DisplayName("Successfully retrieves active paid subscription details")
    void fetchTenantSubscription_Success_Active() throws Exception {
        when(gymSubscriptionRepository.findFirstByGymTenantIdOrderBySubscribedAtDesc(101L))
                .thenReturn(Optional.of(activeSubscription));

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", validTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.isTrial").value(false))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.daysRemaining").value(30))
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.pricePaid").value(149.99));
    }

    @Test
    @DisplayName("Successfully processes request using 'requestType' alias in payload")
    void fetchTenantSubscription_Success_WithAliasPayload() throws Exception {
        when(gymSubscriptionRepository.findFirstByGymTenantIdOrderBySubscribedAtDesc(101L))
                .thenReturn(Optional.of(trialSubscription));

        String rawJson = "{\"requestType\":\"FETCH_TENANT_SUBSCRIPTION\"}";

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", validTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantSubscriptionDetailsResponse.subscriptionId").value(901));
    }

    @Test
    @DisplayName("Master token attempting to access tenant-admin endpoint is rejected with 403 Forbidden")
    void fetchTenantSubscription_MasterToken_ThrowsForbidden() throws Exception {
        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", masterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Tenant-scoped admin token required")));
    }

    @Test
    @DisplayName("Non-admin tenant token (e.g. MEMBER) attempting to access tenant-admin is rejected with 403 Forbidden")
    void fetchTenantSubscription_MemberToken_ThrowsForbidden() throws Exception {
        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", memberTenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("ROLE_TENANT_ADMIN")));
    }

    @Test
    @DisplayName("When no subscription exists for tenant ID, returns 404 Not Found")
    void fetchTenantSubscription_NoSubscriptionFound_ThrowsNotFound() throws Exception {
        when(gymSubscriptionRepository.findFirstByGymTenantIdOrderBySubscribedAtDesc(101L))
                .thenReturn(Optional.empty());

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", validTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No subscription found for tenant ID: 101"));
    }

    @Test
    @DisplayName("Non-admin tenant token (TRAINER) attempting to access tenant-admin is rejected with 403 Forbidden")
    void fetchTenantSubscription_TrainerToken_ThrowsForbidden() throws Exception {
        TenantTokenClaims trainerClaims = TenantTokenClaims.builder()
                .userId(4001L)
                .email("trainer@goldgym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gymverse_tenant_gold_101")
                .role(TenantRole.TRAINER)
                .adminAuthority(AdminAuthority.NONE)
                .build();
        String trainerToken = "Bearer " + jwtUtil.generateTenantAccessToken(trainerClaims);

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("ROLE_TENANT_ADMIN")));
    }

    @Test
    @DisplayName("Non-admin tenant token (STAFF) attempting to access tenant-admin is rejected with 403 Forbidden")
    void fetchTenantSubscription_StaffToken_ThrowsForbidden() throws Exception {
        TenantTokenClaims staffClaims = TenantTokenClaims.builder()
                .userId(5001L)
                .email("staff@goldgym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gymverse_tenant_gold_101")
                .role(TenantRole.STAFF)
                .adminAuthority(AdminAuthority.NONE)
                .build();
        String staffToken = "Bearer " + jwtUtil.generateTenantAccessToken(staffClaims);

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("ROLE_TENANT_ADMIN")));
    }

    @Test
    @DisplayName("Missing tenantAdminRequestType throws 400 Bad Request")
    void fetchTenantSubscription_MissingRequestType_ThrowsBadRequest() throws Exception {
        TenantAdminRequest request = TenantAdminRequest.builder().build();

        mockMvc.perform(post("/api/tenant-admin")
                        .header("Authorization", validTenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tenantAdminRequestType must not be null"));
    }

    @Test
    @DisplayName("Unauthenticated request to tenant-admin throws 401 Unauthorized")
    void fetchTenantSubscription_Unauthenticated_ThrowsUnauthorized() throws Exception {
        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        mockMvc.perform(post("/api/tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
