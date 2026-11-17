package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Step 2 of Master User Signup: Validates and consumes the submitted OTP against Redis.
 * Upon successful match, establishes the verified state token in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupVerifyService implements AuthOperation {

    private final AuthValidations authValidations;
    private final OtpService otpService;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.SIGNUP_VERIFY;
    }

    @Override
    public AuthResponse execute(AuthRequest request) {
        SignupRequest signupRequest = request.getSignupRequest();
        authValidations.validateSignupVerify(signupRequest);

        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();
        otpService.verifyOtp(normalizedEmail, signupRequest.getOtp(), OtpPurpose.SIGNUP);

        log.info("Email verification succeeded for signup: [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("Email successfully verified. Please submit your password to complete registration.")
                .build();
    }
}
