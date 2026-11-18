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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 3 of Password Recovery: Verifies active verified token state in Redis, validates new password complexity,
 * updates the encrypted password in the central database under {@code centralTransactionManager},
 * and invalidates the verified token state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordResetService implements AuthOperation {

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.FORGOT_PASSWORD_RESET;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        ForgotPasswordRequest forgotRequest = request.getForgotPasswordRequest();
        authValidations.validateForgotPasswordReset(forgotRequest);

        String normalizedEmail = forgotRequest.getEmail().trim().toLowerCase();

        // 1. Guard check: verifies verified OTP state token in Redis
        if (!otpService.isOtpVerified(normalizedEmail, OtpPurpose.FORGOT_PASSWORD)) {
            log.warn("Password reset attempted without verified OTP for email: [{}]", normalizedEmail);
            throw new BadRequestException("Password reset authorization missing or expired. Please verify OTP first.");
        }

        // 2. Load account entity from central database
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + normalizedEmail));

        // 3. Status guard: account must be ACTIVE
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Password reset attempted for non-active account [{}], status: [{}]", normalizedEmail, account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 4. Encode and persist new password
        String encodedPassword = passwordEncoder.encode(forgotRequest.getNewPassword());
        account.setPassword(encodedPassword);
        userAccountRepository.save(account);

        // 5. Invalidate verified OTP token state in Redis
        otpService.consumeVerifiedOtp(normalizedEmail, OtpPurpose.FORGOT_PASSWORD);

        log.info("Password reset completed successfully for email: [{}]", normalizedEmail);
        return AuthResponse.builder()
                .message("Password reset successfully. Please login with your new credentials.")
                .build();
    }
}
