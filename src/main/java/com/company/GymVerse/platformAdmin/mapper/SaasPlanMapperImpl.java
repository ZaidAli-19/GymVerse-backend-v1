package com.company.GymVerse.platformAdmin.mapper;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.SaasPlanResponse;
import org.springframework.stereotype.Component;

/**
 * Handwritten implementation of SaasPlanMapper per RULES.md Rule 4.
 */
@Component
public class SaasPlanMapperImpl implements SaasPlanMapper {

    @Override
    public SaasSubscriptionPlan toEntity(PlatformAdminRequest request) {
        if (request == null) {
            return null;
        }

        return SaasSubscriptionPlan.builder()
                .name(request.getName() != null ? request.getName().trim() : null)
                .description(request.getDescription())
                .price(request.getPrice())
                .billingCycle(request.getBillingCycle())
                .maxMembers(request.getMaxMembers())
                .maxTrainers(request.getMaxTrainers())
                .analyticsEnabled(Boolean.TRUE.equals(request.getAnalyticsEnabled()))
                .multiTrainerEnabled(Boolean.TRUE.equals(request.getMultiTrainerEnabled()))
                .customBrandingEnabled(Boolean.TRUE.equals(request.getCustomBrandingEnabled()))
                .active(true)
                .build();
    }

    @Override
    public SaasPlanResponse toResponse(SaasSubscriptionPlan entity) {
        if (entity == null) {
            return null;
        }

        return SaasPlanResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .billingCycle(entity.getBillingCycle())
                .maxMembers(entity.getMaxMembers())
                .maxTrainers(entity.getMaxTrainers())
                .analyticsEnabled(entity.isAnalyticsEnabled())
                .multiTrainerEnabled(entity.isMultiTrainerEnabled())
                .customBrandingEnabled(entity.isCustomBrandingEnabled())
                .active(entity.isActive())
                .build();
    }
}
