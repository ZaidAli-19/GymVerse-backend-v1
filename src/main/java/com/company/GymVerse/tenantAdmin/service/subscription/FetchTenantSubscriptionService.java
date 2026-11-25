package com.company.GymVerse.tenantAdmin.service.subscription;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import com.company.GymVerse.user.util.SubscriptionDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Operation handler for FETCH_TENANT_SUBSCRIPTION requests.
 * Queries Central DB for the active gym tenant's subscription status, trial countdown,
 * plan pricing, and feature entitlement limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchTenantSubscriptionService implements TenantAdminOperation {

    private final GymSubscriptionRepository gymSubscriptionRepository;
    private final TenantAdminValidations tenantAdminValidations;
    private final TenantAdminMapper tenantAdminMapper;
    private final TenantAdminContextHelper contextHelper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "centralTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_TENANT_SUBSCRIPTION operation");

        tenantAdminValidations.validateTenantAdminContext();
        tenantAdminValidations.validateRequest(request);

        Long tenantId = contextHelper.extractTenantId();

        GymSubscription subscription = gymSubscriptionRepository.findFirstByGymTenantIdOrderBySubscribedAtDesc(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for tenant ID: " + tenantId));

        LocalDate today = LocalDate.now();
        boolean isTrial = subscription.getStatus() == GymSubscriptionStatus.TRIAL
                && SubscriptionDateUtil.isTrialActive(subscription.getEndDate(), today);
        long daysRemaining = SubscriptionDateUtil.calculateDaysRemaining(subscription.getEndDate(), today);

        TenantSubscriptionDetailsResponse details = tenantAdminMapper.toSubscriptionDetails(subscription, daysRemaining, isTrial);

        return tenantAdminMapper.toResponse("Tenant subscription details retrieved successfully", details);
    }
}
