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
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles password-based authentication for central Master DB user accounts.
 * Authenticates credentials via {@link AuthenticationManager}, checks account status,
 * and issues MASTER_ACCOUNT JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginPasswordService implements AuthOperation {

    private final AuthValidations authValidations;
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtUtil jwtUtil;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.LOGIN_PASSWORD;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        LoginRequest loginRequest = request.getLoginRequest();
        authValidations.validateLoginPassword(loginRequest);

        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();

        // 1. Authenticate credentials via Spring Security AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword())
        );

        // 2. Retrieve central UserAccount entity
        UserAccount account = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + normalizedEmail));

        // 3. Status guard: only ACTIVE accounts may log in
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Login attempt for non-active account [{}], status: [{}]", normalizedEmail, account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 4. Issue MASTER_ACCOUNT JWT tokens
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

        log.info("User [{}] successfully logged in via password", normalizedEmail);
        return AuthResponse.builder()
                .message("Login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userSummary)
                .build();
    }
}
