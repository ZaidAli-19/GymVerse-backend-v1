package com.company.GymVerse.tenantAdmin.mapper;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handwritten implementation of {@link TenantAdminMapper} avoiding runtime reflection or code-generation overhead.
 */
@Component
public class TenantAdminMapperImpl implements TenantAdminMapper {

    @Override
    public TenantSubscriptionDetailsResponse toSubscriptionDetails(
            final GymSubscription subscription,
            final Long daysRemaining,
            final boolean isTrial
    ) {
        if (subscription == null) {
            return null;
        }

        GymTenant tenant = subscription.getGymTenant();
        SaasSubscriptionPlan plan = subscription.getPlan();

        return TenantSubscriptionDetailsResponse.builder()
                .subscriptionId(subscription.getId())
                .tenantId(tenant != null ? tenant.getId() : null)
                .gymCode(tenant != null ? tenant.getGymCode() : null)
                .gymName(tenant != null ? tenant.getGymName() : null)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getName() : null)
                .planDescription(plan != null ? plan.getDescription() : null)
                .planPrice(plan != null ? plan.getPrice() : null)
                .pricePaid(subscription.getPricePaid())
                .billingCycle(subscription.getBillingCycle())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .isTrial(isTrial)
                .maxMembers(plan != null ? plan.getMaxMembers() : null)
                .maxTrainers(plan != null ? plan.getMaxTrainers() : null)
                .analyticsEnabled(plan != null && plan.isAnalyticsEnabled())
                .multiTrainerEnabled(plan != null && plan.isMultiTrainerEnabled())
                .customBrandingEnabled(plan != null && plan.isCustomBrandingEnabled())
                .build();
    }

    @Override
    public TenantAdminResponse toResponse(
            final String message,
            final TenantSubscriptionDetailsResponse details
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .tenantSubscriptionDetailsResponse(details)
                .build();
    }
}
