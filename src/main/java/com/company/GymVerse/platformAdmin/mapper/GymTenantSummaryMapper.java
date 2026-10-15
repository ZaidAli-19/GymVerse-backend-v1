package com.company.GymVerse.platformAdmin.mapper;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.platformAdmin.response.GymTenantSummaryResponse;

import java.util.List;

/**
 * Contract for mapping central GymTenant entities to GymTenantSummaryResponse DTOs.
 */
public interface GymTenantSummaryMapper {

    /**
     * Maps a single GymTenant entity to GymTenantSummaryResponse.
     *
     * @param tenant the entity to map
     * @return the response DTO, or null if entity is null
     */
    GymTenantSummaryResponse toResponse(GymTenant tenant);

    /**
     * Maps a list of GymTenant entities to a list of GymTenantSummaryResponse DTOs.
     *
     * @param tenants the list of entities to map
     * @return the list of response DTOs
     */
    List<GymTenantSummaryResponse> toResponseList(List<GymTenant> tenants);
}
