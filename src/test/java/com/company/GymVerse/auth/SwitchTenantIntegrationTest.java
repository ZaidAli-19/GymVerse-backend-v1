package com.company.GymVerse.auth;

import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SwitchTenantRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 4.5: Token Exchange Gateway (/api/auth SWITCH_TENANT) Integration Tests")
class SwitchTenantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private GymTenantRepository gymTenantRepository;

    @MockBean
    private UserTenantRoleRepository userTenantRoleRepository;

    private String validMasterToken;
    private UserAccount mockUser;
    private GymTenant activeGym;
    private GymTenant suspendedGym;
    private GymTenant inactiveGym;
    private UserTenantRole activeRole;
    private UserTenantRole suspendedRole;
    private UserTenantRole pendingRole;
    private UserTenantRole memberRole;

    @BeforeEach
    void setUp() {
        mockUser = UserAccount.builder()
                .id(2001L)
                .email("caller@gymverse.com")
                .firstName("Jordan")
                .lastName("Lee")
                .build();

        validMasterToken = "Bearer " + jwtUtil.generateMasterAccessToken(mockUser);

        when(userAccountRepository.findById(2001L)).thenReturn(Optional.of(mockUser));
        when(userAccountRepository.findByEmail("caller@gymverse.com")).thenReturn(Optional.of(mockUser));

        activeGym = GymTenant.builder()
                .id(101L)
                .gymName("Gold Standard Gym")
                .gymCode("GOLD-01")
                .dbName("gymverse_tenant_gold_101")
                .status(GymStatus.ACTIVE)
                .build();

        suspendedGym = GymTenant.builder()
                .id(102L)
                .gymName("Suspended Gym")
                .gymCode("SUSP-02")
                .dbName("gymverse_tenant_susp_102")
                .status(GymStatus.SUSPENDED)
                .build();

        inactiveGym = GymTenant.builder()
                .id(103L)
                .gymName("Inactive Gym")
                .gymCode("INACT-03")
                .dbName("gymverse_tenant_inact_103")
                .status(GymStatus.INACTIVE)
                .build();

        activeRole = UserTenantRole.builder()
                .id(501L)
                .userAccount(mockUser)
                .gymTenant(activeGym)
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .status(TenantMembershipStatus.ACTIVE)
                .build();

        memberRole = UserTenantRole.builder()
                .id(502L)
                .userAccount(mockUser)
                .gymTenant(activeGym)
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .status(TenantMembershipStatus.ACTIVE)
                .build();

        suspendedRole = UserTenantRole.builder()
                .id(503L)
                .userAccount(mockUser)
                .gymTenant(activeGym)
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .status(TenantMembershipStatus.SUSPENDED)
                .build();

        pendingRole = UserTenantRole.builder()
                .id(504L)
                .userAccount(mockUser)
                .gymTenant(activeGym)
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .status(TenantMembershipStatus.PENDING)
                .build();

        when(gymTenantRepository.findById(101L)).thenReturn(Optional.of(activeGym));
        when(gymTenantRepository.findByGymCode("GOLD-01")).thenReturn(Optional.of(activeGym));
        when(gymTenantRepository.findById(102L)).thenReturn(Optional.of(suspendedGym));
        when(gymTenantRepository.findByGymCode("SUSP-02")).thenReturn(Optional.of(suspendedGym));
        when(gymTenantRepository.findById(103L)).thenReturn(Optional.of(inactiveGym));
        when(gymTenantRepository.findByGymCode("INACT-03")).thenReturn(Optional.of(inactiveGym));
    }

    @Test
    @DisplayName("Successfully exchanges master token for tenant token pair using gymTenantId")
    void switchTenant_Success_ById() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(activeRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.switchTenantResponse.tenantId").value(101))
                .andExpect(jsonPath("$.switchTenantResponse.gymCode").value("GOLD-01"))
                .andExpect(jsonPath("$.switchTenantResponse.gymName").value("Gold Standard Gym"))
                .andExpect(jsonPath("$.switchTenantResponse.tenantDbName").value("gymverse_tenant_gold_101"))
                .andExpect(jsonPath("$.switchTenantResponse.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.switchTenantResponse.adminAuthority").value("PRIMARY"))
                .andExpect(jsonPath("$.switchTenantResponse.tenantAccessToken").isNotEmpty())
                .andExpect(jsonPath("$.switchTenantResponse.tenantRefreshToken").isNotEmpty())
                .andDo(result -> {
                    String json = result.getResponse().getContentAsString();
                    AuthResponse res = objectMapper.readValue(json, AuthResponse.class);
                    assertThat(res.getSwitchTenantResponse()).isNotNull();

                    // Validate token claims directly
                    String token = res.getSwitchTenantResponse().getTenantAccessToken();
                    assertThat(jwtUtil.extractTokenType(token)).isEqualTo("TENANT_ACCOUNT");
                    assertThat(jwtUtil.extractTenantDbName(token)).isEqualTo("gymverse_tenant_gold_101");
                    assertThat(jwtUtil.extractGymCode(token)).isEqualTo("GOLD-01");
                    assertThat(jwtUtil.extractRole(token)).isEqualTo("TENANT_ADMIN");
                });
    }

    @Test
    @DisplayName("Successfully exchanges master token for tenant token pair using gymCode")
    void switchTenant_Success_ByGymCode() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(activeRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymCode("GOLD-01")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.switchTenantResponse.gymCode").value("GOLD-01"))
                .andExpect(jsonPath("$.switchTenantResponse.tenantDbName").value("gymverse_tenant_gold_101"));
    }

    @Test
    @DisplayName("Switching to an unauthorized gym returns 404 Not Found")
    void switchTenant_UnauthorizedGym_ThrowsNotFound() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of());

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Switching to a suspended gym returns 403 Forbidden")
    void switchTenant_SuspendedGym_ThrowsForbidden() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(102L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("is not active")));
    }

    @Test
    @DisplayName("Switching to an inactive gym returns 403 Forbidden")
    void switchTenant_InactiveGym_ThrowsForbidden() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(103L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("is not active")));
    }

    @Test
    @DisplayName("Switching to a gym with suspended membership status returns 403 Forbidden")
    void switchTenant_SuspendedRole_ThrowsForbidden() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(suspendedRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("is not active")));
    }

    @Test
    @DisplayName("Switching to a gym with pending membership status returns 403 Forbidden")
    void switchTenant_PendingRole_ThrowsForbidden() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(pendingRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("is not active")));
    }

    @Test
    @DisplayName("Conflicting gymTenantId and gymCode throws 400 Bad Request")
    void switchTenant_ConflictingTarget_ThrowsBadRequest() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .gymCode("DIFFERENT-99")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Conflicting gymTenantId and gymCode provided"));
    }

    @Test
    @DisplayName("Disambiguates role when explicit role is requested among multiple associations")
    void switchTenant_MultipleRoles_DisambiguateExplicitRole() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(activeRole, memberRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .role(TenantRole.MEMBER)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.switchTenantResponse.role").value("MEMBER"))
                .andExpect(jsonPath("$.switchTenantResponse.adminAuthority").value("NONE"));
    }

    @Test
    @DisplayName("Switching to gym with multiple roles without specifying role returns 400 Bad Request")
    void switchTenant_MultipleRoles_MissingRole_ThrowsBadRequest() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(memberRole, activeRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User holds multiple roles in this gym. Please specify the 'role' parameter to switch tenants."));
    }

    @Test
    @DisplayName("Requesting a role the user does not hold returns 404 Not Found")
    void switchTenant_UnownedRole_ThrowsNotFound() throws Exception {
        when(userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(2001L, 101L))
                .thenReturn(List.of(activeRole));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .role(TenantRole.TRAINER)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User does not have requested role TRAINER in gym: Gold Standard Gym"));
    }

    @Test
    @DisplayName("Switching with a tenant-scoped token is rejected with 401 Unauthorized")
    void switchTenant_WithTenantScopedToken_ThrowsUnauthorized() throws Exception {
        TenantTokenClaims tenantClaims = TenantTokenClaims.builder()
                .userId(2001L)
                .email("caller@gymverse.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gymverse_tenant_gold_101")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();
        String tenantScopedToken = "Bearer " + jwtUtil.generateTenantAccessToken(tenantClaims);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", tenantScopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to SWITCH_TENANT throws 401 Unauthorized")
    void switchTenant_Unauthenticated_ThrowsUnauthorized() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder()
                        .gymTenantId(101L)
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing both gymTenantId and gymCode throws 400 Bad Request")
    void switchTenant_MissingTarget_ThrowsBadRequest() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SWITCH_TENANT)
                .switchTenantRequest(SwitchTenantRequest.builder().build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Either gymTenantId or gymCode must be provided to switch tenants"));
    }
}
