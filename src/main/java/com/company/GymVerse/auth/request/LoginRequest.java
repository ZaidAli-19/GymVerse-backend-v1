package com.company.GymVerse.auth.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload carrier for dual-mode login operations (password login, OTP login initiate/verify, and token refresh).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    private String email;

    private String password;

    private String otp;

    private String refreshToken;
}
