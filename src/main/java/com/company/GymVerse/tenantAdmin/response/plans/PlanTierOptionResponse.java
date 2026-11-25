package com.company.GymVerse.tenantAdmin.response.plans;

import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted tier option returned for Plans filter bar and Add Plan tier selector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTierOptionResponse {

    private Long id;
    private String name;
    private PlanTheme themeKey;

    @JsonProperty("isDefault")
    private boolean defaultTier;
}
