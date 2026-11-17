package com.company.GymVerse.auth.request;

import com.company.GymVerse.auth.request.enums.AuthRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified request envelope for all authentication operations matching the module factory gateway standard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {

    private AuthRequestType authRequestType;

    private SignupRequest signupRequest;

    private LoginRequest loginRequest;
}
