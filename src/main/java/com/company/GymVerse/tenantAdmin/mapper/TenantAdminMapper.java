package com.company.GymVerse.tenantAdmin.mapper;

import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.tenant.Expense;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.Trainer;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.tenantAdmin.response.PagedResponse;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.finance.ExpenseTransactionResponse;
import com.company.GymVerse.tenantAdmin.response.finance.FinanceOverviewResponse;
import com.company.GymVerse.tenantAdmin.response.members.ApproveJoinRequestResponse;
import com.company.GymVerse.tenantAdmin.response.members.JoinRequestItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberListItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MembersSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanFilterOptionsResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanTierOptionResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlansSummaryResponse;
import com.company.GymVerse.tenantAdmin.response.plans.TierCountResponse;
import com.company.GymVerse.tenantAdmin.response.subscription.TenantSubscriptionDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainerItemResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainersSummaryResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Handwritten mapper contract for tenant administration domain and response models.
 */
public interface TenantAdminMapper {

    TenantSubscriptionDetailsResponse toSubscriptionDetails(GymSubscription subscription, Long daysRemaining, boolean isTrial);

    TenantAdminResponse toResponse(String message, TenantSubscriptionDetailsResponse details);

    PlanTierOptionResponse toPlanTierOption(PlanTier tier);

    PlanItemResponse toPlanItem(MembershipPlan plan);

    TenantAdminResponse toPlansSummaryResponse(String message, long totalActivePlans, long totalPlans, List<TierCountResponse> tierBreakdown);

    TenantAdminResponse toPlanFilterOptionsResponse(String message, List<PlanTierOptionResponse> tiers, List<String> facilities, List<String> customThemes);

    TenantAdminResponse toPagedPlansResponse(String message, Page<MembershipPlan> planPage);

    TenantAdminResponse toPlanItemResponse(String message, int httpStatus, PlanItemResponse item);

    JoinRequestItemResponse toJoinRequestItem(JoinRequest joinRequest);

    TenantAdminResponse toPagedJoinRequestsResponse(String message, Page<JoinRequest> joinRequestPage);

    TenantAdminResponse toJoinRequestItemResponse(String message, JoinRequestItemResponse item);

    ApproveJoinRequestResponse toApproveJoinRequestResponse(
            JoinRequest joinRequest,
            Member member,
            MemberMembership membership,
            PaymentMode paymentMode
    );

    TenantAdminResponse toApproveJoinRequestAdminResponse(String message, ApproveJoinRequestResponse payload);

    TenantAdminResponse toMembersSummaryResponse(
            String message,
            long totalMembersCount,
            long liveCount,
            long expiredCount,
            long trialCount
    );

    MemberListItemResponse toMemberListItem(
            Member member,
            MemberMembership membership,
            boolean checkedInToday,
            LocalDate today
    );

    TenantAdminResponse toPagedMembersResponse(
            String message,
            Page<Member> memberPage,
            List<MemberListItemResponse> items
    );

    MemberDetailsResponse toMemberDetails(
            Member member,
            MemberMembership membership,
            BigDecimal totalAmountPaid,
            MemberAttendanceSnapshot attendanceSnapshot,
            LocalDate today
    );

    TenantAdminResponse toMemberDetailsAdminResponse(String message, MemberDetailsResponse details);

    TenantAdminResponse toTrainersSummaryResponse(
            String message,
            long totalTrainersCount,
            long activeTrainersCount,
            long activeLiveCount,
            double avgAttendanceRate
    );

    TrainerItemResponse toTrainerItem(Trainer trainer, long assignedMembersCount);

    TenantAdminResponse toPagedTrainersResponse(
            String message,
            Page<Trainer> trainerPage,
            List<TrainerItemResponse> items
    );

    TenantAdminResponse toFinanceOverviewAdminResponse(String message, FinanceOverviewResponse overview);

    ExpenseTransactionResponse toExpenseTransaction(Expense expense);

    TenantAdminResponse toPagedExpensesResponse(String message, Page<Expense> expensePage);

    TenantAdminResponse toExpenseTransactionAdminResponse(
            String message,
            int httpStatus,
            ExpenseTransactionResponse item
    );

    default TrainersSummaryResponse buildTrainersSummary(
            long totalTrainersCount,
            long activeTrainersCount,
            long activeLiveCount,
            double avgAttendanceRate
    ) {
        return TrainersSummaryResponse.builder()
                .totalTrainersCount(totalTrainersCount)
                .activeTrainersCount(activeTrainersCount)
                .activeLiveCount(activeLiveCount)
                .avgAttendanceRate(avgAttendanceRate)
                .build();
    }

    default MembersSummaryResponse buildMembersSummary(
            long totalMembersCount,
            long liveCount,
            long expiredCount,
            long trialCount
    ) {
        return MembersSummaryResponse.builder()
                .totalMembersCount(totalMembersCount)
                .liveCount(liveCount)
                .expiredCount(expiredCount)
                .trialCount(trialCount)
                .build();
    }

    default <T> PagedResponse<T> toPagedResponse(Page<?> page, List<T> content) {
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    default PlansSummaryResponse buildPlansSummary(long totalActivePlans, long totalPlans, List<TierCountResponse> tierBreakdown) {
        return PlansSummaryResponse.builder()
                .totalActivePlans(totalActivePlans)
                .totalPlans(totalPlans)
                .tierBreakdown(tierBreakdown)
                .build();
    }

    default PlanFilterOptionsResponse buildPlanFilterOptions(
            List<PlanTierOptionResponse> tiers,
            List<String> facilities,
            List<String> customThemes
    ) {
        return PlanFilterOptionsResponse.builder()
                .tiers(tiers)
                .facilities(facilities)
                .customThemes(customThemes)
                .build();
    }
}
