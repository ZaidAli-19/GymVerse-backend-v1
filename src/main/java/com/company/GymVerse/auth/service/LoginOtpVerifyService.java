package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.LoginRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles verification of one-time codes for passwordless login.
 * Verifies submitted OTP against Redis, consumes the verified token, and issues MASTER_ACCOUNT JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginOtpVerifyService implements AuthOperation {

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.LOGIN_OTP_VERIFY;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        LoginRequest loginRequest = request.getLoginRequest();
        authValidations.validateLoginOtpVerify(loginRequest);

        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();

        // 1. Retrieve account from central repository
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + normalizedEmail));

        // 2. Status guard: only ACTIVE accounts may log in
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Login OTP verify attempt for non-active account [{}], status: [{}]", normalizedEmail, account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 3. Verify OTP via OtpService
        otpService.verifyOtp(normalizedEmail, loginRequest.getOtp(), OtpPurpose.LOGIN);

        // 4. Invalidate verified token state
        otpService.consumeVerifiedOtp(normalizedEmail, OtpPurpose.LOGIN);

        // 5. Issue MASTER_ACCOUNT JWT tokens
        String accessToken = jwtUtil.generateMasterAccessToken(account);
        String refreshToken = jwtUtil.generateMasterRefreshToken(account);

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .phoneNumber(account.getPhoneNumber())
                .status(account.getStatus())
                .build();

        log.info("User [{}] successfully authenticated via OTP login", normalizedEmail);
        return AuthResponse.builder()
                .message("Login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userSummary)
                .build();
    }
}
