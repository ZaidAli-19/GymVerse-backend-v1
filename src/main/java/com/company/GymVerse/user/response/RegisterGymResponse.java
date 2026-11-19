package com.company.GymVerse.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Detailed confirmation payload returned upon successful gym facility registration,
 * tenant database provisioning, free trial activation, and scoped token generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterGymResponse {

    private Long gymTenantId;
    private String gymCode;
    private String gymName;
    private String dbName;
    private String city;
    private String address;

    private String userRole;
    private String adminAuthority;

    private String subscriptionStatus;
    private LocalDate trialEndDate;
    private Long trialDaysRemaining;

    private String tenantAccessToken;
    private String tenantRefreshToken;
}
