package com.company.GymVerse.platformAdmin.mapper;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.SaasPlanResponse;

/**
 * Mapper interface for transforming between SaaS subscription plan entities and DTOs.
 */
public interface SaasPlanMapper {

    SaasSubscriptionPlan toEntity(PlatformAdminRequest request);

    SaasPlanResponse toResponse(SaasSubscriptionPlan entity);
}
