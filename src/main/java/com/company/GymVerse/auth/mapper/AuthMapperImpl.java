package com.company.GymVerse.auth.mapper;

import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.auth.response.UserSummaryResponse;
import com.company.GymVerse.models.central.UserAccount;
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
}
