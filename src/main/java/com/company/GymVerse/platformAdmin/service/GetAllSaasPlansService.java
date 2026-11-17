package com.company.GymVerse.platformAdmin.service;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.mapper.SaasPlanMapper;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.request.SaasPlanFilterRequest;
import com.company.GymVerse.platformAdmin.response.PagedResponse;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.response.SaasPlanResponse;
import com.company.GymVerse.platformAdmin.specifications.SaasPlanSpecification;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.platformAdmin.validations.PlatformAdminValidations;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service executing dynamic catalog query and retrieval of SaaS subscription plans
 * utilizing Spring Data JPA Specifications and centralized pagination boundaries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAllSaasPlansService implements PlatformAdminFactoryOperation {

    private final SaasSubscriptionPlanRepository planRepository;
    private final SaasPlanMapper planMapper;
    private final PlatformAdminValidations validations;

    @Override
    public PlatformAdminRequestType getRequestType() {
        return PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS;
    }

    @Override
    @Transactional(transactionManager = "centralTransactionManager", readOnly = true)
    public PlatformAdminResponse execute(PlatformAdminRequest request) {
        log.info("Processing SaaS subscription plans query");

        // 1. Validate pagination boundaries, sort field whitelist, and price ranges
        validations.validateFetchPlans(request);

        // 2. Resolve query filter parameters strictly from planFilter
        SaasPlanFilterRequest filter = request != null ? request.getPlanFilter() : null;
        Boolean active = filter != null ? filter.getActive() : null;
        BillingCycle billingCycle = filter != null ? filter.getBillingCycle() : null;
        String name = filter != null ? filter.getName() : null;
        BigDecimal minPrice = filter != null ? filter.getMinPrice() : null;
        BigDecimal maxPrice = filter != null ? filter.getMaxPrice() : null;

        // 3. Resolve pagination & sorting strictly from root PlatformAdminRequest
        int page = request != null && request.getPage() != null ? request.getPage() : 0;
        int size = request != null && request.getSize() != null ? request.getSize() : 10;
        String sortBy = request != null && request.getSortBy() != null && !request.getSortBy().trim().isBlank()
                ? request.getSortBy().trim() : "id";
        String sortDir = request != null && request.getSortDirection() != null && !request.getSortDirection().trim().isBlank()
                ? request.getSortDirection().trim() : "asc";

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // 4. Build dynamic JPA Specification
        Specification<SaasSubscriptionPlan> spec = SaasPlanSpecification.withFilters(
                active, billingCycle, name, minPrice, maxPrice
        );

        // 5. Query repository at the database engine level
        Page<SaasSubscriptionPlan> planPage = planRepository.findAll(spec, pageable);

        // 6. Map domain entities to response DTOs
        List<SaasPlanResponse> content = planPage.getContent().stream()
                .map(planMapper::toResponse)
                .toList();

        PagedResponse<SaasPlanResponse> pagedResponse = new PagedResponse<>(
                content,
                planPage.getNumber(),
                planPage.getSize(),
                planPage.getTotalElements(),
                planPage.getTotalPages(),
                planPage.isLast()
        );

        log.info("Retrieved {} SaaS plans for page {} of {}", content.size(), planPage.getNumber(), planPage.getTotalPages());

        return PlatformAdminResponse.builder()
                .message("SaaS subscription plans catalog retrieved successfully")
                .plans(content)
                .pagedPlans(pagedResponse)
                .currentPage(planPage.getNumber())
                .totalPages(planPage.getTotalPages())
                .totalRecords(planPage.getTotalElements())
                .pageSize(planPage.getSize())
                .last(planPage.isLast())
                .build();
    }
}
