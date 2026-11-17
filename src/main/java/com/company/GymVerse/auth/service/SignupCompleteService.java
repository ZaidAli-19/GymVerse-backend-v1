package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 3 of Master User Signup: Verifies presence of the verified OTP token state in Redis,
 * encodes the password, provisions the central {@link UserAccount} entity, consumes the verified token,
 * and issues initial MASTER_ACCOUNT JWT access and refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupCompleteService implements AuthOperation {

    private static final String PENDING_SIGNUP_KEY_PREFIX = "gymverse:signup:pending:";

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.SIGNUP_COMPLETE;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        SignupRequest signupRequest = request.getSignupRequest();
        authValidations.validateSignupComplete(signupRequest);

        String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

        // 1. Verify OTP token state in Redis
        if (!otpService.isOtpVerified(normalizedEmail, OtpPurpose.SIGNUP)) {
            log.warn("Attempt to complete signup without verified email for: [{}]", normalizedEmail);
            throw new BadRequestException("Email has not been verified. Please verify your email first.");
        }

        // 2. Prevent race conditions on duplicate email
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            log.warn("Duplicate account creation attempt for email: [{}]", normalizedEmail);
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        // 3. Resolve user profile details (from request or cached pending registration)
        ProfileDetails profile = resolveProfileDetails(normalizedEmail, signupRequest);

        // 4. Provision UserAccount entity in central database
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        UserAccount account = UserAccount.builder()
                .email(normalizedEmail)
                .firstName(profile.firstName)
                .lastName(profile.lastName)
                .phoneNumber(profile.phoneNumber)
                .password(encodedPassword)
                .emailVerified(true)
                .isPlatformAdmin(false) // Security invariant: public signup never grants admin privileges
                .status(UserAccountStatus.ACTIVE)
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);
        log.info("Provisioned new UserAccount with ID [{}] for email: [{}]", savedAccount.getId(), normalizedEmail);

        // 5. Invalidate verified OTP token and pending cache in Redis
        otpService.consumeVerifiedOtp(normalizedEmail, OtpPurpose.SIGNUP);
        redisTemplate.delete(PENDING_SIGNUP_KEY_PREFIX + normalizedEmail);

        // 6. Issue initial Master JWT tokens
        String accessToken = jwtUtil.generateMasterAccessToken(savedAccount);
        String refreshToken = jwtUtil.generateMasterRefreshToken(savedAccount);

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(savedAccount.getId())
                .email(savedAccount.getEmail())
                .firstName(savedAccount.getFirstName())
                .lastName(savedAccount.getLastName())
                .phoneNumber(savedAccount.getPhoneNumber())
                .status(savedAccount.getStatus())
                .build();

        return AuthResponse.builder()
                .message("Account registration completed successfully")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userSummary)
                .build();
    }

    private ProfileDetails resolveProfileDetails(String email, SignupRequest request) {
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        String phoneNumber = request.getPhoneNumber();

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            String cachedJson = redisTemplate.opsForValue().get(PENDING_SIGNUP_KEY_PREFIX + email);
            if (cachedJson != null && !cachedJson.isBlank()) {
                try {
                    SignupRequest cached = objectMapper.readValue(cachedJson, SignupRequest.class);
                    if (firstName == null || firstName.isBlank()) {
                        firstName = cached.getFirstName();
                    }
                    if (lastName == null || lastName.isBlank()) {
                        lastName = cached.getLastName();
                    }
                    if (phoneNumber == null || phoneNumber.isBlank()) {
                        phoneNumber = cached.getPhoneNumber();
                    }
                } catch (Exception e) {
                    log.error("Failed to parse cached signup profile for email [{}]: {}", email, e.getMessage());
                }
            }
        }

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new BadRequestException("First name and last name are required to complete registration");
        }

        return new ProfileDetails(firstName.trim(), lastName.trim(), phoneNumber != null ? phoneNumber.trim() : null);
    }

    private record ProfileDetails(String firstName, String lastName, String phoneNumber) {}
}
