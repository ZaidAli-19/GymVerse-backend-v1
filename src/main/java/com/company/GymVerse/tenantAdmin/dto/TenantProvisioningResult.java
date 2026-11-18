package com.company.GymVerse.tenantAdmin.dto;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.tenant.GymConfig;
import com.company.GymVerse.models.tenant.TenantUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Result DTO representing the outcome of tenant dynamic provisioning and initial database seeding.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantProvisioningResult {

    private GymTenant gymTenant;
    private UserTenantRole userTenantRole;
    private GymSubscription gymSubscription;
    private TenantUser tenantAdminUser;
    private GymConfig gymConfig;
    private String gymCode;
    private String dbName;
    private LocalDate trialEndDate;
    private long trialDaysRemaining;
}
