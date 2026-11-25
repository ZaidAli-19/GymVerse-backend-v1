package com.company.GymVerse.tenantAdmin.request.plans;

import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for CREATE_PLAN operations supporting default tiers (ELITE, LEGENDARY),
 * custom tiers with UI-aligned themes, reusable facilities, and optional discounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePlanRequest {

    private String name;
    private String description;
    private String tier;
    private PlanTheme customTheme;
    private List<String> facilities;
    private BigDecimal basePrice;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer durationDays;
    private Boolean isTrial;
}
