package com.company.GymVerse.tenantAdmin.response.plans;

import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed membership plan item returned in plan catalog queries and mutations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemResponse {

    private Long id;
    private String name;
    private String description;
    private Long tierId;
    private String tierName;
    private PlanTheme themeKey;
    private List<String> facilities;
    private BigDecimal basePrice;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal finalPrice;
    private boolean hasOffer;
    private Integer durationDays;

    @JsonProperty("isTrial")
    private boolean trial;

    private boolean active;
}
