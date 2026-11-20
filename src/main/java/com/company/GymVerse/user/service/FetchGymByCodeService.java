package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.user.factory.UserOperation;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.FetchGymByCodeResponse;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.util.UserHelper;
import com.company.GymVerse.user.validations.UserValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operation handler for FETCH_GYM_BY_CODE requests.
 * Allows central authenticated master users to discover a gym and inspect its metadata before submitting a join request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchGymByCodeService implements UserOperation {

    private final GymTenantRepository gymTenantRepository;
    private final UserValidations userValidations;
    private final UserHelper userHelper;
    private final UserMapper userMapper;

    @Override
    public UserRequestType getRequestType() {
        return UserRequestType.FETCH_GYM_BY_CODE;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public UserResponse execute(final UserRequest request) {
        log.info("Processing FETCH_GYM_BY_CODE operation");

        userValidations.validateMasterContext();
        userValidations.validateFetchGymByCode(request);

        String gymCode = userHelper.normalizeGymCode(request.getFetchGymByCodeRequest().getGymCode());

        GymTenant gymTenant = gymTenantRepository.findByGymCode(gymCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with code: " + gymCode));

        GymSubscriptionStatus subscriptionStatus = userHelper.getLatestSubscriptionStatus(gymTenant.getId());

        userValidations.validateGymActiveForJoin(gymTenant, subscriptionStatus);

        FetchGymByCodeResponse responsePayload = userMapper.toFetchGymByCodeResponse(gymTenant, subscriptionStatus);

        return userMapper.toFetchGymByCodeUserResponse("Gym details retrieved successfully", responsePayload);
    }
}
