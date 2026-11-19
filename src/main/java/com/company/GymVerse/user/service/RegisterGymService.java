package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import com.company.GymVerse.user.dto.TenantProvisioningResult;
import com.company.GymVerse.user.factory.UserOperation;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.RegisterGymResponse;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.validations.UserValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Operation handler for REGISTER_GYM requests.
 * Orchestrates domain validations, dynamic tenant provisioning, and immediate tenant-scoped JWT token issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterGymService implements UserOperation {

    private final TenantProvisioningService tenantProvisioningService;
    private final UserValidations userValidations;
    private final UserMapper userMapper;
    private final UserAccountRepository userAccountRepository;
    private final JwtUtil jwtUtil;

    @Override
    public UserRequestType getRequestType() {
        return UserRequestType.REGISTER_GYM;
    }

    @Override
    public UserResponse execute(final UserRequest request) {
        log.info("Processing REGISTER_GYM operation for gym registration");

        // 1. Validate request payload domain invariants
        userValidations.validateRegisterGym(request);

        // 2. Resolve authenticated master caller identity
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BadCredentialsException("Full authentication is required to access this resource");
        }

        String principal = authentication.getName();
        UserAccount callerAccount;
        try {
            Long userId = Long.parseLong(principal);
            callerAccount = userAccountRepository.findById(userId)
                    .or(() -> userAccountRepository.findByEmail(principal))
                    .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found for authenticated ID: " + userId));
        } catch (NumberFormatException e) {
            callerAccount = userAccountRepository.findByEmail(principal)
                    .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found for authenticated email: " + principal));
        }

        // 3. Transform request to provisioning command
        TenantProvisioningCommand command = userMapper.toProvisioningCommand(
                request.getRegisterGymRequest(),
                callerAccount
        );

        // 4. Orchestrate dynamic database creation, migration, seed, and trial enrollment
        TenantProvisioningResult provisioningResult = tenantProvisioningService.provisionTenant(command);

        // 5. Issue tenant-scoped access & refresh tokens
        TenantTokenClaims tokenClaims = TenantTokenClaims.builder()
                .userId(callerAccount.getId())
                .email(callerAccount.getEmail())
                .tenantId(provisioningResult.getGymTenant().getId())
                .gymCode(provisioningResult.getGymCode())
                .tenantDbName(provisioningResult.getDbName())
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();

        String tenantAccessToken = jwtUtil.generateTenantAccessToken(tokenClaims);
        String tenantRefreshToken = jwtUtil.generateTenantRefreshToken(tokenClaims);

        log.info("Successfully registered gym '{}' [gymCode='{}'] and issued tenant tokens for owner '{}'",
                provisioningResult.getGymTenant().getGymName(),
                provisioningResult.getGymCode(),
                callerAccount.getEmail());

        // 6. Map and return response envelope
        RegisterGymResponse registerGymResponse = userMapper.toRegisterGymResponse(
                provisioningResult,
                tenantAccessToken,
                tenantRefreshToken
        );

        return userMapper.toUserResponse(
                "Gym registered successfully and 14-day free trial activated",
                registerGymResponse
        );
    }
}
