package com.company.GymVerse.auth.response;

import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for SWITCH_TENANT token exchange operation containing scoped tokens and tenant identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchTenantResponse {

    private String tenantAccessToken;
    private String tenantRefreshToken;
    private Long tenantId;
    private String gymCode;
    private String gymName;
    private String tenantDbName;
    private TenantRole role;
    private AdminAuthority adminAuthority;
}
