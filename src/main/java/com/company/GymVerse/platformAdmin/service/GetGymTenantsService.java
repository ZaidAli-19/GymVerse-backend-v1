package com.company.GymVerse.platformAdmin.service;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.mapper.GymTenantSummaryMapper;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.GymTenantSummaryResponse;
import com.company.GymVerse.platformAdmin.response.PagedResponse;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.specifications.GymTenantSpecification;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.platformAdmin.validations.PlatformAdminValidations;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service executing dynamic directory query and retrieval of registered gym tenants
 * utilizing Spring Data JPA Specifications and pagination boundaries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetGymTenantsService implements PlatformAdminFactoryOperation {

    private final GymTenantRepository gymTenantRepository;
    private final GymTenantSummaryMapper tenantMapper;
    private final PlatformAdminValidations validations;

    @Override
    public PlatformAdminRequestType getRequestType() {
        return PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager", readOnly = true)
    public PlatformAdminResponse execute(PlatformAdminRequest request) {
        log.info("Processing registered gym tenants directory query");

        // 1. Validate pagination boundaries, sort field whitelist, and directions
        validations.validateFetchGymTenants(request);

        // 2. Resolve query parameters (supporting both direct fields and nested tenantFilter)
        GymStatus status = request != null && request.getGymStatus() != null ? request.getGymStatus()
                : (request != null && request.getTenantFilter() != null ? request.getTenantFilter().getStatus() : null);
        String city = request != null && request.getCity() != null ? request.getCity()
                : (request != null && request.getTenantFilter() != null ? request.getTenantFilter().getCity() : null);
        String gymName = request != null && request.getGymName() != null ? request.getGymName()
                : (request != null && request.getTenantFilter() != null ? request.getTenantFilter().getGymName() : null);
        String gymCode = request != null && request.getGymCode() != null ? request.getGymCode()
                : (request != null && request.getTenantFilter() != null ? request.getTenantFilter().getGymCode() : null);

        // 3. Resolve pagination defaults (default sortBy: "registeredAt", default sortDir: "desc")
        int page = request != null && request.getPage() != null ? request.getPage()
                : (request != null && request.getTenantFilter() != null && request.getTenantFilter().getPage() != null
                ? request.getTenantFilter().getPage() : 0);
        int size = request != null && request.getSize() != null ? request.getSize()
                : (request != null && request.getTenantFilter() != null && request.getTenantFilter().getSize() != null
                ? request.getTenantFilter().getSize() : 10);
        String sortBy = request != null && request.getSortBy() != null && !request.getSortBy().trim().isBlank()
                ? request.getSortBy().trim()
                : (request != null && request.getTenantFilter() != null && request.getTenantFilter().getSortBy() != null && !request.getTenantFilter().getSortBy().trim().isBlank()
                ? request.getTenantFilter().getSortBy().trim() : "registeredAt");
        String sortDir = request != null && request.getSortDirection() != null && !request.getSortDirection().trim().isBlank()
                ? request.getSortDirection().trim()
                : (request != null && request.getTenantFilter() != null && request.getTenantFilter().getSortDirection() != null && !request.getTenantFilter().getSortDirection().trim().isBlank()
                ? request.getTenantFilter().getSortDirection().trim() : "desc");

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // 4. Build dynamic JPA Specification
        Specification<GymTenant> spec = GymTenantSpecification.withFilters(status, city, gymName, gymCode);

        // 5. Query repository at the central database level
        Page<GymTenant> tenantPage = gymTenantRepository.findAll(spec, pageable);

        // 6. Map domain entities to response DTOs
        List<GymTenantSummaryResponse> content = tenantMapper.toResponseList(tenantPage.getContent());

        PagedResponse<GymTenantSummaryResponse> pagedResponse = new PagedResponse<>(
                content,
                tenantPage.getNumber(),
                tenantPage.getSize(),
                tenantPage.getTotalElements(),
                tenantPage.getTotalPages(),
                tenantPage.isLast()
        );

        log.info("Retrieved {} gym tenants for page {} of {}", content.size(), tenantPage.getNumber(), tenantPage.getTotalPages());

        return PlatformAdminResponse.builder()
                .message(tenantPage.getTotalElements() + " gym tenant(s) retrieved successfully")
                .tenants(content)
                .pagedTenants(pagedResponse)
                .currentPage(tenantPage.getNumber())
                .pageSize(tenantPage.getSize())
                .totalRecords(tenantPage.getTotalElements())
                .totalPages(tenantPage.getTotalPages())
                .last(tenantPage.isLast())
                .build();
    }
}
