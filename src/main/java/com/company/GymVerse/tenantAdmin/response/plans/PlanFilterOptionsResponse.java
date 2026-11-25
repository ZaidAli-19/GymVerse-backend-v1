package com.company.GymVerse.tenantAdmin.response.plans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filter & Add-Plan dropdown options payload returned by FETCH_PLAN_FILTER_OPTIONS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFilterOptionsResponse {

    private List<PlanTierOptionResponse> tiers;
    private List<String> facilities;
    private List<String> customThemes;
}
