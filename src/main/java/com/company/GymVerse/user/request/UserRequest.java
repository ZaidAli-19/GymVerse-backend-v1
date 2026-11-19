package com.company.GymVerse.user.request;

import com.company.GymVerse.user.request.enums.UserRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request envelope for all central master user operations.
 * Exclusively driven by the {@link UserRequestType} discriminator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private UserRequestType requestType;
    private RegisterGymRequest registerGymRequest;
}
