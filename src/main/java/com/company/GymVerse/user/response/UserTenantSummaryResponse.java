package com.company.GymVerse.user.response;

import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary DTO representing a single gym tenant association for an authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTenantSummaryResponse {

    private Long tenantId;
    private String gymCode;
    private String gymName;
    private String logoUrl;
    private String city;
    private String address;
    private TenantRole userRole;
    private AdminAuthority adminAuthority;
    private TenantMembershipStatus roleStatus;
    private GymStatus gymStatus;
    private GymSubscriptionStatus subscriptionStatus;
    private Instant joinedAt;
}
