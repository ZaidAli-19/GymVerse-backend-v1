package com.company.GymVerse.user.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Utility functions for subscription and free trial lifecycle calculations.
 */
public final class SubscriptionDateUtil {

    private SubscriptionDateUtil() {
        // Private constructor for utility class
    }

    /**
     * Calculates the trial expiration date from a given start date and duration in days.
     *
     * @param startDate    the trial start date (must not be null)
     * @param durationDays the duration in days (must not be negative)
     * @return the end date of the trial
     */
    public static LocalDate calculateTrialEndDate(final LocalDate startDate, final int durationDays) {
        Objects.requireNonNull(startDate, "Start date must not be null");
        if (durationDays < 0) {
            throw new IllegalArgumentException("Duration days must not be negative");
        }
        return startDate.plusDays(durationDays);
    }

    /**
     * Overloaded convenience method calculating trial end date starting from today.
     *
     * @param durationDays the duration in days (must not be negative)
     * @return the end date of the trial
     */
    public static LocalDate calculateTrialEndDate(final int durationDays) {
        return calculateTrialEndDate(LocalDate.now(), durationDays);
    }

    /**
     * Calculates the remaining days between the current date and subscription end date.
     * Returns 0 if the subscription has already expired.
     *
     * @param endDate     the subscription end date (must not be null)
     * @param currentDate the reference evaluation date (must not be null)
     * @return remaining days (non-negative)
     */
    public static long calculateDaysRemaining(final LocalDate endDate, final LocalDate currentDate) {
        Objects.requireNonNull(endDate, "End date must not be null");
        Objects.requireNonNull(currentDate, "Current date must not be null");
        long days = ChronoUnit.DAYS.between(currentDate, endDate);
        return Math.max(0, days);
    }

    /**
     * Overloaded convenience method calculating remaining days between today and subscription end date.
     *
     * @param endDate the subscription end date (must not be null)
     * @return remaining days (non-negative)
     */
    public static long calculateDaysRemaining(final LocalDate endDate) {
        return calculateDaysRemaining(endDate, LocalDate.now());
    }

    /**
     * Determines whether a trial subscription is active given an end date and reference date.
     *
     * @param endDate     the subscription end date (must not be null)
     * @param currentDate the reference evaluation date (must not be null)
     * @return true if currentDate is on or before endDate, false otherwise
     */
    public static boolean isTrialActive(final LocalDate endDate, final LocalDate currentDate) {
        Objects.requireNonNull(endDate, "End date must not be null");
        Objects.requireNonNull(currentDate, "Current date must not be null");
        return !currentDate.isAfter(endDate);
    }

    /**
     * Overloaded convenience method evaluating trial activity against today.
     *
     * @param endDate the subscription end date (must not be null)
     * @return true if today is on or before endDate, false otherwise
     */
    public static boolean isTrialActive(final LocalDate endDate) {
        return isTrialActive(endDate, LocalDate.now());
    }
}
