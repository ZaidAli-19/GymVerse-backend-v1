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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles JWT token renewal via refresh tokens.
 * Validates cryptographic signature, expiry, MASTER_ACCOUNT typing, and REFRESH claim,
 * ensuring non-active accounts are blocked, before issuing fresh token pairs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService implements AuthOperation {

    private final AuthValidations authValidations;
    private final UserAccountRepository userAccountRepository;
    private final JwtUtil jwtUtil;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.REFRESH_TOKEN;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(AuthRequest request) {
        LoginRequest loginRequest = request.getLoginRequest();
        authValidations.validateRefreshToken(loginRequest);

        String rawRefreshToken = loginRequest.getRefreshToken().trim();

        // 1. Verify token type and claims
        String type = jwtUtil.extractType(rawRefreshToken);
        if (!"REFRESH".equalsIgnoreCase(type)) {
            log.warn("Token refresh rejected: token type is [{}] instead of REFRESH", type);
            throw new BadCredentialsException("Invalid token: provided token is not a refresh token");
        }

        String tokenType = jwtUtil.extractTokenType(rawRefreshToken);
        if (!"MASTER_ACCOUNT".equalsIgnoreCase(tokenType)) {
            log.warn("Token refresh rejected: tokenType is [{}] instead of MASTER_ACCOUNT", tokenType);
            throw new BadCredentialsException("Invalid token: token is not a master account token");
        }

        if (jwtUtil.isTokenExpired(rawRefreshToken)) {
            log.warn("Token refresh rejected: token has expired");
            throw new BadCredentialsException("Refresh token has expired");
        }

        // 2. Extract subject (user account ID)
        String subject = jwtUtil.extractSubject(rawRefreshToken);
        Long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.warn("Token refresh rejected: malformed subject [{}]", subject);
            throw new BadCredentialsException("Invalid token subject format");
        }

        // 3. Load account entity from central database
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with ID " + userId));

        // 4. Status guard: only ACTIVE accounts may refresh tokens
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            log.warn("Token refresh rejected: account [{}] has status [{}]", account.getEmail(), account.getStatus());
            throw new BadRequestException("Account is not active. Status: " + account.getStatus());
        }

        // 5. Issue fresh MASTER_ACCOUNT tokens
        String newAccessToken = jwtUtil.generateMasterAccessToken(account);
        String newRefreshToken = jwtUtil.generateMasterRefreshToken(account);

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .phoneNumber(account.getPhoneNumber())
                .status(account.getStatus())
                .build();

        log.info("Successfully refreshed tokens for user [{}]", account.getEmail());
        return AuthResponse.builder()
                .message("Token refreshed successfully")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userSummary)
                .build();
    }
}
