package com.company.GymVerse.auth.service;

import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.mapper.AuthMapper;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.SwitchTenantRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.SwitchTenantResponse;
import com.company.GymVerse.auth.validations.AuthValidations;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operation handler for SWITCH_TENANT requests.
 * Acts as an authenticated token exchange airlock bridge trading a valid MASTER_ACCOUNT identity
 * for a scoped TENANT_ACCOUNT access and refresh token pair bound to an isolated tenant database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwitchTenantService implements AuthOperation {

    private final UserAccountRepository userAccountRepository;
    private final GymTenantRepository gymTenantRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;
    private final AuthValidations authValidations;

    @Override
    public AuthRequestType getRequestType() {
        return AuthRequestType.SWITCH_TENANT;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public AuthResponse execute(final AuthRequest request) {
        log.info("Processing SWITCH_TENANT token exchange operation");

        authValidations.validateMasterContext();
        SwitchTenantRequest switchReq = request != null ? request.getSwitchTenantRequest() : null;
        authValidations.validateSwitchTenantRequest(switchReq);

        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        UserAccount callerAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found for authenticated ID: " + userId));

        GymTenant gymTenant = resolveGymTenant(switchReq);
        authValidations.validateGymActiveStatus(gymTenant);

        List<UserTenantRole> userRoles = userTenantRoleRepository.findAllByUserAccountIdAndGymTenantId(callerAccount.getId(), gymTenant.getId());
        UserTenantRole userTenantRole = authValidations.resolveUserTenantRole(userRoles, switchReq.getRole(), gymTenant.getGymName());
        authValidations.validateMembershipActiveStatus(userTenantRole);

        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(callerAccount.getId())
                .email(callerAccount.getEmail())
                .tenantId(gymTenant.getId())
                .gymCode(gymTenant.getGymCode())
                .tenantDbName(gymTenant.getDbName())
                .role(userTenantRole.getRole())
                .adminAuthority(userTenantRole.getAdminAuthority())
                .build();

        String tenantAccessToken = jwtUtil.generateTenantAccessToken(claims);
        String tenantRefreshToken = jwtUtil.generateTenantRefreshToken(claims);

        SwitchTenantResponse switchResponse = authMapper.toSwitchTenantResponse(gymTenant, userTenantRole, tenantAccessToken, tenantRefreshToken);

        return authMapper.toSwitchTenantAuthResponse(
                "Tenant switch successful. Scoped tokens issued for gym: " + gymTenant.getGymName(),
                tenantAccessToken,
                tenantRefreshToken,
                switchResponse
        );
    }

    private GymTenant resolveGymTenant(final SwitchTenantRequest switchReq) {
        if (switchReq.getGymTenantId() != null) {
            GymTenant gymTenant = gymTenantRepository.findById(switchReq.getGymTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gym tenant not found with ID: " + switchReq.getGymTenantId()));

            if (switchReq.getGymCode() != null && !switchReq.getGymCode().isBlank()) {
                authValidations.validateGymTargetConsistency(gymTenant, switchReq.getGymCode());
            }
            return gymTenant;
        }

        String code = switchReq.getGymCode().trim().toUpperCase();
        return gymTenantRepository.findByGymCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Gym tenant not found with code: " + code));
    }
}
