package com.company.GymVerse.auth;

import com.company.GymVerse.auth.factory.AuthFactory;
import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.ForgotPasswordRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.service.ForgotPasswordInitiateService;
import com.company.GymVerse.auth.service.ForgotPasswordResetService;
import com.company.GymVerse.auth.service.ForgotPasswordVerifyService;
import com.company.GymVerse.auth.validations.AuthValidationsImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordLifecycleIntegrationTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthValidationsImpl authValidations;

    private ForgotPasswordInitiateService forgotPasswordInitiateService;
    private ForgotPasswordVerifyService forgotPasswordVerifyService;
    private ForgotPasswordResetService forgotPasswordResetService;

    private AuthFactory authFactory;

    @BeforeEach
    void setUp() {
        authValidations = new AuthValidationsImpl();

        forgotPasswordInitiateService = new ForgotPasswordInitiateService(
                authValidations,
                userAccountRepository,
                otpService
        );

        forgotPasswordVerifyService = new ForgotPasswordVerifyService(
                authValidations,
                otpService
        );

        forgotPasswordResetService = new ForgotPasswordResetService(
                authValidations,
                userAccountRepository,
                otpService,
                passwordEncoder
        );

        List<AuthOperation> operations = List.of(
                forgotPasswordInitiateService,
                forgotPasswordVerifyService,
                forgotPasswordResetService
        );

        authFactory = new AuthFactory(operations);
        authFactory.init();
    }

    @Test
    @DisplayName("AuthFactory correctly resolves all 3 password recovery operations")
    void authFactory_ResolvesOperations() {
        assertThat(authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_INITIATE)).isSameAs(forgotPasswordInitiateService);
        assertThat(authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_VERIFY)).isSameAs(forgotPasswordVerifyService);
        assertThat(authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_RESET)).isSameAs(forgotPasswordResetService);
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_INITIATE throws ResourceNotFoundException when email not registered")
    void initiate_WhenEmailNotFound_ThrowsResourceNotFoundException() {
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_INITIATE)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("unknown@example.com")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_INITIATE).execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No account registered");

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_INITIATE throws BadRequestException when account is suspended")
    void initiate_WhenAccountSuspended_ThrowsBadRequestException() {
        UserAccount account = UserAccount.builder()
                .id(1L)
                .email("locked@example.com")
                .status(UserAccountStatus.SUSPENDED)
                .build();

        when(userAccountRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(account));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_INITIATE)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("locked@example.com")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_INITIATE).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is not active. Status: SUSPENDED");

        verifyNoInteractions(otpService);
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_INITIATE dispatches OTP for registered active user")
    void initiate_Success() {
        UserAccount account = UserAccount.builder()
                .id(2L)
                .email("active@example.com")
                .firstName("Tony")
                .lastName("Stark")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findByEmail("active@example.com")).thenReturn(Optional.of(account));

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_INITIATE)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("active@example.com")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_INITIATE).execute(request);

        assertThat(response.getMessage()).isEqualTo("Password reset OTP has been sent to your email");
        verify(otpService).sendOtp("active@example.com", OtpPurpose.FORGOT_PASSWORD, "Tony Stark");
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_VERIFY throws OtpInvalidException on wrong code")
    void verify_WhenOtpInvalid_PropagatesException() {
        doThrow(new OtpInvalidException("Invalid OTP code. 4 attempts remaining"))
                .when(otpService).verifyOtp("user@example.com", "000000", OtpPurpose.FORGOT_PASSWORD);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_VERIFY)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("user@example.com")
                        .otp("000000")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_VERIFY).execute(request))
                .isInstanceOf(OtpInvalidException.class)
                .hasMessageContaining("Invalid OTP code");
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_VERIFY returns confirmation message on valid OTP")
    void verify_Success() {
        when(otpService.verifyOtp("user@example.com", "654321", OtpPurpose.FORGOT_PASSWORD)).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_VERIFY)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("user@example.com")
                        .otp("654321")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_VERIFY).execute(request);

        assertThat(response.getMessage()).contains("OTP verified successfully");
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_RESET throws BadRequestException if OTP was never verified")
    void reset_WhenNotVerified_ThrowsBadRequestException() {
        when(otpService.isOtpVerified("unverified@example.com", OtpPurpose.FORGOT_PASSWORD)).thenReturn(false);

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_RESET)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("unverified@example.com")
                        .newPassword("BrandNewP@ss1")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_RESET).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password reset authorization missing or expired");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_RESET throws BadRequestException when new password is weak")
    void reset_WhenPasswordWeak_ThrowsBadRequestException() {
        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_RESET)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("user@example.com")
                        .newPassword("weak")
                        .build())
                .build();

        assertThatThrownBy(() -> authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_RESET).execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    @DisplayName("FORGOT_PASSWORD_RESET updates password in repository and consumes verified state")
    void reset_Success() {
        UserAccount account = UserAccount.builder()
                .id(3L)
                .email("tony@example.com")
                .password("$2a$12$oldHashedPassword")
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(otpService.isOtpVerified("tony@example.com", OtpPurpose.FORGOT_PASSWORD)).thenReturn(true);
        when(userAccountRepository.findByEmail("tony@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("StarkTech#2026!")).thenReturn("$2a$12$newEncryptedPassword");

        AuthRequest request = AuthRequest.builder()
                .authRequestType(AuthRequestType.FORGOT_PASSWORD_RESET)
                .forgotPasswordRequest(ForgotPasswordRequest.builder()
                        .email("tony@example.com")
                        .newPassword("StarkTech#2026!")
                        .build())
                .build();

        AuthResponse response = authFactory.getOperation(AuthRequestType.FORGOT_PASSWORD_RESET).execute(request);

        assertThat(response.getMessage()).isEqualTo("Password reset successfully. Please login with your new credentials.");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$12$newEncryptedPassword");

        verify(otpService).consumeVerifiedOtp("tony@example.com", OtpPurpose.FORGOT_PASSWORD);
    }
}
