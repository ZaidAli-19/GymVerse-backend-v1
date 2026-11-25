package com.company.GymVerse.tenantAdmin.mapper;

import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.tenantAdmin.helper.MembershipCalculationHelper;
import com.company.GymVerse.tenantAdmin.response.PagedResponse;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Handwritten implementation of {@link TenantAdminMapper} avoiding runtime reflection or code-generation overhead.
 */
@Component
public class TenantAdminMapperImpl implements TenantAdminMapper {

    @Override
    public TenantSubscriptionDetailsResponse toSubscriptionDetails(
            final GymSubscription subscription,
            final Long daysRemaining,
            final boolean isTrial
    ) {
        if (subscription == null) {
            return null;
        }

        GymTenant tenant = subscription.getGymTenant();
        SaasSubscriptionPlan plan = subscription.getPlan();

        return TenantSubscriptionDetailsResponse.builder()
                .subscriptionId(subscription.getId())
                .tenantId(tenant != null ? tenant.getId() : null)
                .gymCode(tenant != null ? tenant.getGymCode() : null)
                .gymName(tenant != null ? tenant.getGymName() : null)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getName() : null)
                .planDescription(plan != null ? plan.getDescription() : null)
                .planPrice(plan != null ? plan.getPrice() : null)
                .pricePaid(subscription.getPricePaid())
                .billingCycle(subscription.getBillingCycle())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .isTrial(isTrial)
                .maxMembers(plan != null ? plan.getMaxMembers() : null)
                .maxTrainers(plan != null ? plan.getMaxTrainers() : null)
                .analyticsEnabled(plan != null && plan.isAnalyticsEnabled())
                .multiTrainerEnabled(plan != null && plan.isMultiTrainerEnabled())
                .customBrandingEnabled(plan != null && plan.isCustomBrandingEnabled())
                .build();
    }

    @Override
    public TenantAdminResponse toResponse(
            final String message,
            final TenantSubscriptionDetailsResponse details
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .tenantSubscriptionDetailsResponse(details)
                .build();
    }

    @Override
    public PlanTierOptionResponse toPlanTierOption(final PlanTier tier) {
        if (tier == null) {
            return null;
        }
        return PlanTierOptionResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .themeKey(tier.getThemeKey())
                .defaultTier(tier.isDefaultTier())
                .build();
    }

    @Override
    public PlanItemResponse toPlanItem(final MembershipPlan plan) {
        if (plan == null) {
            return null;
        }

        PlanTier tier = plan.getPlanTier();
        List<String> facilityNames = plan.getFacilities() == null
                ? Collections.emptyList()
                : plan.getFacilities().stream()
                .map(GymFacility::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();

        boolean hasOffer = plan.getDiscountType() != null
                && plan.getDiscountValue() != null
                && plan.getDiscountValue().compareTo(BigDecimal.ZERO) > 0;

        return PlanItemResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .tierId(tier != null ? tier.getId() : null)
                .tierName(tier != null ? tier.getName() : null)
                .themeKey(tier != null ? tier.getThemeKey() : null)
                .facilities(facilityNames)
                .basePrice(plan.getBasePrice())
                .discountType(plan.getDiscountType())
                .discountValue(plan.getDiscountValue())
                .finalPrice(plan.getFinalPrice())
                .hasOffer(hasOffer)
                .durationDays(plan.getDurationDays())
                .trial(plan.isTrial())
                .active(plan.isActive())
                .build();
    }

    @Override
    public TenantAdminResponse toPlansSummaryResponse(
            final String message,
            final long totalActivePlans,
            final long totalPlans,
            final List<TierCountResponse> tierBreakdown
    ) {
        PlansSummaryResponse summary = buildPlansSummary(totalActivePlans, totalPlans, tierBreakdown);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .plansSummaryResponse(summary)
                .build();
    }

    @Override
    public TenantAdminResponse toPlanFilterOptionsResponse(
            final String message,
            final List<PlanTierOptionResponse> tiers,
            final List<String> facilities,
            final List<String> customThemes
    ) {
        PlanFilterOptionsResponse options = buildPlanFilterOptions(tiers, facilities, customThemes);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .planFilterOptionsResponse(options)
                .build();
    }

    @Override
    public TenantAdminResponse toPagedPlansResponse(
            final String message,
            final Page<MembershipPlan> planPage
    ) {
        List<PlanItemResponse> items = planPage.getContent().stream()
                .map(this::toPlanItem)
                .toList();
        PagedResponse<PlanItemResponse> paged = toPagedResponse(planPage, items);

        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .plansPageResponse(paged)
                .build();
    }

    @Override
    public TenantAdminResponse toPlanItemResponse(
            final String message,
            final int httpStatus,
            final PlanItemResponse item
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(httpStatus)
                .timestamp(Instant.now())
                .planItemResponse(item)
                .build();
    }

    @Override
    public JoinRequestItemResponse toJoinRequestItem(final JoinRequest joinRequest) {
        if (joinRequest == null) {
            return null;
        }
        return JoinRequestItemResponse.builder()
                .id(joinRequest.getId())
                .centralUserId(joinRequest.getCentralUserId())
                .email(joinRequest.getEmail())
                .firstName(joinRequest.getFirstName())
                .lastName(joinRequest.getLastName())
                .phoneNumber(joinRequest.getPhoneNumber())
                .status(joinRequest.getStatus())
                .requestNotes(joinRequest.getRequestNotes())
                .adminNotes(joinRequest.getAdminNotes())
                .createdAt(joinRequest.getCreatedAt())
                .reviewedAt(joinRequest.getReviewedAt())
                .reviewedByAdminId(joinRequest.getReviewedByAdmin() != null ? joinRequest.getReviewedByAdmin().getId() : null)
                .build();
    }

    @Override
    public TenantAdminResponse toPagedJoinRequestsResponse(
            final String message,
            final Page<JoinRequest> joinRequestPage
    ) {
        List<JoinRequestItemResponse> items = joinRequestPage.getContent().stream()
                .map(this::toJoinRequestItem)
                .toList();
        PagedResponse<JoinRequestItemResponse> paged = toPagedResponse(joinRequestPage, items);

        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .joinRequestsPageResponse(paged)
                .build();
    }

    @Override
    public TenantAdminResponse toJoinRequestItemResponse(
            final String message,
            final JoinRequestItemResponse item
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .joinRequestItemResponse(item)
                .build();
    }

    @Override
    public ApproveJoinRequestResponse toApproveJoinRequestResponse(
            final JoinRequest joinRequest,
            final Member member,
            final MemberMembership membership,
            final PaymentMode paymentMode
    ) {
        BigDecimal finalPrice = membership.getFinalPrice();
        BigDecimal paidAmount = membership.getPaidAmount();
        BigDecimal remainingDue = finalPrice.subtract(paidAmount).max(BigDecimal.ZERO);

        return ApproveJoinRequestResponse.builder()
                .joinRequestId(joinRequest.getId())
                .memberId(member.getId())
                .membershipId(membership.getId())
                .gymId(member.getGymId())
                .status(joinRequest.getStatus())
                .paymentStatus(membership.getPaymentStatus())
                .paymentMode(paymentMode)
                .paidAmount(paidAmount)
                .finalPrice(finalPrice)
                .remainingDue(remainingDue)
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .build();
    }

    @Override
    public TenantAdminResponse toApproveJoinRequestAdminResponse(
            final String message,
            final ApproveJoinRequestResponse payload
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .approveJoinRequestResponse(payload)
                .build();
    }

    @Override
    public TenantAdminResponse toMembersSummaryResponse(
            final String message,
            final long totalMembersCount,
            final long liveCount,
            final long expiredCount,
            final long trialCount
    ) {
        MembersSummaryResponse summary = buildMembersSummary(totalMembersCount, liveCount, expiredCount, trialCount);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .membersSummaryResponse(summary)
                .build();
    }

    @Override
    public MemberListItemResponse toMemberListItem(
            final Member member,
            final MemberMembership membership,
            final boolean checkedInToday,
            final LocalDate today
    ) {
        if (member == null) {
            return null;
        }
        TenantUser user = member.getUser();
        MembershipPlan plan = membership != null ? membership.getPlan() : null;
        PlanTier tier = plan != null ? plan.getPlanTier() : null;
        MemberMembershipStatus membershipStatus = MembershipCalculationHelper.resolveMemberStatus(
                member.getTrialEndDate(), membership, today
        );
        long daysRemaining = MembershipCalculationHelper.resolveMemberDaysRemaining(
                member.getTrialEndDate(), membership, today
        );

        return MemberListItemResponse.builder()
                .memberId(member.getId())
                .gymId(member.getGymId())
                .fullName(buildFullName(user))
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .planName(plan != null ? plan.getName() : null)
                .tier(tier != null ? tier.getName() : null)
                .themeKey(tier != null ? tier.getThemeKey() : null)
                .membershipStatus(membershipStatus)
                .daysRemaining(daysRemaining)
                .paidAmount(membership != null ? membership.getPaidAmount() : null)
                .finalPrice(membership != null ? membership.getFinalPrice() : null)
                .paymentStatus(membership != null ? membership.getPaymentStatus() : null)
                .checkedInToday(checkedInToday)
                .build();
    }

    @Override
    public TenantAdminResponse toPagedMembersResponse(
            final String message,
            final Page<Member> memberPage,
            final List<MemberListItemResponse> items
    ) {
        PagedResponse<MemberListItemResponse> paged = toPagedResponse(memberPage, items);
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .membersPageResponse(paged)
                .build();
    }

    @Override
    public MemberDetailsResponse toMemberDetails(
            final Member member,
            final MemberMembership membership,
            final BigDecimal totalAmountPaid,
            final MemberAttendanceSnapshot attendanceSnapshot,
            final LocalDate today
    ) {
        if (member == null) {
            return null;
        }
        TenantUser user = member.getUser();
        MembershipPlan plan = membership != null ? membership.getPlan() : null;
        PlanTier tier = plan != null ? plan.getPlanTier() : null;
        List<String> facilities = (plan == null || plan.getFacilities() == null)
                ? Collections.emptyList()
                : plan.getFacilities().stream()
                .map(GymFacility::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();

        MemberMembershipStatus membershipStatus = MembershipCalculationHelper.resolveMemberStatus(
                member.getTrialEndDate(), membership, today
        );
        long remainingDays = MembershipCalculationHelper.resolveMemberDaysRemaining(
                member.getTrialEndDate(), membership, today
        );
        boolean isTrialMember = (membership != null
                && (membership.isTrial() || (plan != null && plan.isTrial())))
                || member.getTrialEndDate() != null;

        BigDecimal normalizedTotalPaid = (totalAmountPaid != null ? totalAmountPaid : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        MemberDetailsResponse.AttendanceActivityCard attendanceCard = MemberDetailsResponse.AttendanceActivityCard.builder()
                .totalVisitCount(attendanceSnapshot != null ? attendanceSnapshot.getTotalVisitCount() : 0L)
                .currentStreak(attendanceSnapshot != null ? attendanceSnapshot.getCurrentStreak() : 0)
                .dailyAttendance(MemberDetailsResponse.DailyAttendanceCard.builder()
                        .checkedIn(attendanceSnapshot != null && attendanceSnapshot.isCheckedInToday())
                        .checkInTime(attendanceSnapshot != null ? attendanceSnapshot.getCheckInTime() : null)
                        .elapsedMinutes(attendanceSnapshot != null ? attendanceSnapshot.getElapsedMinutes() : null)
                        .build())
                .build();

        MemberDetailsResponse.QuickStatsCard quickStatsCard = MemberDetailsResponse.QuickStatsCard.builder()
                .totalAmountPaid(normalizedTotalPaid)
                .daysLeftUntilExpiry(remainingDays)
                .build();

        MemberDetailsResponse.MembershipDetailsCard membershipCard = MemberDetailsResponse.MembershipDetailsCard.builder()
                .planName(plan != null ? plan.getName() : null)
                .tier(tier != null ? tier.getName() : null)
                .themeKey(tier != null ? tier.getThemeKey() : null)
                .facilities(facilities)
                .joinDate(member.getJoinDate())
                .startDate(membership != null ? membership.getStartDate() : null)
                .expiryDate(membership != null ? membership.getEndDate() : member.getTrialEndDate())
                .remainingDays(remainingDays)
                .finalPrice(membership != null ? membership.getFinalPrice() : null)
                .paidAmount(membership != null ? membership.getPaidAmount() : null)
                .paymentStatus(membership != null ? membership.getPaymentStatus() : null)
                .trial(isTrialMember)
                .membershipStatus(membershipStatus)
                .build();

        MemberDetailsResponse.ContactDetailsCard contactCard = MemberDetailsResponse.ContactDetailsCard.builder()
                .memberId(member.getId())
                .gymId(member.getGymId())
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .build();

        return MemberDetailsResponse.builder()
                .attendanceActivity(attendanceCard)
                .quickStats(quickStatsCard)
                .membershipDetails(membershipCard)
                .contactDetails(contactCard)
                .build();
    }

    @Override
    public TenantAdminResponse toMemberDetailsAdminResponse(
            final String message,
            final MemberDetailsResponse details
    ) {
        return TenantAdminResponse.builder()
                .message(message)
                .status(200)
                .timestamp(Instant.now())
                .memberDetailsResponse(details)
                .build();
    }

    private String buildFullName(final TenantUser user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? null : combined;
    }
}
