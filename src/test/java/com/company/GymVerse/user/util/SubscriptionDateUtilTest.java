package com.company.GymVerse.user.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SubscriptionDateUtil Unit Tests")
class SubscriptionDateUtilTest {

    @Test
    @DisplayName("calculateTrialEndDate adds duration days to start date")
    void calculateTrialEndDate_WithStartDate() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = SubscriptionDateUtil.calculateTrialEndDate(start, 14);

        assertThat(end).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("calculateTrialEndDate starting today returns future date")
    void calculateTrialEndDate_Today() {
        LocalDate end = SubscriptionDateUtil.calculateTrialEndDate(14);
        assertThat(end).isEqualTo(LocalDate.now().plusDays(14));
    }

    @Test
    @DisplayName("calculateTrialEndDate throws IllegalArgumentException on negative duration")
    void calculateTrialEndDate_NegativeDuration_Throws() {
        assertThatThrownBy(() -> SubscriptionDateUtil.calculateTrialEndDate(LocalDate.now(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration days must not be negative");
    }

    @Test
    @DisplayName("calculateDaysRemaining returns exact remaining days when future")
    void calculateDaysRemaining_Future() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 15);

        long remaining = SubscriptionDateUtil.calculateDaysRemaining(end, today);
        assertThat(remaining).isEqualTo(14);
    }

    @Test
    @DisplayName("calculateDaysRemaining returns 0 when expired")
    void calculateDaysRemaining_Expired() {
        LocalDate today = LocalDate.of(2026, 1, 20);
        LocalDate end = LocalDate.of(2026, 1, 15);

        long remaining = SubscriptionDateUtil.calculateDaysRemaining(end, today);
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateDaysRemaining against today returns non-negative days")
    void calculateDaysRemaining_Today() {
        LocalDate end = LocalDate.now().plusDays(7);
        long remaining = SubscriptionDateUtil.calculateDaysRemaining(end);
        assertThat(remaining).isEqualTo(7);
    }

    @Test
    @DisplayName("isTrialActive returns true on or before end date")
    void isTrialActive_Valid() {
        LocalDate end = LocalDate.of(2026, 1, 15);

        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 15))).isTrue();
    }

    @Test
    @DisplayName("isTrialActive returns false after end date")
    void isTrialActive_Expired() {
        LocalDate end = LocalDate.of(2026, 1, 15);
        assertThat(SubscriptionDateUtil.isTrialActive(end, LocalDate.of(2026, 1, 16))).isFalse();
    }

    @Test
    @DisplayName("isTrialActive against today returns true for future end date")
    void isTrialActive_Today() {
        LocalDate futureEnd = LocalDate.now().plusDays(1);
        assertThat(SubscriptionDateUtil.isTrialActive(futureEnd)).isTrue();
    }
}
