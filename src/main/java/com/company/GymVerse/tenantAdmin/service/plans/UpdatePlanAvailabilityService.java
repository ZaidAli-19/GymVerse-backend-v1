package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates toggling active availability for a tenant membership plan.
 */
@Slf4j
@Service("tenantUpdatePlanAvailabilityService")
@RequiredArgsConstructor
public class UpdatePlanAvailabilityService implements TenantAdminOperation {

    private final MembershipPlanRepository membershipPlanRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.UPDATE_PLAN_AVAILABILITY;
    }

    @Override
    @Transactional(transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing UPDATE_PLAN_AVAILABILITY operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        UpdatePlanAvailabilityRequest payload = request.getUpdatePlanAvailabilityRequest();
        validations.validateUpdatePlanAvailability(payload);

        MembershipPlan plan = membershipPlanRepository.findById(payload.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found with ID: " + payload.getPlanId()));

        plan.setActive(Boolean.TRUE.equals(payload.getActive()));
        MembershipPlan saved = membershipPlanRepository.save(plan);

        PlanItemResponse item = mapper.toPlanItem(saved);
        return mapper.toPlanItemResponse("Membership plan availability updated successfully", 200, item);
    }
}
