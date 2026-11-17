package com.company.GymVerse.auth;

import com.company.GymVerse.auth.factory.AuthFactory;
import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.service.SignupCompleteService;
import com.company.GymVerse.auth.service.SignupInitiateService;
import com.company.GymVerse.auth.service.SignupVerifyService;
import com.company.GymVerse.auth.validations.AuthValidationsImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupLifecycleIntegrationTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private AuthValidationsImpl authValidations;

    private SignupInitiateService signupInitiateService;
    private SignupVerifyService signupVerifyService;
    private SignupCompleteService signupCompleteService;

    private AuthFactory authFactory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authValidations = new AuthValidationsImpl();

        signupInitiateService = new SignupInitiateService(
                authValidations,
                userAccountRepository,
                otpService,
                redisTemplate,
                objectMapper
        );

        signupVerifyService = new SignupVerifyService(
                authValidations,
                otpService
        );

        signupCompleteService = new SignupCompleteService(
                authValidations,
                userAccountRepository,
                otpService,
                passwordEncoder,
                jwtUtil,
                redisTemplate,
                objectMapper
        );

        List<AuthOperation> operations = List.of(
                signupInitiateService,
                signupVerifyService,
                signupCompleteService
        );
        authFactory = new AuthFactory(operations);
        authFactory.init();
    }

    @Test
    @DisplayName("AuthFactory correctly registers all three signup operations")
    void authFactory_RegistersOperations() {
        assertThat(authFactory.getOperation(AuthRequestType.SIGNUP_INITIATE)).isSameAs(signupInitiateService);
        assertThat(authFactory.getOperation(AuthRequestType.SIGNUP_VERIFY)).isSameAs(signupVerifyService);
        assertThat(authFactory.getOperation(AuthRequestType.SIGNUP_COMPLETE)).isSameAs(signupCompleteService);
    }

    @Test
    @DisplayName("Step 1: SIGNUP_INITIATE throws ConflictException when email is already registered")
    void signupInitiate_WhenEmailExists_ThrowsConflictException() {
        when(userAccountRepository.existsByEmail("existing@example.com")).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_INITIATE)
                .signupRequest(SignupRequest.builder()
                        .email("Existing@Example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("+1234567890")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.SIGNUP_INITIATE).execute(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("Step 1: SIGNUP_INITIATE sends OTP and caches profile when email is available")
    void signupInitiate_WhenEmailAvailable_SendsOtpAndCachesProfile() {
        when(userAccountRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_INITIATE)
                .signupRequest(SignupRequest.builder()
                        .email("NewUser@Example.com")
                        .firstName("Bruce")
                        .lastName("Wayne")
                        .phoneNumber("+1987654321")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.SIGNUP_INITIATE).execute(request);

        assertThat(response.getMessage()).isEqualTo("Verification OTP has been sent to your email");
        verify(otpService).sendOtp("newuser@example.com", OtpPurpose.SIGNUP, "Bruce Wayne");
        verify(valueOperations).set(eq("gymverse:signup:pending:newuser@example.com"), anyString(), any());
    }

    @Test
    @DisplayName("Step 2: SIGNUP_VERIFY propagates OtpInvalidException on invalid OTP")
    void signupVerify_WhenOtpInvalid_PropagatesException() {
        doThrow(new OtpInvalidException("Invalid OTP code. 4 attempts remaining"))
                .when(otpService).verifyOtp("user@example.com", "000000", OtpPurpose.SIGNUP);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_VERIFY)
                .signupRequest(SignupRequest.builder()
                        .email("user@example.com")
                        .otp("000000")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.SIGNUP_VERIFY).execute(request))
                .isInstanceOf(OtpInvalidException.class)
                .hasMessageContaining("Invalid OTP code");
    }

    @Test
    @DisplayName("Step 2: SIGNUP_VERIFY returns confirmation message on valid OTP")
    void signupVerify_WhenOtpValid_ReturnsSuccess() {
        when(otpService.verifyOtp("user@example.com", "123456", OtpPurpose.SIGNUP)).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_VERIFY)
                .signupRequest(SignupRequest.builder()
                        .email("user@example.com")
                        .otp("123456")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.SIGNUP_VERIFY).execute(request);

        assertThat(response.getMessage()).contains("Email successfully verified");
    }

    @Test
    @DisplayName("Step 3: SIGNUP_COMPLETE throws BadRequestException when email has not been verified")
    void signupComplete_WhenNotVerified_ThrowsBadRequestException() {
        when(otpService.isOtpVerified("unverified@example.com", OtpPurpose.SIGNUP)).thenReturn(false);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_COMPLETE)
                .signupRequest(SignupRequest.builder()
                        .email("unverified@example.com")
                        .password("SecureP@ss1")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.SIGNUP_COMPLETE).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email has not been verified");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Step 3: SIGNUP_COMPLETE throws BadRequestException when password is weak")
    void signupComplete_WhenPasswordWeak_ThrowsBadRequestException() {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_COMPLETE)
                .signupRequest(SignupRequest.builder()
                        .email("verified@example.com")
                        .password("weak")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.SIGNUP_COMPLETE).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    @DisplayName("Step 3: SIGNUP_COMPLETE provisions user, consumes verified state, and returns JWT tokens")
    void signupComplete_WhenVerifiedAndPasswordValid_ProvisionsUserAndIssuesTokens() {
        when(otpService.isOtpVerified("clark@example.com", OtpPurpose.SIGNUP)).thenReturn(true);
        when(userAccountRepository.existsByEmail("clark@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Krypt0n!Secret")).thenReturn("$2a$12$hashedPassword");

        UserAccount mockSavedAccount = UserAccount.builder()
                .id(101L)
                .email("clark@example.com")
                .firstName("Clark")
                .lastName("Kent")
                .phoneNumber("+1122334455")
                .password("$2a$12$hashedPassword")
                .emailVerified(true)
                .isPlatformAdmin(false)
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(mockSavedAccount);
        when(jwtUtil.generateMasterAccessToken(mockSavedAccount)).thenReturn("mock.access.token");
        when(jwtUtil.generateMasterRefreshToken(mockSavedAccount)).thenReturn("mock.refresh.token");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.SIGNUP_COMPLETE)
                .signupRequest(SignupRequest.builder()
                        .email("Clark@Example.com")
                        .firstName("Clark")
                        .lastName("Kent")
                        .phoneNumber("+1122334455")
                        .password("Krypt0n!Secret")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.SIGNUP_COMPLETE).execute(request);

        assertThat(response.getMessage()).isEqualTo("Account registration completed successfully");
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(101L);
        assertThat(response.getUser().getEmail()).isEqualTo("clark@example.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Clark");
        assertThat(response.getUser().getStatus()).isEqualTo(UserAccountStatus.ACTIVE);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        UserAccount persisted = accountCaptor.getValue();
        assertThat(persisted.isPlatformAdmin()).isFalse();
        assertThat(persisted.isEmailVerified()).isTrue();
        assertThat(persisted.getPassword()).isEqualTo("$2a$12$hashedPassword");

        verify(otpService).consumeVerifiedOtp("clark@example.com", OtpPurpose.SIGNUP);
        verify(redisTemplate).delete("gymverse:signup:pending:clark@example.com");
    }
}
