package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.RejectJoinRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.members.JoinRequestItemResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Orchestrates rejecting a pending join request inside the tenant database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RejectJoinRequestService implements TenantAdminOperation {

    private final JoinRequestRepository joinRequestRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminContextHelper contextHelper;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.REJECT_JOIN_REQUEST;
    }

    @Override
    @Transactional(transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing REJECT_JOIN_REQUEST operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        RejectJoinRequest payload = request.getRejectJoinRequest();
        validations.validateRejectJoinRequest(payload);

        JoinRequest joinRequest = joinRequestRepository.findById(payload.getJoinRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found with ID: " + payload.getJoinRequestId()));

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING join requests can be rejected; current status is " + joinRequest.getStatus());
        }

        TenantUser reviewingAdmin = contextHelper.resolveCurrentAdminUser();

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setAdminNotes(payload.getAdminNotes());
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedByAdmin(reviewingAdmin);

        JoinRequest saved = joinRequestRepository.save(joinRequest);
        JoinRequestItemResponse item = mapper.toJoinRequestItem(saved);

        log.info("Rejected joinRequestId={}", saved.getId());
        return mapper.toJoinRequestItemResponse("Join request rejected successfully", item);
    }
}
