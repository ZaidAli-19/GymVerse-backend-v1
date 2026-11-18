package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Value object encapsulating claims and tenant context for tenant-scoped JWT tokens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantTokenClaims {

    /**
     * Central master user ID (stored in token subject).
     */
    private Long userId;

    /**
     * Central user account email address.
     */
    private String email;

    /**
     * Central gym tenant registry ID.
     */
    private Long tenantId;

    /**
     * Unique gym business code (e.g. GYM-A7B2C1D9).
     */
    private String gymCode;

    /**
     * Physical database name for multi-tenant isolation routing (e.g. gymverse_tenant_gold_gym_123456).
     */
    private String tenantDbName;

    /**
     * Operational role within the gym tenant (TENANT_ADMIN, MEMBER, TRAINER, STAFF).
     */
    private TenantRole role;

    /**
     * Administrative authority level for tenant administrators (PRIMARY, DELEGATED, NONE).
     */
    @Builder.Default
    private AdminAuthority adminAuthority = AdminAuthority.NONE;
}
