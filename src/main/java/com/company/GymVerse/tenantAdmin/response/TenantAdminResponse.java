package com.company.GymVerse.tenantAdmin.response;

import com.company.GymVerse.tenantAdmin.response.members.ApproveJoinRequestResponse;
import com.company.GymVerse.tenantAdmin.response.members.JoinRequestItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberListItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MembersSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanFilterOptionsResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlansSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard unified response wrapper for tenant-scoped operations on /api/tenant-admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantAdminResponse {

    private String message;
    private int status;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private TenantSubscriptionDetailsResponse tenantSubscriptionDetailsResponse;
    private PlansSummaryResponse plansSummaryResponse;
    private PlanFilterOptionsResponse planFilterOptionsResponse;
    private PagedResponse<PlanItemResponse> plansPageResponse;
    private PlanItemResponse planItemResponse;
    private PagedResponse<JoinRequestItemResponse> joinRequestsPageResponse;
    private JoinRequestItemResponse joinRequestItemResponse;
    private ApproveJoinRequestResponse approveJoinRequestResponse;
    private MembersSummaryResponse membersSummaryResponse;
    private PagedResponse<MemberListItemResponse> membersPageResponse;
    private MemberDetailsResponse memberDetailsResponse;
}
