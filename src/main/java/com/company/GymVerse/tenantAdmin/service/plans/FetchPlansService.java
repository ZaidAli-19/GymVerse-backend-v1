package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.specifications.plans.MembershipPlanSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates paginated, sorted, and JPA Specification-filtered membership plan queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchPlansService implements TenantAdminOperation {

    private final MembershipPlanRepository membershipPlanRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_PLANS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_PLANS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchPlansRequest filter = request.getFetchPlansRequest();
        Pageable pageable = validations.validateFetchPlans(filter);

        Specification<MembershipPlan> spec = MembershipPlanSpecification.withFilters(
                filter != null ? filter.getTier() : null,
                filter != null ? filter.getHasOffer() : null,
                filter != null ? filter.getActive() : null,
                filter != null ? filter.getIsTrial() : null,
                filter != null ? filter.getSearch() : null
        );

        Page<MembershipPlan> planPage = membershipPlanRepository.findAll(spec, pageable);
        return mapper.toPagedPlansResponse("Membership plans retrieved successfully", planPage);
    }
}
