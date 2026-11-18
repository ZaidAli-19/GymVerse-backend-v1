package com.company.GymVerse.auth.request;

import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias({"requestType", "authRequestType"})
    private AuthRequestType authRequestType;

    @JsonAlias({"signup", "signupRequest"})
    private SignupRequest signupRequest;

    @JsonAlias({"login", "loginRequest"})
    private LoginRequest loginRequest;

    @JsonAlias({"forgotPassword", "forgotPasswordRequest"})
    private ForgotPasswordRequest forgotPasswordRequest;

    public AuthRequestType getRequestType() {
        return authRequestType;
    }
}
