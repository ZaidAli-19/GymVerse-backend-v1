package com.company.GymVerse.security;

import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 3.1: Security Baseline & AuthenticationManager Integration Tests")
class SecurityBaselineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userAccountRepository.findByEmail("sec-test-user@gymverse.com")
                .ifPresent(userAccountRepository::delete);
    }

    @Test
    @DisplayName("AuthenticationManager bean should be present and operational in application context")
    void authenticationManager_IsLoaded() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @DisplayName("Public auth endpoints (/api/auth/**) must be permitted anonymously (not return 401)")
    void publicEndpoint_AuthApi_PermittedAnonymously() throws Exception {
        mockMvc.perform(get("/api/auth/health-check")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Must NOT be 401 Unauthorized or 403 Forbidden
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Protected endpoints (/api/platform-admin) must return HTTP 401 when requested without authentication")
    void protectedEndpoint_ReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestType\":\"FETCH_ALL_SAAS_PLANS\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("AuthenticationManager should successfully authenticate valid user credentials against database")
    void authenticationManager_AuthenticatesValidUserCredentials() {
        final String rawPassword = "SecurePassword123!";
        final UserAccount testUser = UserAccount.builder()
                .email("sec-test-user@gymverse.com")
                .firstName("Security")
                .lastName("Tester")
                .password(passwordEncoder.encode(rawPassword))
                .phoneNumber("+1234567890")
                .emailVerified(true)
                .isPlatformAdmin(false)
                .status(UserAccountStatus.ACTIVE)
                .build();
        userAccountRepository.save(testUser);

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("sec-test-user@gymverse.com", rawPassword)
        );

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("sec-test-user@gymverse.com");
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("AuthenticationManager should throw BadCredentialsException when password does not match")
    void authenticationManager_ThrowsBadCredentialsOnPasswordMismatch() {
        final String rawPassword = "CorrectPassword123!";
        final UserAccount testUser = UserAccount.builder()
                .email("sec-test-user@gymverse.com")
                .firstName("Security")
                .lastName("Tester")
                .password(passwordEncoder.encode(rawPassword))
                .phoneNumber("+1234567890")
                .emailVerified(true)
                .isPlatformAdmin(false)
                .status(UserAccountStatus.ACTIVE)
                .build();
        userAccountRepository.save(testUser);

        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("sec-test-user@gymverse.com", "WrongPassword!")
        )).isInstanceOf(BadCredentialsException.class);
    }
}
