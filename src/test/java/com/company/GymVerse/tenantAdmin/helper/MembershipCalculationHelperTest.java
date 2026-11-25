package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.tenant.enums.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipCalculationHelperTest {

    @Test
    @DisplayName("Computes final price accurately for PERCENTAGE and FLAT discounts and no discount")
    void computeFinalPrice_Cases() {
        assertThat(MembershipCalculationHelper.computeFinalPrice(new BigDecimal("100.00"), null, null))
                .isEqualByComparingTo("100.00");

        assertThat(MembershipCalculationHelper.computeFinalPrice(new BigDecimal("200.00"), DiscountType.PERCENTAGE, new BigDecimal("25")))
                .isEqualByComparingTo("150.00");

        assertThat(MembershipCalculationHelper.computeFinalPrice(new BigDecimal("200.00"), DiscountType.FLAT, new BigDecimal("40.00")))
                .isEqualByComparingTo("160.00");
    }

    @Test
    @DisplayName("Rejects invalid discount bounds")
    void computeFinalPrice_InvalidInputs() {
        assertThatThrownBy(() -> MembershipCalculationHelper.computeFinalPrice(
                new BigDecimal("100.00"), DiscountType.PERCENTAGE, new BigDecimal("110")))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> MembershipCalculationHelper.computeFinalPrice(
                new BigDecimal("100.00"), DiscountType.FLAT, new BigDecimal("150")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Supports both date-based and flag-based active trial evaluation")
    void isTrialActive_DualSupport() {
        LocalDate today = LocalDate.of(2026, 11, 25);

        // Date-based trial active
        assertThat(MembershipCalculationHelper.isTrialActive(today.plusDays(3), false, null, false, today))
                .isTrue();

        // Flag-based trial active
        assertThat(MembershipCalculationHelper.isTrialActive(null, true, today.plusDays(5), true, today))
                .isTrue();

        // Expired date & expired flag
        assertThat(MembershipCalculationHelper.isTrialActive(today.minusDays(1), true, today.minusDays(1), true, today))
                .isFalse();
    }

    @Test
    @DisplayName("Computes safe percentage without divide-by-zero")
    void safePercentage_ZeroGuard() {
        assertThat(MembershipCalculationHelper.safePercentage(new BigDecimal("50"), BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
        assertThat(MembershipCalculationHelper.safePercentage(new BigDecimal("25"), new BigDecimal("100")))
                .isEqualByComparingTo("25.00");
    }
}
