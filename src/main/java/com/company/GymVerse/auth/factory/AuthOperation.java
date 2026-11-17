package com.company.GymVerse.auth.factory;

import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;

/**
 * Service contract for individual operation beans in the unified Auth module.
 */
public interface AuthOperation {

    /**
     * @return the discriminator request type this bean handles
     */
    AuthRequestType getRequestType();

    /**
     * Executes the concrete authentication operation.
     *
     * @param request the unified auth request envelope
     * @return the unified auth response envelope
     */
    AuthResponse execute(AuthRequest request);
}
