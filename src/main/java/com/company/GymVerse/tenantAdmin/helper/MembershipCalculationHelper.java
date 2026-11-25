package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.tenant.enums.DiscountType;

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
