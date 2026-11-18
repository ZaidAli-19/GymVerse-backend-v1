package com.company.GymVerse.auth.mapper;

import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.models.central.UserAccount;

/**
 * Handwritten mapper contract transforming UserAccount entities into Auth responses.
 */
public interface AuthMapper {

    UserSummaryResponse toUserSummary(UserAccount account);

    AuthResponse toAuthResponse(String message, String accessToken, String refreshToken, UserAccount account);

    AuthResponse toMessageResponse(String message);
}
