package com.company.GymVerse.user;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import com.company.GymVerse.user.dto.TenantProvisioningResult;
import com.company.GymVerse.user.service.TenantProvisioningService;
import com.company.GymVerse.user.request.RegisterGymRequest;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 4.4R: Central User Module Gateway (/api/user) Integration Tests")
class RegisterGymIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private TenantProvisioningService tenantProvisioningService;

    private String validMasterToken;
    private UserAccount mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserAccount.builder()
                .id(1001L)
                .email("owner@gymverse.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .build();

        validMasterToken = "Bearer " + jwtUtil.generateMasterAccessToken(mockUser);

        when(userAccountRepository.findById(1001L)).thenReturn(Optional.of(mockUser));
        when(userAccountRepository.findByEmail("owner@gymverse.com")).thenReturn(Optional.of(mockUser));
    }

    private UserRequest buildValidRegisterRequest() {
        return UserRequest.builder()
                .requestType(UserRequestType.REGISTER_GYM)
                .registerGymRequest(RegisterGymRequest.builder()
                        .gymName("Iron Paradise")
                        .address("100 Ocean Drive")
                        .city("Miami")
                        .customGymCode("IRON99")
                        .build())
                .build();
    }

    @Test
    @DisplayName("REGISTER_GYM succeeds with valid master token and provisions tenant")
    void registerGym_Success() throws Exception {
        GymTenant mockTenant = GymTenant.builder()
                .id(50L)
                .gymName("Iron Paradise")
                .gymCode("IRON99")
                .dbName("gymverse_tenant_iron99")
                .address("100 Ocean Drive")
                .city("Miami")
                .status(GymStatus.ACTIVE)
                .build();

        GymSubscription mockSub = GymSubscription.builder()
                .id(200L)
                .status(GymSubscriptionStatus.TRIAL)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .build();

        com.company.GymVerse.models.central.UserTenantRole mockRole = com.company.GymVerse.models.central.UserTenantRole.builder()
                .userAccount(mockUser)
                .gymTenant(mockTenant)
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();

        TenantProvisioningResult mockResult = TenantProvisioningResult.builder()
                .gymTenant(mockTenant)
                .gymCode("IRON99")
                .dbName("gymverse_tenant_iron99")
                .userTenantRole(mockRole)
                .gymSubscription(mockSub)
                .trialEndDate(LocalDate.now().plusDays(14))
                .trialDaysRemaining(14L)
                .build();

        when(tenantProvisioningService.provisionTenant(any(TenantProvisioningCommand.class)))
                .thenReturn(mockResult);

        UserRequest request = buildValidRegisterRequest();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.registerGymResponse.gymCode").value("IRON99"))
                .andExpect(jsonPath("$.registerGymResponse.gymName").value("Iron Paradise"))
                .andExpect(jsonPath("$.registerGymResponse.userRole").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.registerGymResponse.adminAuthority").value("PRIMARY"))
                .andExpect(jsonPath("$.registerGymResponse.subscriptionStatus").value("TRIAL"))
                .andExpect(jsonPath("$.registerGymResponse.tenantAccessToken").isNotEmpty())
                .andExpect(jsonPath("$.registerGymResponse.tenantRefreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/user is rejected with 401 Unauthorized")
    void registerGym_Unauthenticated() throws Exception {
        UserRequest request = buildValidRegisterRequest();

        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with null UserRequestType is rejected with 400 Bad Request")
    void registerGym_NullRequestType() throws Exception {
        UserRequest request = UserRequest.builder().build();

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("UserRequestType must not be null"));
    }

    @Test
    @DisplayName("Request with missing gym name is rejected with 400 Bad Request")
    void registerGym_MissingName() throws Exception {
        UserRequest request = buildValidRegisterRequest();
        request.getRegisterGymRequest().setGymName("  ");

        mockMvc.perform(post("/api/user")
                        .header("Authorization", validMasterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gym name is required"));
    }
}
