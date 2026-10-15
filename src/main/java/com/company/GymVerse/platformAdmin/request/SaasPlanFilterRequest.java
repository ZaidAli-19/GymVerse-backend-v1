package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Filter criteria and pagination parameters for querying SaaS subscription plans.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaasPlanFilterRequest {

    private Boolean active;
    private BillingCycle billingCycle;
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
