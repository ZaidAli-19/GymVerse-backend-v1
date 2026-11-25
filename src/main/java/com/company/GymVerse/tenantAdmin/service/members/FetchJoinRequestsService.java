package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchJoinRequestsRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.specifications.members.JoinRequestSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates paginated, sorted, and JPA Specification-filtered retrieval of gym join requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchJoinRequestsService implements TenantAdminOperation {

    private final JoinRequestRepository joinRequestRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_JOIN_REQUESTS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_JOIN_REQUESTS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchJoinRequestsRequest filter = request.getFetchJoinRequestsRequest();
        Pageable pageable = validations.validateFetchJoinRequests(filter);
        JoinRequestStatus statusFilter = validations.parseJoinRequestStatusFilter(filter != null ? filter.getStatus() : null);

        Specification<JoinRequest> spec = JoinRequestSpecification.withFilters(
                statusFilter,
                filter != null ? filter.getSearch() : null
        );

        Page<JoinRequest> page = joinRequestRepository.findAll(spec, pageable);
        return mapper.toPagedJoinRequestsResponse("Join requests retrieved successfully", page);
    }
}
