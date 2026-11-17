package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Step 1 of Master User Signup: Validates basic profile information, ensures email uniqueness,
 * caches pending profile metadata, and triggers asynchronous OTP generation and email delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupInitiateService implements AuthOperation {

    private static final String PENDING_SIGNUP_KEY_PREFIX = "gymverse:signup:pending:";

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.SIGNUP_INITIATE;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        SignupRequest signupRequest = request.getSignupRequest();
        authValidations.validateSignupInitiate(signupRequest);

        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            log.warn("Signup initiated for existing email: [{}]", normalizedEmail);
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        String recipientName = signupRequest.getFirstName().trim() + " " + signupRequest.getLastName().trim();
        otpService.sendOtp(normalizedEmail, OtpPurpose.SIGNUP, recipientName);

        cachePendingProfile(normalizedEmail, signupRequest);

        log.info("Signup initiated successfully for email: [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("Verification OTP has been sent to your email")
                .build();
    }

    private void cachePendingProfile(String email, SignupRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForValue().set(
                    PENDING_SIGNUP_KEY_PREFIX + email,
                    json,
                    Duration.ofMinutes(15)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize pending signup profile for email: [{}]: {}", email, e.getMessage(), e);
        }
    }
}
