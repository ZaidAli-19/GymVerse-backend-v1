package com.company.GymVerse.tenantAdmin.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionDateUtilTest {

    @Test
    @DisplayName("calculateTrialEndDate correctly adds duration days to start date")
    void calculateTrialEndDateSuccess() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = SubscriptionDateUtil.calculateTrialEndDate(start, 14);

        assertThat(end).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("calculateTrialEndDate throws exception when startDate is null")
    void calculateTrialEndDateNullDateThrows() {
        assertThatThrownBy(() -> SubscriptionDateUtil.calculateTrialEndDate(null, 14))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Start date must not be null");
    }

    @Test
    @DisplayName("calculateTrialEndDate throws exception when duration is negative")
    void calculateTrialEndDateNegativeDurationThrows() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> SubscriptionDateUtil.calculateTrialEndDate(start, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration days must not be negative");
    }

    @Test
    @DisplayName("calculateTrialEndDate with 0 days returns same date")
    void calculateTrialEndDateZeroDays() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = SubscriptionDateUtil.calculateTrialEndDate(start, 0);

        assertThat(end).isEqualTo(start);
    }

    @Test
    @DisplayName("calculateDaysRemaining returns exact positive days when before end date")
    void calculateDaysRemainingPositive() {
        LocalDate current = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 15);

        long remaining = SubscriptionDateUtil.calculateDaysRemaining(end, current);
        assertThat(remaining).isEqualTo(14L);
    }

    @Test
    @DisplayName("calculateDaysRemaining returns 0 when current date equals end date")
    void calculateDaysRemainingZeroOnSameDay() {
        LocalDate date = LocalDate.of(2026, 1, 15);

        long remaining = SubscriptionDateUtil.calculateDaysRemaining(date, date);
        assertThat(remaining).isEqualTo(0L);
    }

    @Test
    @DisplayName("calculateDaysRemaining returns 0 when subscription has expired")
    void calculateDaysRemainingExpiredReturnsZero() {
        LocalDate end = LocalDate.of(2026, 1, 15);
        LocalDate current = LocalDate.of(2026, 1, 20);

        long remaining = SubscriptionDateUtil.calculateDaysRemaining(end, current);
        assertThat(remaining).isEqualTo(0L);
    }

    @Test
    @DisplayName("isTrialActive returns true on and before end date, false after")
    void isTrialActiveVerification() {
        LocalDate end = LocalDate.of(2026, 1, 15);

        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 14))).isTrue();
        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 15))).isTrue();
        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 16))).isFalse();
    }

    @Test
    @DisplayName("isExpiringSoon correctly detects threshold warning window")
    void isExpiringSoonVerification() {
        LocalDate end = LocalDate.of(2026, 1, 15);

        // 5 days remaining (threshold 3) -> false
        assertThat(SubscriptionDateUtil.isExpiringSoon(end, LocalDate.of(2026, 1, 10), 3)).isFalse();

        // 3 days remaining (threshold 3) -> true
        assertThat(SubscriptionDateUtil.isExpiringSoon(end, LocalDate.of(2026, 1, 12), 3)).isTrue();

        // 1 day remaining (threshold 3) -> true
        assertThat(SubscriptionDateUtil.isExpiringSoon(end, LocalDate.of(2026, 1, 14), 3)).isTrue();

        // Expired (threshold 3) -> false
        assertThat(SubscriptionDateUtil.isExpiringSoon(end, LocalDate.of(2026, 1, 16), 3)).isFalse();
    }
}
