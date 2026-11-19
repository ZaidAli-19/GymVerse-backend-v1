package com.company.GymVerse.auth.mapper;

import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.SwitchTenantResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import org.springframework.stereotype.Component;

/**
 * Handwritten implementation of {@link AuthMapper} avoiding runtime reflection or code-generation overhead.
 */
@Component
public class AuthMapperImpl implements AuthMapper {

    @Override
    public UserSummaryResponse toUserSummary(UserAccount account) {
        if (account == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .phoneNumber(account.getPhoneNumber())
                .status(account.getStatus())
                .build();
    }

    @Override
    public AuthResponse toAuthResponse(String message, String accessToken, String refreshToken, UserAccount account) {
        return AuthResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toUserSummary(account))
                .build();
    }

    @Override
    public AuthResponse toMessageResponse(String message) {
        return AuthResponse.builder()
                .message(message)
                .build();
    }

    @Override
    public SwitchTenantResponse toSwitchTenantResponse(
            final GymTenant gymTenant,
            final UserTenantRole userTenantRole,
            final String accessToken,
            final String refreshToken
    ) {
        if (gymTenant == null && userTenantRole == null) {
            return null;
        }
        return SwitchTenantResponse.builder()
                .tenantAccessToken(accessToken)
                .tenantRefreshToken(refreshToken)
                .tenantId(gymTenant != null ? gymTenant.getId() : null)
                .gymCode(gymTenant != null ? gymTenant.getGymCode() : null)
                .gymName(gymTenant != null ? gymTenant.getGymName() : null)
                .tenantDbName(gymTenant != null ? gymTenant.getDbName() : null)
                .role(userTenantRole != null ? userTenantRole.getRole() : null)
                .adminAuthority(userTenantRole != null ? userTenantRole.getAdminAuthority() : null)
                .build();
    }

    @Override
    public AuthResponse toSwitchTenantAuthResponse(
            final String message,
            final String accessToken,
            final String refreshToken,
            final SwitchTenantResponse switchTenantResponse
    ) {
        return AuthResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .switchTenantResponse(switchTenantResponse)
                .build();
    }
}
