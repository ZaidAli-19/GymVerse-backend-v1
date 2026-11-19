package com.company.GymVerse.auth.request;

import com.company.GymVerse.models.central.enums.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for SWITCH_TENANT token exchange operation.
 * Supports targeting a gym by its database ID or unique discovery gym code,
 * and optionally disambiguating the target role if the user holds multiple roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchTenantRequest {

    /**
     * Central database ID of the target gym tenant.
     */
    private Long gymTenantId;

    /**
     * Unique human-readable code of the target gym tenant (e.g. "GYM-124D94FD").
     */
    private String gymCode;

    /**
     * Optional role context to assume if the caller holds multiple roles in the target gym.
     */
    private TenantRole role;
}
