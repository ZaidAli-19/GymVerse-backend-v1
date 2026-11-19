package com.company.GymVerse.user.factory;

import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.UserResponse;

/**
 * Functional contract for individual master user operation handlers
 * registered with {@link UserFactory}.
 */
public interface UserOperation {

    /**
     * Executes the specific user operation command/query.
     *
     * @param request the unified user request envelope
     * @return the unified user response envelope
     */
    UserResponse execute(UserRequest request);

    /**
     * Identifies the request type discriminator handled by this bean.
     *
     * @return the {@link UserRequestType} enum
     */
    UserRequestType getRequestType();
}
