package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Filter criteria for querying SaaS subscription plans.
 * Pagination and sorting are managed exclusively at the root PlatformAdminRequest level.
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
}
