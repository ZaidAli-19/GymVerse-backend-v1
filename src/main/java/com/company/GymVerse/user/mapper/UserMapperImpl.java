package com.company.GymVerse.user.mapper;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import com.company.GymVerse.user.dto.TenantProvisioningResult;
import com.company.GymVerse.user.request.RegisterGymRequest;
import com.company.GymVerse.user.response.FetchMyTenantsResponse;
import com.company.GymVerse.user.response.RegisterGymResponse;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.response.UserTenantSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Handwritten implementation of {@link UserMapper} with zero reflection.
 */
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public TenantProvisioningCommand toProvisioningCommand(
            final RegisterGymRequest request,
            final UserAccount ownerAccount
    ) {
        if (request == null) {
            return null;
        }

        Double lat = request.getLatitude() != null ? request.getLatitude().doubleValue() : null;
        Double lng = request.getLongitude() != null ? request.getLongitude().doubleValue() : null;

        return TenantProvisioningCommand.builder()
                .userAccount(ownerAccount)
                .gymName(request.getGymName())
                .address(request.getAddress())
                .city(request.getCity())
                .customGymCode(request.getCustomGymCode())
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail() : (ownerAccount != null ? ownerAccount.getEmail() : null))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : (ownerAccount != null ? ownerAccount.getPhoneNumber() : null))
                .latitude(lat)
                .longitude(lng)
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .build();
    }

    @Override
    public RegisterGymResponse toRegisterGymResponse(
            final TenantProvisioningResult result,
            final String tenantAccessToken,
            final String tenantRefreshToken
    ) {
        if (result == null) {
            return null;
        }

        GymTenant tenant = result.getGymTenant();
        GymSubscription sub = result.getGymSubscription();
        UserTenantRole role = result.getUserTenantRole();

        return RegisterGymResponse.builder()
                .gymTenantId(tenant != null ? tenant.getId() : null)
                .gymCode(result.getGymCode())
                .gymName(tenant != null ? tenant.getGymName() : null)
                .dbName(result.getDbName())
                .city(tenant != null ? tenant.getCity() : null)
                .address(tenant != null ? tenant.getAddress() : null)
                .userRole(role != null && role.getRole() != null ? role.getRole().name() : null)
                .adminAuthority(role != null && role.getAdminAuthority() != null ? role.getAdminAuthority().name() : null)
                .subscriptionStatus(sub != null && sub.getStatus() != null ? sub.getStatus().name() : null)
                .trialEndDate(result.getTrialEndDate())
                .trialDaysRemaining(result.getTrialDaysRemaining())
                .tenantAccessToken(tenantAccessToken)
                .tenantRefreshToken(tenantRefreshToken)
                .build();
    }

    @Override
    public UserResponse toUserResponse(
            final String message,
            final RegisterGymResponse registerGymResponse
    ) {
        return UserResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .registerGymResponse(registerGymResponse)
                .build();
    }

    @Override
    public UserTenantSummaryResponse toUserTenantSummaryResponse(
            final UserTenantRole role,
            final GymSubscriptionStatus subscriptionStatus
    ) {
        if (role == null) {
            return null;
        }
        GymTenant gym = role.getGymTenant();
        return UserTenantSummaryResponse.builder()
                .tenantId(gym != null ? gym.getId() : null)
                .gymCode(gym != null ? gym.getGymCode() : null)
                .gymName(gym != null ? gym.getGymName() : null)
                .logoUrl(gym != null ? gym.getLogoUrl() : null)
                .city(gym != null ? gym.getCity() : null)
                .address(gym != null ? gym.getAddress() : null)
                .userRole(role.getRole())
                .adminAuthority(role.getAdminAuthority())
                .roleStatus(role.getStatus())
                .gymStatus(gym != null ? gym.getStatus() : null)
                .subscriptionStatus(subscriptionStatus)
                .joinedAt(role.getJoinedAt())
                .build();
    }

    @Override
    public UserResponse toFetchMyTenantsUserResponse(
            final String message,
            final List<UserTenantSummaryResponse> tenants
    ) {
        List<UserTenantSummaryResponse> safeList = tenants != null ? tenants : List.of();
        FetchMyTenantsResponse payload = FetchMyTenantsResponse.builder()
                .tenants(safeList)
                .totalTenants(safeList.size())
                .build();

        return UserResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .fetchMyTenantsResponse(payload)
                .build();
    }
}
