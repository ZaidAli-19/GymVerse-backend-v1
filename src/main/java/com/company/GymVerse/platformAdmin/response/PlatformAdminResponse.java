package com.company.GymVerse.platformAdmin.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformAdminResponse {

    private String message;
    private Long planId;
    private SaasPlanResponse saasPlan;

    // Direct scalar fields for backward compatibility and convenient client consumption
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean active;

    // ==========================================
    // FETCH_ALL_SAAS_PLANS fields
    // ==========================================
    private List<SaasPlanResponse> plans;
    private PagedResponse<SaasPlanResponse> pagedPlans;

    // ==========================================
    // FETCH_ALL_GYM_TENANTS fields
    // ==========================================
    private List<GymTenantSummaryResponse> tenants;
    private PagedResponse<GymTenantSummaryResponse> pagedTenants;

    private Integer currentPage;
    private Integer totalPages;
    private Long totalRecords;
    private Integer pageSize;
    private Boolean last;
}

