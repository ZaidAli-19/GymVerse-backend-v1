package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reusable calculations for plan discounts, membership expiry countdowns, and trial classification.
 */
public final class MembershipCalculationHelper {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private MembershipCalculationHelper() {
    }

    /**
     * Computes final membership price from base price and optional discount.
     */
    public static BigDecimal computeFinalPrice(
            final BigDecimal basePrice,
            final DiscountType discountType,
            final BigDecimal discountValue
    ) {
        if (basePrice == null) {
            throw new BadRequestException("basePrice must not be null");
        }
        if (discountType == null || discountValue == null) {
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discounted;
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) < 0 || discountValue.compareTo(ONE_HUNDRED) > 0) {
                throw new BadRequestException("Percentage discount must be between 0 and 100");
            }
            BigDecimal discountAmount = basePrice.multiply(discountValue)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            discounted = basePrice.subtract(discountAmount);
        } else if (discountType == DiscountType.FLAT) {
            if (discountValue.compareTo(BigDecimal.ZERO) < 0 || discountValue.compareTo(basePrice) > 0) {
                throw new BadRequestException("Flat discount must be between 0 and basePrice");
            }
            discounted = basePrice.subtract(discountValue);
        } else {
            throw new BadRequestException("Unsupported discountType: " + discountType);
        }

        return discounted.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates non-negative days remaining until endDate.
     */
    public static long calculateDaysRemaining(final LocalDate endDate, final LocalDate today) {
        if (endDate == null || today == null || endDate.isBefore(today)) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(today, endDate);
    }

    /**
     * Evaluates whether a member is currently on trial (supporting BOTH date and flag semantics).
     */
    public static boolean isTrialActive(
            final LocalDate memberTrialEndDate,
            final boolean membershipTrialFlag,
            final LocalDate membershipEndDate,
            final boolean membershipActive,
            final LocalDate today
    ) {
        boolean dateTrialActive = memberTrialEndDate != null && !memberTrialEndDate.isBefore(today);
        boolean flagTrialActive = membershipTrialFlag && membershipActive
                && membershipEndDate != null && !membershipEndDate.isBefore(today);
        return dateTrialActive || flagTrialActive;
    }

    /**
     * Evaluates dual trial classification (members.trial_end_date or active trial membership/plan flag).
     */
    public static boolean isTrialActive(
            final LocalDate memberTrialEndDate,
            final MemberMembership membership,
            final LocalDate today
    ) {
        boolean membershipTrialFlag = membership != null
                && (membership.isTrial() || (membership.getPlan() != null && membership.getPlan().isTrial()));
        LocalDate membershipEndDate = membership != null ? membership.getEndDate() : null;
        boolean membershipActive = membership != null && membership.isActive();
        return isTrialActive(memberTrialEndDate, membershipTrialFlag, membershipEndDate, membershipActive, today);
    }

    /**
     * Resolves a member's operational status (TRIAL, ACTIVE, or EXPIRED) consistently across directory and details views.
     */
    public static MemberMembershipStatus resolveMemberStatus(
            final LocalDate memberTrialEndDate,
            final MemberMembership membership,
            final LocalDate today
    ) {
        if (isTrialActive(memberTrialEndDate, membership, today)) {
            return MemberMembershipStatus.TRIAL;
        }
        boolean hasActiveMembership = membership != null
                && membership.isActive()
                && membership.getEndDate() != null
                && !membership.getEndDate().isBefore(today);
        return hasActiveMembership ? MemberMembershipStatus.ACTIVE : MemberMembershipStatus.EXPIRED;
    }

    /**
     * Resolves non-negative days remaining until active membership expiry or trial end date.
     */
    public static long resolveMemberDaysRemaining(
            final LocalDate memberTrialEndDate,
            final MemberMembership membership,
            final LocalDate today
    ) {
        if (membership != null
                && membership.isActive()
                && membership.getEndDate() != null
                && !membership.getEndDate().isBefore(today)) {
            return calculateDaysRemaining(membership.getEndDate(), today);
        }
        if (memberTrialEndDate != null && !memberTrialEndDate.isBefore(today)) {
            return calculateDaysRemaining(memberTrialEndDate, today);
        }
        return 0L;
    }

    /**
     * Computes percentage safely (returns 0.00 when denominator is null or zero).
     */
    public static BigDecimal safePercentage(final BigDecimal numerator, final BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.multiply(ONE_HUNDRED)
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
