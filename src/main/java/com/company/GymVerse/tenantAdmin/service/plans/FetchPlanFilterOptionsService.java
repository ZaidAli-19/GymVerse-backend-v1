package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.repositories.tenant.GymFacilityRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanTierOptionResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves persisted plan tiers, facilities, and canonical custom theme keys
 * for populating Plans filters and the Add Plan modal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchPlanFilterOptionsService implements TenantAdminOperation {

    private final PlanTierRepository planTierRepository;
    private final GymFacilityRepository gymFacilityRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_PLAN_FILTER_OPTIONS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_PLAN_FILTER_OPTIONS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        List<PlanTierOptionResponse> tiers = planTierRepository.findAllByOrderByDefaultTierDescNameAsc()
                .stream()
                .map(mapper::toPlanTierOption)
                .toList();

        List<String> facilities = gymFacilityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(GymFacility::getName)
                .toList();

        List<String> customThemes = PlanTheme.getCustomThemeKeys();

        return mapper.toPlanFilterOptionsResponse(
                "Plan filter options retrieved successfully",
                tiers,
                facilities,
                customThemes
        );
    }
}
