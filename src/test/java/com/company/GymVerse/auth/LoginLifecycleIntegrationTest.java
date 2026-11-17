package com.company.GymVerse.auth;

import com.company.GymVerse.auth.factory.AuthFactory;
import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.LoginRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.service.LoginOtpInitiateService;
import com.company.GymVerse.auth.service.LoginOtpVerifyService;
import com.company.GymVerse.auth.service.LoginPasswordService;
import com.company.GymVerse.auth.service.RefreshTokenService;
import com.company.GymVerse.auth.validations.AuthValidationsImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginLifecycleIntegrationTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpService otpService;

    private AuthValidationsImpl authValidations;

    private LoginPasswordService loginPasswordService;
    private LoginOtpInitiateService loginOtpInitiateService;
    private LoginOtpVerifyService loginOtpVerifyService;
    private RefreshTokenService refreshTokenService;

    private AuthFactory authFactory;

    @BeforeEach
    void setUp() {
        authValidations = new AuthValidationsImpl();

        loginPasswordService = new LoginPasswordService(
                authValidations,
                authenticationManager,
                userAccountRepository,
                jwtUtil
        );

        loginOtpInitiateService = new LoginOtpInitiateService(
                authValidations,
                userAccountRepository,
                otpService
        );

        loginOtpVerifyService = new LoginOtpVerifyService(
                authValidations,
                userAccountRepository,
                otpService,
                jwtUtil
        );

        refreshTokenService = new RefreshTokenService(
                authValidations,
                userAccountRepository,
                jwtUtil
        );

        List<AuthOperation> operations = List.of(
                loginPasswordService,
                loginOtpInitiateService,
                loginOtpVerifyService,
                refreshTokenService
        );

        authFactory = new AuthFactory(operations);
        authFactory.init();
    }

    @Test
    @DisplayName("AuthFactory correctly resolves all 4 login and refresh operations")
    void authFactory_ResolvesOperations() {
        assertThat(authFactory.getOperation(AuthRequestType.LOGIN_PASSWORD)).isSameAs(loginPasswordService);
        assertThat(authFactory.getOperation(AuthRequestType.LOGIN_OTP_INITIATE)).isSameAs(loginOtpInitiateService);
        assertThat(authFactory.getOperation(AuthRequestType.LOGIN_OTP_VERIFY)).isSameAs(loginOtpVerifyService);
        assertThat(authFactory.getOperation(AuthRequestType.REFRESH_TOKEN)).isSameAs(refreshTokenService);
    }

    @Test
    @DisplayName("LOGIN_PASSWORD succeeds with valid credentials and active account")
    void loginPassword_Success() {
        UserAccount account = UserAccount.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(jwtUtil.generateMasterAccessToken(account)).thenReturn("access.token.jwt");
        when(jwtUtil.generateMasterRefreshToken(account)).thenReturn("refresh.token.jwt");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_PASSWORD)
                .loginRequest(LoginRequest.builder()
                        .email("User@Example.com")
                        .password("ValidPass123!")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.LOGIN_PASSWORD).execute(request);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getAccessToken()).isEqualTo("access.token.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user@example.com", "ValidPass123!")
        );
    }

    @Test
    @DisplayName("LOGIN_PASSWORD throws BadCredentialsException when password fails authentication")
    void loginPassword_WhenBadCredentials_ThrowsBadCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_PASSWORD)
                .loginRequest(LoginRequest.builder()
                        .email("user@example.com")
                        .password("WrongPassword1!")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.LOGIN_PASSWORD).execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("LOGIN_PASSWORD throws BadRequestException when account is suspended")
    void loginPassword_WhenSuspended_ThrowsBadRequestException() {
        UserAccount account = UserAccount.builder()
                .id(2L)
                .email("suspended@example.com")
                .status(UserAccountStatus.SUSPENDED)
                .build();

        when(userAccountRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(account));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_PASSWORD)
                .loginRequest(LoginRequest.builder()
                        .email("suspended@example.com")
                        .password("ValidPass123!")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.LOGIN_PASSWORD).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is not active. Status: SUSPENDED");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("LOGIN_OTP_INITIATE sends OTP when account exists and is active")
    void loginOtpInitiate_Success() {
        UserAccount account = UserAccount.builder()
                .id(3L)
                .email("otpuser@example.com")
                .firstName("Sarah")
                .lastName("Connor")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findByEmail("otpuser@example.com")).thenReturn(Optional.of(account));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_OTP_INITIATE)
                .loginRequest(LoginRequest.builder()
                        .email("otpuser@example.com")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.LOGIN_OTP_INITIATE).execute(request);

        assertThat(response.getMessage()).isEqualTo("Login verification OTP sent to your email");
        verify(otpService).sendOtp("otpuser@example.com", OtpPurpose.LOGIN, "Sarah Connor");
    }

    @Test
    @DisplayName("LOGIN_OTP_INITIATE throws ResourceNotFoundException when email not found")
    void loginOtpInitiate_WhenNotFound_ThrowsResourceNotFoundException() {
        when(userAccountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_OTP_INITIATE)
                .loginRequest(LoginRequest.builder()
                        .email("nonexistent@example.com")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.LOGIN_OTP_INITIATE).execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No account registered");

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("LOGIN_OTP_VERIFY verifies OTP, consumes token, and returns JWT tokens")
    void loginOtpVerify_Success() {
        UserAccount account = UserAccount.builder()
                .id(4L)
                .email("verify@example.com")
                .firstName("Ellen")
                .lastName("Ripley")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findByEmail("verify@example.com")).thenReturn(Optional.of(account));
        when(otpService.verifyOtp("verify@example.com", "849201", OtpPurpose.LOGIN)).thenReturn(true);
        when(jwtUtil.generateMasterAccessToken(account)).thenReturn("otp.access.jwt");
        when(jwtUtil.generateMasterRefreshToken(account)).thenReturn("otp.refresh.jwt");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.LOGIN_OTP_VERIFY)
                .loginRequest(LoginRequest.builder()
                        .email("verify@example.com")
                        .otp("849201")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.LOGIN_OTP_VERIFY).execute(request);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getAccessToken()).isEqualTo("otp.access.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("otp.refresh.jwt");
        assertThat(response.getUser()).isNotNull();

        verify(otpService).consumeVerifiedOtp("verify@example.com", OtpPurpose.LOGIN);
    }

    @Test
    @DisplayName("REFRESH_TOKEN exchanges valid refresh token for fresh access and refresh token pair")
    void refreshToken_Success() {
        String token = "valid.refresh.token";
        when(jwtUtil.extractType(token)).thenReturn("REFRESH");
        when(jwtUtil.extractTokenType(token)).thenReturn("MASTER_ACCOUNT");
        when(jwtUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtUtil.extractSubject(token)).thenReturn("5");

        UserAccount account = UserAccount.builder()
                .id(5L)
                .email("renew@example.com")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(jwtUtil.generateMasterAccessToken(account)).thenReturn("new.access.token");
        when(jwtUtil.generateMasterRefreshToken(account)).thenReturn("new.refresh.token");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.REFRESH_TOKEN)
                .loginRequest(LoginRequest.builder()
                        .refreshToken(token)
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.REFRESH_TOKEN).execute(request);

        assertThat(response.getMessage()).isEqualTo("Token refreshed successfully");
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
    }

    @Test
    @DisplayName("REFRESH_TOKEN rejects ACCESS token passed in as refresh token")
    void refreshToken_WhenAccessTokenProvided_ThrowsBadCredentialsException() {
        String token = "valid.access.token";
        when(jwtUtil.extractType(token)).thenReturn("ACCESS");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.REFRESH_TOKEN)
                .loginRequest(LoginRequest.builder()
                        .refreshToken(token)
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.REFRESH_TOKEN).execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    @DisplayName("REFRESH_TOKEN rejects expired refresh token")
    void refreshToken_WhenExpired_ThrowsBadCredentialsException() {
        String token = "expired.refresh.token";
        when(jwtUtil.extractType(token)).thenReturn("REFRESH");
        when(jwtUtil.extractTokenType(token)).thenReturn("MASTER_ACCOUNT");
        when(jwtUtil.isTokenExpired(token)).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.REFRESH_TOKEN)
                .loginRequest(LoginRequest.builder()
                        .refreshToken(token)
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.REFRESH_TOKEN).execute(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token has expired");
    }
}
