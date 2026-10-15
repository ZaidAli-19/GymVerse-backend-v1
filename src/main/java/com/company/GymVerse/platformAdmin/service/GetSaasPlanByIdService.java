package com.company.GymVerse.platformAdmin.service;

import com.company.GymVerse.exceptions.BadRequestException;
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
 * Service retrieving a single SaaS subscription plan by its unique identifier.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetSaasPlanByIdService implements PlatformAdminFactoryOperation {

    private final SaasSubscriptionPlanRepository planRepository;
    private final SaasPlanMapper planMapper;
    private final PlatformAdminValidations validations;

    @Override
    public PlatformAdminRequestType getRequestType() {
        return PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID;
    }

    /**
     * Retrieves a SaaS subscription plan by ID directly.
     *
     * @param planId the primary key identifier of the plan
     * @return the mapped SaasPlanResponse
     * @throws ResourceNotFoundException if no plan exists with the given ID
     * @throws BadRequestException       if planId is null or non-positive
     */
    @Transactional(transactionManager = "centralTransactionManager", readOnly = true)
    public SaasPlanResponse getPlanById(Long planId) {
        if (planId == null || planId <= 0) {
            throw new BadRequestException("Plan ID must be a positive number");
        }

        SaasSubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.warn("SaaS subscription plan not found with id: {}", planId);
                    return new ResourceNotFoundException("SaasSubscriptionPlan not found with id: " + planId);
                });

        return planMapper.toResponse(plan);
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager", readOnly = true)
    public PlatformAdminResponse execute(PlatformAdminRequest request) {
        log.info("Processing single SaaS subscription plan retrieval");

        // 1. Validate plan ID input
        validations.validateFetchPlanById(request);

        // 2. Retrieve plan or throw ResourceNotFoundException
        SaasPlanResponse saasPlan = getPlanById(request.getPlanId());

        // 3. Build and return shared module response
        return PlatformAdminResponse.builder()
                .message("SaaS subscription plan retrieved successfully")
                .planId(saasPlan.getId())
                .id(saasPlan.getId())
                .name(saasPlan.getName())
                .price(saasPlan.getPrice())
                .active(saasPlan.getActive())
                .saasPlan(saasPlan)
                .build();
    }
}
