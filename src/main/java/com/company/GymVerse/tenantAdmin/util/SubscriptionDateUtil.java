package com.company.GymVerse.tenantAdmin.util;

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

        if (currentDate.isAfter(endDate)) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(currentDate, endDate);
    }

    /**
     * Overloaded convenience method calculating remaining days relative to today.
     *
     * @param endDate the subscription end date (must not be null)
     * @return remaining days relative to today
     */
    public static long calculateDaysRemaining(final LocalDate endDate) {
        return calculateDaysRemaining(endDate, LocalDate.now());
    }

    /**
     * Evaluates whether a trial or subscription remains active on a given date.
     *
     * @param endDate     the subscription end date (must not be null)
     * @param currentDate the reference date (must not be null)
     * @return true if the current date is on or before the end date; false otherwise
     */
    public static boolean isTrialActive(final LocalDate endDate, final LocalDate currentDate) {
        Objects.requireNonNull(endDate, "End date must not be null");
        Objects.requireNonNull(currentDate, "Current date must not be null");

        return !currentDate.isAfter(endDate);
    }

    /**
     * Overloaded convenience method checking if a subscription is active today.
     *
     * @param endDate the subscription end date (must not be null)
     * @return true if active today; false otherwise
     */
    public static boolean isTrialActive(final LocalDate endDate) {
        return isTrialActive(endDate, LocalDate.now());
    }

    /**
     * Evaluates whether a trial is expiring within the specified threshold days.
     *
     * @param endDate       the subscription end date (must not be null)
     * @param currentDate   the reference date (must not be null)
     * @param thresholdDays warning threshold in days (e.g. 3 days)
     * @return true if active and remaining days <= thresholdDays
     */
    public static boolean isExpiringSoon(final LocalDate endDate, final LocalDate currentDate, final int thresholdDays) {
        if (!isTrialActive(endDate, currentDate)) {
            return false;
        }
        long daysLeft = calculateDaysRemaining(endDate, currentDate);
        return daysLeft <= thresholdDays;
    }
}
