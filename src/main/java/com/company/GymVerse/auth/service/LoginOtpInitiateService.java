package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.LoginRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.otp.service.OtpService;
import com.company.GymVerse.otp.util.OtpPurpose;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the initiation of passwordless OTP login.
 * Verifies account existence and active status, then triggers asynchronous OTP dispatch via {@link OtpService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginOtpInitiateService implements AuthOperation {

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.LOGIN_OTP_INITIATE;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        LoginRequest loginRequest = request.getLoginRequest();
        authValidations.validateLoginOtpInitiate(loginRequest);

        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();

        // 1. Retrieve account from central repository
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + normalizedEmail));

        // 2. Status guard: only ACTIVE accounts may log in
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Login OTP initiate attempt for non-active account [{}], status: [{}]", normalizedEmail, account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 3. Dispatch OTP via OtpService
        String recipientName = account.getFirstName() + " " + account.getLastName();
        otpService.sendOtp(normalizedEmail, OtpPurpose.LOGIN, recipientName);

        log.info("Dispatched login verification OTP to [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("Login verification OTP sent to your email")
                .build();
    }
}
