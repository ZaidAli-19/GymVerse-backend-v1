package com.company.GymVerse.user;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.user.request.FetchMyTenantsRequest;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 4.5: User Tenants Directory (/api/user FETCH_MY_TENANTS) Integration Tests")
class FetchMyTenantsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private UserTenantRoleRepository userTenantRoleRepository;

    @MockBean
    private GymSubscriptionRepository gymSubscriptionRepository;

    private String validMasterToken;
    private UserAccount mockUser;
    private GymTenant gym1;
    private GymTenant gym2;
    private UserTenantRole role1;
    private UserTenantRole role2;

    @BeforeEach
    void setUp() {
        mockUser = UserAccount.builder()
                .id(1001L)
                .email("user@gymverse.com")
                .firstName("Alex")
                .lastName("Smith")
                .build();

        validMasterToken = "Bearer " + jwtUtil.generateMasterAccessToken(mockUser);

        when(userAccountRepository.findById(1001L)).thenReturn(Optional.of(mockUser));
        when(userAccountRepository.findByEmail("user@gymverse.com")).thenReturn(Optional.of(mockUser));

        gym1 = GymTenant.builder()
                .id(10L)
                .gymName("Iron Paradise")
                .gymCode("IRON-01")
                .dbName("gymverse_tenant_iron_10")
                .city("Los Angeles")
                .address("100 Muscle Blvd")
                .status(GymStatus.ACTIVE)
                .build();

        gym2 = GymTenant.builder()
                .id(20L)
                .gymName("Metro Flex")
                .gymCode("METRO-02")
                .dbName("gymverse_tenant_metro_20")
                .city("Dallas")
                .address("200 Power Ave")
                .status(GymStatus.ACTIVE)
                .build();

        role1 = UserTenantRole.builder()
                .id(101L)
                .userAccount(mockUser)
                .gymTenant(gym1)
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .status(TenantMembershipStatus.ACTIVE)
                .joinedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        role2 = UserTenantRole.builder()
                .id(102L)
                .userAccount(mockUser)
                .gymTenant(gym2)
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .status(TenantMembershipStatus.ACTIVE)
                .joinedAt(Instant.parse("2026-02-01T12:00:00Z"))
                .build();

        GymSubscription sub1 = GymSubscription.builder()
                .gymTenant(gym1)
                .status(GymSubscriptionStatus.TRIAL)
                .build();

        GymSubscription sub2 = GymSubscription.builder()
                .gymTenant(gym2)
                .status(GymSubscriptionStatus.ACTIVE)
                .build();

        when(gymSubscriptionRepository.findByGymTenantIdOrderBySubscribedAtDesc(10L)).thenReturn(List.of(sub1));
        when(gymSubscriptionRepository.findByGymTenantIdOrderBySubscribedAtDesc(20L)).thenReturn(List.of(sub2));
    }

    @Test
    @DisplayName("Successfully retrieves user tenant directory with multiple gym roles")
    void fetchMyTenants_Success_MultipleTenants() throws Exception {
        when(userTenantRoleRepository.findByUserAccountId(1001L)).thenReturn(List.of(role1, role2));

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_MY_TENANTS)
                .build();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User tenant directory retrieved successfully"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.totalTenants").value(2))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].gymCode").value("IRON-01"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].gymName").value("Iron Paradise"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].userRole").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].adminAuthority").value("PRIMARY"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].subscriptionStatus").value("TRIAL"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[1].gymCode").value("METRO-02"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[1].gymName").value("Metro Flex"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[1].userRole").value("MEMBER"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[1].adminAuthority").value("NONE"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[1].subscriptionStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("Successfully returns empty list when user has zero tenant associations")
    void fetchMyTenants_Success_EmptyList() throws Exception {
        when(userTenantRoleRepository.findByUserAccountId(1001L)).thenReturn(List.of());

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_MY_TENANTS)
                .build();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.totalTenants").value(0))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants").isEmpty());
    }

    @Test
    @DisplayName("Successfully filters tenants by role")
    void fetchMyTenants_WithRoleFilter() throws Exception {
        when(userTenantRoleRepository.findByUserAccountId(1001L)).thenReturn(List.of(role1, role2));

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_MY_TENANTS)
                .fetchMyTenantsRequest(FetchMyTenantsRequest.builder()
                        .role(TenantRole.TENANT_ADMIN)
                        .build())
                .build();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetchMyTenantsResponse.totalTenants").value(1))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].gymCode").value("IRON-01"))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].userRole").value("TENANT_ADMIN"));
    }

    @Test
    @DisplayName("Successfully processes request using 'userRequestType' alias in payload")
    void fetchMyTenants_Success_WithAliasPayload() throws Exception {
        when(userTenantRoleRepository.findByUserAccountId(1001L)).thenReturn(List.of(role1));

        String rawJson = "{\"userRequestType\":\"FETCH_MY_TENANTS\"}";

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.totalTenants").value(1))
                .andExpect(jsonPath("$.fetchMyTenantsResponse.tenants[0].gymCode").value("IRON-01"));
    }

    @Test
    @DisplayName("Tenant-scoped token attempting FETCH_MY_TENANTS is rejected with 401 Unauthorized")
    void fetchMyTenants_WithTenantScopedToken_ThrowsUnauthorized() throws Exception {
        TenantTokenClaims tenantClaims = TenantTokenClaims.builder()
                .userId(1001L)
                .email("user@gymverse.com")
                .tenantId(10L)
                .gymCode("IRON-01")
                .tenantDbName("gymverse_tenant_iron_10")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();
        String tenantScopedToken = "Bearer " + jwtUtil.generateTenantAccessToken(tenantClaims);

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_MY_TENANTS)
                .build();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", tenantScopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to FETCH_MY_TENANTS throws 401 Unauthorized")
    void fetchMyTenants_Unauthenticated_ThrowsUnauthorized() throws Exception {
        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_MY_TENANTS)
                .build();

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
