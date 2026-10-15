package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAdminRequest {

    private PlatformAdminRequestType platformAdminRequestType;

    // ==========================================
    // CREATE_SAAS_PLAN fields
    // ==========================================
    private String name;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private Integer maxMembers;
    private Integer maxTrainers;
    private Boolean analyticsEnabled;
    private Boolean multiTrainerEnabled;
    private Boolean customBrandingEnabled;

    // ==========================================
    // UPDATE_PLAN_AVAILABILITY fields (for future use)
    // ==========================================
    private Long planId;
    private Boolean active;
}
