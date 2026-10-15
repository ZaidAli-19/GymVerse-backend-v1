package com.company.GymVerse.platformAdmin.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nested helper DTO representing plan availability toggle parameters.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanAvailabilityRequest {

    private Long planId;
    private Boolean active;
}
