package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Shared module-level request DTO for Platform Administration.
 * Modeled after ConnectSouq's FranchiseOperatorRequest standard.
 *
 * <p>Common pagination (page, size) and sorting (sortBy, sortDirection) are centralized
 * exclusively at the root level, eliminating parameter duplication and precedence ambiguities.
 * Operation-specific query filters are encapsulated into dedicated nested DTOs (planFilter, tenantFilter).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAdminRequest {

    // ==========================================
    // Operation Discriminator
    // ==========================================
    private PlatformAdminRequestType platformAdminRequestType;

    // ==========================================
    // Unified Pagination & Sorting (Root Level)
    // Applies across all multi-record fetch queries
    // ==========================================
    private Integer page;          // default: 0, min: 0
    private Integer size;          // default: 10, min: 1, max: 100
    private String sortBy;        // whitelist-guarded per operation
    private String sortDirection; // "asc" | "desc"

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
    // FETCH_SAAS_PLAN_BY_ID target identifier
    // ==========================================
    private Long planId;

    // ==========================================
    // UPDATE_PLAN_AVAILABILITY fields
    // ==========================================
    private Boolean active;
    private UpdatePlanAvailabilityRequest planAvailability;

    // ==========================================
    // FETCH_ALL_SAAS_PLANS domain filter criteria
    // ==========================================
    private SaasPlanFilterRequest planFilter;

    // ==========================================
    // FETCH_ALL_GYM_TENANTS domain filter criteria
    // ==========================================
    private GymTenantFilterRequest tenantFilter;
}
