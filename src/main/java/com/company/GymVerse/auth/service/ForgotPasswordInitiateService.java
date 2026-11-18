package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.ForgotPasswordRequest;
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
 * Step 1 of Password Recovery: Validates email, verifies active account status,
 * and triggers asynchronous OTP dispatch via {@link OtpService} under the FORGOT_PASSWORD purpose.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordInitiateService implements AuthOperation {

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.FORGOT_PASSWORD_INITIATE;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        ForgotPasswordRequest forgotRequest = request.getForgotPasswordRequest();
        authValidations.validateForgotPasswordInitiate(forgotRequest);

        String normalizedEmail = forgotRequest.getEmail().trim().toLowerCase();

        // 1. Retrieve account from central repository
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + normalizedEmail));

        // 2. Status guard: only ACTIVE accounts may reset password
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Password reset initiated for non-active account [{}], status: [{}]", normalizedEmail, account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 3. Dispatch OTP via OtpService
        String recipientName = account.getFirstName() + " " + account.getLastName();
        otpService.sendOtp(normalizedEmail, OtpPurpose.FORGOT_PASSWORD, recipientName);

        log.info("Dispatched password reset OTP to email: [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("Password reset OTP has been sent to your email")
                .build();
    }
}
