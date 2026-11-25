package com.company.GymVerse.tenantAdmin.response;

import com.company.GymVerse.tenantAdmin.response.dashboard.AdminDashboardResponse;
import com.company.GymVerse.tenantAdmin.response.finance.ExpenseTransactionResponse;
import com.company.GymVerse.tenantAdmin.response.finance.FinanceOverviewResponse;
import com.company.GymVerse.tenantAdmin.response.liveRoster.LiveRosterResponse;
import com.company.GymVerse.tenantAdmin.response.members.ApproveJoinRequestResponse;
import com.company.GymVerse.tenantAdmin.response.members.JoinRequestItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberListItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MembersSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanFilterOptionsResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlansSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainerItemResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainersSummaryResponse;
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
    private TrainersSummaryResponse trainersSummaryResponse;
    private PagedResponse<TrainerItemResponse> trainersPageResponse;
    private FinanceOverviewResponse financeOverviewResponse;
    private PagedResponse<ExpenseTransactionResponse> expenseTransactionsPageResponse;
    private ExpenseTransactionResponse expenseTransactionResponse;
    private AdminDashboardResponse adminDashboardResponse;
    private LiveRosterResponse liveRosterResponse;
}
