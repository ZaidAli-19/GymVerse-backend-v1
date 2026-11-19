package com.company.GymVerse.tenantAdmin.response;

import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detailed SaaS subscription and feature entitlements payload for an operational gym tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionDetailsResponse {

    private Long subscriptionId;
    private Long tenantId;
    private String gymCode;
    private String gymName;
    private Long planId;
    private String planName;
    private String planDescription;
    private BigDecimal planPrice;
    private BigDecimal pricePaid;
    private BillingCycle billingCycle;
    private GymSubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysRemaining;

    @JsonProperty("isTrial")
    private boolean isTrial;

    // Feature Entitlements
    private Integer maxMembers;
    private Integer maxTrainers;
    private boolean analyticsEnabled;
    private boolean multiTrainerEnabled;
    private boolean customBrandingEnabled;
}
