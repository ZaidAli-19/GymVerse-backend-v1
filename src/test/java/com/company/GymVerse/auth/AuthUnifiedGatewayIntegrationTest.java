package com.company.GymVerse.auth;

import com.company.GymVerse.auth.factory.AuthFactory;
import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.LoginRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.exceptions.otp.OtpRateLimitException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@DisplayName("Story 3.8: Unified Auth Controller Gateway (/api/auth) Integration Tests")
class AuthUnifiedGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthFactory authFactory;

    @Test
    @DisplayName("POST /api/auth returns HTTP 400 when authRequestType is null or missing")
    void postAuth_WhenRequestTypeMissing_ReturnsHttp400() throws Exception {
        AuthRequest request = AuthRequest.builder().build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("AuthRequestType must not be null"));
    }

    @Test
    @DisplayName("POST /api/auth returns HTTP 400 when unsupported authRequestType is supplied")
    void postAuth_WhenUnsupportedRequestType_ReturnsHttp400() throws Exception {
        when(authFactory.getOperation(any())).thenReturn(null);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_PASSWORD)
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported auth request type: LOGIN_PASSWORD"));
    }

    @Test
    @DisplayName("POST /api/auth routes successfully to operation handler and returns HTTP 200")
    void postAuth_Success_ReturnsHttp200() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.SIGNUP_INITIATE)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenReturn(
                AuthResponse.builder()
                        .message("Verification OTP has been sent to your email")
                        .build()
        );

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_INITIATE)
                .signupRequest(SignupRequest.builder()
                        .email("user@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification OTP has been sent to your email"));
    }

    @Test
    @DisplayName("POST /api/auth maps BadCredentialsException to HTTP 401 Unauthorized")
    void postAuth_WhenBadCredentials_ReturnsHttp401() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.LOGIN_PASSWORD)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_PASSWORD)
                .loginRequest(LoginRequest.builder()
                        .email("user@example.com")
                        .password("wrong-pass")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    @DisplayName("POST /api/auth maps OtpRateLimitException to HTTP 429 Too Many Requests")
    void postAuth_WhenRateLimited_ReturnsHttp429() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.LOGIN_OTP_INITIATE)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenThrow(new OtpRateLimitException("Please wait 45 seconds before requesting a new OTP"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_OTP_INITIATE)
                .loginRequest(LoginRequest.builder()
                        .email("user@example.com")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Please wait 45 seconds before requesting a new OTP"));
    }

    @Test
    @DisplayName("POST /api/auth maps ConflictException to HTTP 409 Conflict")
    void postAuth_WhenConflict_ReturnsHttp409() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.SIGNUP_INITIATE)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenThrow(new ConflictException("An account with email user@example.com already exists"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_INITIATE)
                .signupRequest(SignupRequest.builder()
                        .email("user@example.com")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with email user@example.com already exists"));
    }

    @Test
    @DisplayName("POST /api/auth maps ResourceNotFoundException to HTTP 404 Not Found")
    void postAuth_WhenNotFound_ReturnsHttp404() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.LOGIN_OTP_INITIATE)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenThrow(new ResourceNotFoundException("No account registered with email user@example.com"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_OTP_INITIATE)
                .loginRequest(LoginRequest.builder()
                        .email("user@example.com")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No account registered with email user@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth maps OtpInvalidException to HTTP 400 Bad Request")
    void postAuth_WhenOtpInvalid_ReturnsHttp400() throws Exception {
        AuthOperation mockOperation = org.mockito.Mockito.mock(AuthOperation.class);
        when(authFactory.getOperation(AuthRequestType.SIGNUP_VERIFY)).thenReturn(mockOperation);
        when(mockOperation.execute(any())).thenThrow(new OtpInvalidException("Invalid OTP code. 3 attempts remaining"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_VERIFY)
                .signupRequest(SignupRequest.builder()
                        .email("user@example.com")
                        .otp("111111")
                        .build())
                .build();

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid OTP code. 3 attempts remaining"));
    }
}
