package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 3.2: JwtFilter Integration Tests")
class JwtFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Valid MASTER_ACCOUNT access token should authenticate and allow request past security barrier")
    void validMasterAccessToken_AuthorizesProtectedEndpoint() throws Exception {
        final UserAccount account = UserAccount.builder()
                .id(42L)
                .email("verified@gymverse.com")
                .isPlatformAdmin(false)
                .build();

        final String accessToken = jwtUtil.generateMasterAccessToken(account);

        mockMvc.perform(post("/api/platform-admin")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    // Must NOT be 401 Unauthorized - request must have passed security filter
                    assertThat(statusCode).isNotEqualTo(401);
                });
    }

    @Test
    @DisplayName("Refresh token must be rejected with HTTP 401 when attempted on protected API endpoints")
    void refreshToken_RejectedWith401_OnProtectedEndpoint() throws Exception {
        final UserAccount account = UserAccount.builder()
                .id(43L)
                .email("refresh.user@gymverse.com")
                .isPlatformAdmin(false)
                .build();

        final String refreshToken = jwtUtil.generateMasterRefreshToken(account);

        mockMvc.perform(post("/api/platform-admin")
                        .header("Authorization", "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token cannot be used to access APIs"));
    }

    @Test
    @DisplayName("Tampered or invalid token must be rejected with HTTP 401")
    void tamperedToken_RejectedWith401() throws Exception {
        mockMvc.perform(post("/api/platform-admin")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or malformed JWT token"));
    }

    @Test
    @DisplayName("Empty Bearer header must be rejected with HTTP 401")
    void emptyBearerHeader_RejectedWith401() throws Exception {
        mockMvc.perform(post("/api/platform-admin")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or malformed JWT token"));
    }

    @Test
    @DisplayName("Public endpoints must permit requests without Authorization header")
    void publicEndpoint_PermittedWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/ping")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    assertThat(statusCode).isNotEqualTo(401);
                    assertThat(statusCode).isNotEqualTo(403);
                });
    }
}
