package com.company.GymVerse.platformAdmin.service;

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
 * Single-responsibility service handling SaaS subscription plan creation.
 * Implements PlatformAdminFactoryOperation per module-level factory conventions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSaasPlanService implements PlatformAdminFactoryOperation {

    private final SaasSubscriptionPlanRepository saasSubscriptionPlanRepository;
    private final PlatformAdminValidations platformAdminValidations;
    private final SaasPlanMapper saasPlanMapper;

    @Override
    public PlatformAdminRequestType getRequestType() {
        return PlatformAdminRequestType.CREATE_SAAS_PLAN;
    }

    @Override
    @Transactional("centralTransactionManager")
    public PlatformAdminResponse execute(PlatformAdminRequest request) {
        log.info("Processing CREATE_SAAS_PLAN for plan name: '{}'", request != null ? request.getName() : "null");

        // 1. Validation
        platformAdminValidations.validateCreatePlan(request);

        // 2. Entity Mapping & Persistence
        SaasSubscriptionPlan plan = saasPlanMapper.toEntity(request);
        plan.setActive(true);
        SaasSubscriptionPlan savedPlan = saasSubscriptionPlanRepository.save(plan);

        log.info("Successfully created SaaS subscription plan ID: {} ('{}')", savedPlan.getId(), savedPlan.getName());

        // 3. Response Construction
        SaasPlanResponse planResponse = saasPlanMapper.toResponse(savedPlan);
        return PlatformAdminResponse.builder()
                .message("SaaS subscription plan created successfully")
                .planId(savedPlan.getId())
                .id(savedPlan.getId())
                .name(savedPlan.getName())
                .price(savedPlan.getPrice())
                .active(savedPlan.isActive())
                .saasPlan(planResponse)
                .build();
    }
}
