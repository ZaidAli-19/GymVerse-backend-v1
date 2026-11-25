package com.company.GymVerse.tenantAdmin.request.plans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated and filtered query request for FETCH_PLANS operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchPlansRequest {

    /**
     * Optional tier name filter (null or "ALL" returns plans across all tiers).
     */
    private String tier;

    /**
     * Optional offer filter (true = plans with active discount, false = no discount).
     */
    private Boolean hasOffer;

    /**
     * Optional active status filter.
     */
    private Boolean active;

    /**
     * Optional trial flag filter.
     */
    private Boolean isTrial;

    /**
     * Optional case-insensitive partial match against plan name, description, or facility names.
     */
    private String search;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
