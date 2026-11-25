package com.company.GymVerse.tenantAdmin.mapper;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.tenantAdmin.response.PagedResponse;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanFilterOptionsResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanTierOptionResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlansSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.plans.TierCountResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

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

    @Override
    public PlanTierOptionResponse toPlanTierOption(final PlanTier tier) {
        if (tier == null) {
            return null;
        }
        return PlanTierOptionResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .themeKey(tier.getThemeKey())
                .defaultTier(tier.isDefaultTier())
                .build();
    }

    @Override
    public PlanItemResponse toPlanItem(final MembershipPlan plan) {
        if (plan == null) {
            return null;
        }

        PlanTier tier = plan.getPlanTier();
        List<String> facilityNames = plan.getFacilities() == null
                ? Collections.emptyList()
                : plan.getFacilities().stream()
                .map(GymFacility::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();

        boolean hasOffer = plan.getDiscountType() != null
                && plan.getDiscountValue() != null
                && plan.getDiscountValue().compareTo(BigDecimal.ZERO) > 0;

        return PlanItemResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .tierId(tier != null ? tier.getId() : null)
                .tierName(tier != null ? tier.getName() : null)
                .themeKey(tier != null ? tier.getThemeKey() : null)
                .facilities(facilityNames)
                .basePrice(plan.getBasePrice())
                .discountType(plan.getDiscountType())
                .discountValue(plan.getDiscountValue())
                .finalPrice(plan.getFinalPrice())
                .hasOffer(hasOffer)
                .durationDays(plan.getDurationDays())
                .trial(plan.isTrial())
                .active(plan.isActive())
                .build();
    }

    @Override
    public TenantAdminResponse toPlansSummaryResponse(
            final String message,
            final long totalActivePlans,
            final long totalPlans,
            final List<TierCountResponse> tierBreakdown
    ) {
        PlansSummaryResponse summary = buildPlansSummary(totalActivePlans, totalPlans, tierBreakdown);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .plansSummaryResponse(summary)
                .build();
    }

    @Override
    public TenantAdminResponse toPlanFilterOptionsResponse(
            final String message,
            final List<PlanTierOptionResponse> tiers,
            final List<String> facilities,
            final List<String> customThemes
    ) {
        PlanFilterOptionsResponse options = buildPlanFilterOptions(tiers, facilities, customThemes);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .planFilterOptionsResponse(options)
                .build();
    }

    @Override
    public TenantAdminResponse toPagedPlansResponse(
            final String message,
            final Page<MembershipPlan> planPage
    ) {
        List<PlanItemResponse> items = planPage.getContent().stream()
                .map(this::toPlanItem)
                .toList();
        PagedResponse<PlanItemResponse> paged = toPagedResponse(planPage, items);

        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .plansPageResponse(paged)
                .build();
    }

    @Override
    public TenantAdminResponse toPlanItemResponse(
            final String message,
            final int httpStatus,
            final PlanItemResponse item
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(httpStatus)
                .timestamp(Instant.now())
                .planItemResponse(item)
                .build();
    }
}
