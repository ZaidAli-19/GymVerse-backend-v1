package com.company.GymVerse.platformAdmin.service;

import com.company.GymVerse.exceptions.ResourceNotFoundException;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.mapper.SaasPlanMapper;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.response.SaasPlanResponse;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.platformAdmin.validations.PlatformAdminValidations;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing SaaS subscription plan availability (activation and deactivation)
 * while strictly guaranteeing the immutability of prices, limits, and feature entitlements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdatePlanAvailabilityService implements PlatformAdminFactoryOperation {

    private final SaasSubscriptionPlanRepository planRepository;
    private final SaasPlanMapper planMapper;
    private final PlatformAdminValidations validations;

    @Override
    public PlatformAdminRequestType getRequestType() {
        return PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager")
    public PlatformAdminResponse execute(PlatformAdminRequest request) {
        log.info("Processing SaaS subscription plan availability update");

        // 1. Validate inputs
        validations.validateUpdatePlanAvailability(request);

        Long planId = request.getPlanId() != null ? request.getPlanId()
                : (request.getPlanAvailability() != null ? request.getPlanAvailability().getPlanId() : null);
        Boolean active = request.getActive() != null ? request.getActive()
                : (request.getPlanAvailability() != null ? request.getPlanAvailability().getActive() : null);

        // 2. Fetch existing plan or throw ResourceNotFoundException
        SaasSubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.warn("Cannot update plan availability: Plan not found with id: {}", planId);
                    return new ResourceNotFoundException("SaasSubscriptionPlan not found with id: " + planId);
                });

        // 3. Surgical update: mutate ONLY the active status
        plan.setActive(active);
        SaasSubscriptionPlan updatedPlan = planRepository.save(plan);

        String statusLabel = active ? "ACTIVE" : "INACTIVE";
        log.info("SaaS subscription plan id={} availability updated to: {}", planId, statusLabel);

        // 4. Map to response
        SaasPlanResponse responseDto = planMapper.toResponse(updatedPlan);

        return PlatformAdminResponse.builder()
                .message("SaaS subscription plan availability updated to: " + statusLabel)
                .planId(updatedPlan.getId())
                .id(updatedPlan.getId())
                .name(updatedPlan.getName())
                .price(updatedPlan.getPrice())
                .active(updatedPlan.isActive())
                .saasPlan(responseDto)
                .build();
    }
}
