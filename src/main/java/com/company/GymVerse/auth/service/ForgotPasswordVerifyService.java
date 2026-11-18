package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.ForgotPasswordRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Step 2 of Password Recovery: Validates and consumes the submitted OTP against Redis.
 * Upon successful match, establishes the verified state token in Redis for password reset.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordVerifyService implements AuthOperation {

    private final AuthValidations authValidations;
    private final OtpService otpService;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.FORGOT_PASSWORD_VERIFY;
    }

    @Override
    public AuthResponse execute(AuthRequest request) {
        ForgotPasswordRequest forgotRequest = request.getForgotPasswordRequest();
        authValidations.validateForgotPasswordVerify(forgotRequest);

        String normalizedEmail = forgotRequest.getEmail().trim().toLowerCase();

        // Verify OTP code against Redis (sets verified token state on success)
        otpService.verifyOtp(normalizedEmail, forgotRequest.getOtp(), OtpPurpose.FORGOT_PASSWORD);

        log.info("Password reset OTP successfully verified for email: [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("OTP verified successfully. Please submit your new password.")
                .build();
    }
}
