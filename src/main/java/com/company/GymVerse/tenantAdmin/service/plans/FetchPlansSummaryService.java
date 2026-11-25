package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.PlanHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.TierCountResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates Plans mini-dashboard retrieval: total active plans and per-tier breakdown.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchPlansSummaryService implements TenantAdminOperation {

    private final PlanTierRepository planTierRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final PlanHelper planHelper;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_PLANS_SUMMARY;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_PLANS_SUMMARY operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        List<PlanTier> tiers = planTierRepository.findAllByOrderByDefaultTierDescNameAsc();
        List<MembershipPlan> plans = membershipPlanRepository.findAll();

        long totalPlans = plans.size();
        long totalActivePlans = plans.stream().filter(MembershipPlan::isActive).count();
        List<TierCountResponse> tierBreakdown = planHelper.buildTierCounts(tiers, plans);

        return mapper.toPlansSummaryResponse(
                "Plans summary retrieved successfully",
                totalActivePlans,
                totalPlans,
                tierBreakdown
        );
    }
}
