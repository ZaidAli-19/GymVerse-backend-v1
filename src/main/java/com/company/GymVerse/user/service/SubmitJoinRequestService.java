package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.user.factory.UserOperation;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.SubmitJoinRequestResponse;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.util.UserHelper;
import com.company.GymVerse.user.validations.UserValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Operation handler for SUBMIT_JOIN_REQUEST requests.
 * Allows central authenticated master users to submit a membership join application to an active gym tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitJoinRequestService implements UserOperation {

    private final GymTenantRepository gymTenantRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final TenantRoutingBridge tenantRoutingBridge;
    private final UserValidations userValidations;
    private final UserHelper userHelper;
    private final UserMapper userMapper;

    @Override
    public UserRequestType getRequestType() {
        return UserRequestType.SUBMIT_JOIN_REQUEST;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager")
    public UserResponse execute(final UserRequest request) {
        log.info("Processing SUBMIT_JOIN_REQUEST operation");

        userValidations.validateMasterContext();
        userValidations.validateSubmitJoinRequest(request);

        UserAccount callerAccount = userHelper.getAuthenticatedUserAccount();
        String gymCode = userHelper.normalizeGymCode(request.getSubmitJoinRequest().getGymCode());

        GymTenant gymTenant = gymTenantRepository.findByGymCode(gymCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with code: " + gymCode));

        GymSubscriptionStatus subscriptionStatus = userHelper.getLatestSubscriptionStatus(gymTenant.getId());

        userValidations.validateGymActiveForJoin(gymTenant, subscriptionStatus);
        userValidations.validateUserNotAlreadyAssociated(callerAccount.getId(), gymTenant.getId());

        JoinRequest savedJoinRequest = tenantRoutingBridge.callInTenantTx(gymTenant.getDbName(), status -> {
            userValidations.validateNoPendingJoinRequest(callerAccount.getId(), joinRequestRepository);

            JoinRequest newRequest = JoinRequest.builder()
                    .centralUserId(callerAccount.getId())
                    .email(callerAccount.getEmail())
                    .firstName(callerAccount.getFirstName())
                    .lastName(callerAccount.getLastName())
                    .phoneNumber(callerAccount.getPhoneNumber())
                    .status("PENDING")
                    .requestNotes(request.getSubmitJoinRequest().getRequestNotes())
                    .createdAt(Instant.now())
                    .build();

            return joinRequestRepository.save(newRequest);
        });

        log.info("Successfully recorded join request id={} for user id={} in gym '{}' (dbName='{}')",
                savedJoinRequest.getId(), callerAccount.getId(), gymTenant.getGymCode(), gymTenant.getDbName());

        SubmitJoinRequestResponse responsePayload = userMapper.toSubmitJoinRequestResponse(
                savedJoinRequest,
                gymTenant.getGymCode(),
                gymTenant.getGymName()
        );

        return userMapper.toSubmitJoinRequestUserResponse(
                "Join request submitted successfully. Awaiting gym administrator review.",
                responsePayload
        );
    }
}
