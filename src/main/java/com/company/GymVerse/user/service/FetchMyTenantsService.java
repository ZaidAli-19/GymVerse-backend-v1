package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.user.factory.UserOperation;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.request.FetchMyTenantsRequest;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.response.UserTenantSummaryResponse;
import com.company.GymVerse.user.validations.UserValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operation handler for FETCH_MY_TENANTS requests.
 * Queries central user-tenant associations, aggregates gym metadata and latest subscription status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchMyTenantsService implements UserOperation {

    private final UserAccountRepository userAccountRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final GymSubscriptionRepository gymSubscriptionRepository;
    private final UserMapper userMapper;
    private final UserValidations userValidations;

    @Override
    public UserRequestType getRequestType() {
        return UserRequestType.FETCH_MY_TENANTS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public UserResponse execute(final UserRequest request) {
        log.info("Processing FETCH_MY_TENANTS operation");

        userValidations.validateMasterContext();

        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        UserAccount callerAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found for authenticated ID: " + userId));

        List<UserTenantRole> roles = userTenantRoleRepository.findByUserAccountId(callerAccount.getId());

        FetchMyTenantsRequest filter = request != null ? request.getFetchMyTenantsRequest() : null;

        List<UserTenantSummaryResponse> summaries = roles.stream()
                .filter(role -> {
                    if (filter == null) {
                        return true;
                    }
                    if (filter.getRole() != null && !filter.getRole().equals(role.getRole())) {
                        return false;
                    }
                    if (filter.getGymStatus() != null && role.getGymTenant() != null && !filter.getGymStatus().equals(role.getGymTenant().getStatus())) {
                        return false;
                    }
                    return true;
                })
                .map(role -> {
                    GymTenant gym = role.getGymTenant();
                    GymSubscriptionStatus subStatus = null;
                    if (gym != null && gym.getId() != null) {
                        List<GymSubscription> subscriptions = gymSubscriptionRepository.findByGymTenantIdOrderBySubscribedAtDesc(gym.getId());
                        if (!subscriptions.isEmpty()) {
                            subStatus = subscriptions.get(0).getStatus();
                        }
                    }

                    return userMapper.toUserTenantSummaryResponse(role, subStatus);
                })
                .toList();

        return userMapper.toFetchMyTenantsUserResponse("User tenant directory retrieved successfully", summaries);
    }
}
