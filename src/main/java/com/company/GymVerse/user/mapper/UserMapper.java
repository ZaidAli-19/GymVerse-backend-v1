package com.company.GymVerse.user.mapper;

import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import com.company.GymVerse.user.dto.TenantProvisioningResult;
import com.company.GymVerse.user.request.RegisterGymRequest;
import com.company.GymVerse.user.response.RegisterGymResponse;
import com.company.GymVerse.user.response.UserResponse;

/**
 * Handwritten mapper contract for transformation between central user DTOs and internal domain models.
 */
public interface UserMapper {

    /**
     * Converts a {@link RegisterGymRequest} and caller {@link UserAccount} into a {@link TenantProvisioningCommand}.
     *
     * @param request      the gym registration payload
     * @param ownerAccount the authenticated user account of the gym creator
     * @return populated {@link TenantProvisioningCommand}
     */
    TenantProvisioningCommand toProvisioningCommand(RegisterGymRequest request, UserAccount ownerAccount);

    /**
     * Converts a {@link TenantProvisioningResult} and scoped tokens into a {@link RegisterGymResponse}.
     *
     * @param result             the dynamic provisioning result
     * @param tenantAccessToken  issued tenant access token
     * @param tenantRefreshToken issued tenant refresh token
     * @return populated {@link RegisterGymResponse}
     */
    RegisterGymResponse toRegisterGymResponse(TenantProvisioningResult result, String tenantAccessToken, String tenantRefreshToken);

    /**
     * Wraps a {@link RegisterGymResponse} and status message into a unified {@link UserResponse}.
     *
     * @param message             confirmation message
     * @param registerGymResponse registration response payload
     * @return populated {@link UserResponse}
     */
    UserResponse toUserResponse(String message, RegisterGymResponse registerGymResponse);
}
