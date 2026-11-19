package com.company.GymVerse.auth.mapper;

import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.SwitchTenantResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;

/**
 * Handwritten mapper contract transforming central entities into Auth responses.
 */
public interface AuthMapper {

    UserSummaryResponse toUserSummary(UserAccount account);

    AuthResponse toAuthResponse(String message, String accessToken, String refreshToken, UserAccount account);

    AuthResponse toMessageResponse(String message);

    SwitchTenantResponse toSwitchTenantResponse(GymTenant gymTenant, UserTenantRole userTenantRole, String accessToken, String refreshToken);

    AuthResponse toSwitchTenantAuthResponse(String message, String accessToken, String refreshToken, SwitchTenantResponse switchTenantResponse);
}
