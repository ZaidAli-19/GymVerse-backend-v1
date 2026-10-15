package com.company.GymVerse.platformAdmin.mapper;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.platformAdmin.response.GymTenantSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Handwritten mapper implementation for GymTenant entities to GymTenantSummaryResponse DTOs.
 */
@Component
public class GymTenantSummaryMapperImpl implements GymTenantSummaryMapper {

    @Override
    public GymTenantSummaryResponse toResponse(GymTenant tenant) {
        if (tenant == null) {
            return null;
        }

        return GymTenantSummaryResponse.builder()
                .id(tenant.getId())
                .gymName(tenant.getGymName())
                .contactEmail(tenant.getContactEmail())
                .phoneNumber(tenant.getPhoneNumber())
                .address(tenant.getAddress())
                .city(tenant.getCity())
                .logoUrl(tenant.getLogoUrl())
                .gymCode(tenant.getGymCode())
                .dbName(tenant.getDbName())
                .status(tenant.getStatus())
                .registeredAt(tenant.getRegisteredAt())
                .suspendedAt(tenant.getSuspendedAt())
                .suspensionReason(tenant.getSuspensionReason())
                .build();
    }

    @Override
    public List<GymTenantSummaryResponse> toResponseList(List<GymTenant> tenants) {
        if (tenants == null) {
            return Collections.emptyList();
        }

        return tenants.stream()
                .map(this::toResponse)
                .toList();
    }
}
