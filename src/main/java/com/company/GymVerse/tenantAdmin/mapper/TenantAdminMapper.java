package com.company.GymVerse.tenantAdmin.mapper;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;

/**
 * Handwritten mapper contract for tenant administration domain and response models.
 */
public interface TenantAdminMapper {

    /**
     * Converts a {@link GymSubscription} and calculated trial metrics into a {@link TenantSubscriptionDetailsResponse}.
     *
     * @param subscription   the gym subscription entity
     * @param daysRemaining  remaining days in subscription/trial period
     * @param isTrial        flag indicating active trial status
     * @return populated {@link TenantSubscriptionDetailsResponse}
     */
    TenantSubscriptionDetailsResponse toSubscriptionDetails(GymSubscription subscription, Long daysRemaining, boolean isTrial);

    /**
     * Wraps a {@link TenantSubscriptionDetailsResponse} and status message into a unified {@link TenantAdminResponse}.
     *
     * @param message  confirmation message
     * @param details  subscription details payload
     * @return populated {@link TenantAdminResponse}
     */
    TenantAdminResponse toResponse(String message, TenantSubscriptionDetailsResponse details);
}
