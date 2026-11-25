package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * Date-range resolver for WEEKLY, MONTHLY, and YEARLY Finance aggregations.
 */
public final class FinancePeriodHelper {

    private FinancePeriodHelper() {
    }

    public record PeriodWindow(
            LocalDate startDate,
            LocalDate endDate,
            Instant startInstant,
            Instant endInstant
    ) {
    }

    /**
     * Resolves inclusive calendar window [startDate, endDate] and UTC Instant bounds.
     */
    public static PeriodWindow resolveWindow(final FinancePeriodType periodType, final LocalDate referenceDate) {
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now(ZoneOffset.UTC);
        FinancePeriodType type = periodType != null ? periodType : FinancePeriodType.MONTHLY;

        LocalDate start;
        LocalDate end;

        switch (type) {
            case WEEKLY -> {
                start = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case YEARLY -> {
                start = ref.withDayOfYear(1);
                end = ref.withDayOfYear(ref.lengthOfYear());
            }
            case MONTHLY -> {
                start = ref.withDayOfMonth(1);
                end = ref.withDayOfMonth(ref.lengthOfMonth());
            }
            default -> {
                start = ref.withDayOfMonth(1);
                end = ref.withDayOfMonth(ref.lengthOfMonth());
            }
        }

        Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
        return new PeriodWindow(start, end, startInstant, endInstant);
    }
}
