package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.MembershipCalculationHelper;
import com.company.GymVerse.tenantAdmin.helper.PlanHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Orchestrates membership plan creation with default/custom tiers, UI-mapped themes,
 * auto-persisted facilities, and discount pricing calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePlanService implements TenantAdminOperation {

    private final MembershipPlanRepository membershipPlanRepository;
    private final PlanHelper planHelper;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.CREATE_PLAN;
    }

    @Override
    @Transactional(transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing CREATE_PLAN operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        CreatePlanRequest payload = request.getCreatePlanRequest();
        validations.validateCreatePlan(payload);

        PlanTier planTier = planHelper.resolveOrCreatePlanTier(payload.getTier(), payload.getCustomTheme());
        Set<GymFacility> facilities = planHelper.resolveOrCreateFacilities(payload.getFacilities());
        BigDecimal finalPrice = MembershipCalculationHelper.computeFinalPrice(
                payload.getBasePrice(),
                payload.getDiscountType(),
                payload.getDiscountValue()
        );

        MembershipPlan plan = MembershipPlan.builder()
                .name(payload.getName().trim())
                .description(payload.getDescription() != null ? payload.getDescription().trim() : null)
                .planTier(planTier)
                .facilities(facilities)
                .basePrice(payload.getBasePrice())
                .discountType(payload.getDiscountType())
                .discountValue(payload.getDiscountValue())
                .finalPrice(finalPrice)
                .durationDays(payload.getDurationDays())
                .trial(Boolean.TRUE.equals(payload.getIsTrial()))
                .active(true)
                .build();

        MembershipPlan saved = membershipPlanRepository.save(plan);
        PlanItemResponse item = mapper.toPlanItem(saved);

        log.info("Created membership plan id={} name='{}' tier='{}'", saved.getId(), saved.getName(), planTier.getName());
        return mapper.toPlanItemResponse("Membership plan created successfully", 201, item);
    }
}
