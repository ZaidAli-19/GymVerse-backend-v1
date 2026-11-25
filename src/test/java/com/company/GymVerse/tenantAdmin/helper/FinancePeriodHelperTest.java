package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.tenantAdmin.request.enums.FinancePeriodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancePeriodHelperTest {

    @Test
    @DisplayName("Resolves WEEKLY, MONTHLY, and YEARLY windows accurately")
    void resolveWindow_AllPeriods() {
        LocalDate wednesday = LocalDate.of(2026, 11, 25);

        FinancePeriodHelper.PeriodWindow weekly = FinancePeriodHelper.resolveWindow(FinancePeriodType.WEEKLY, wednesday);
        assertThat(weekly.startDate()).isEqualTo(LocalDate.of(2026, 11, 23)); // Monday
        assertThat(weekly.endDate()).isEqualTo(LocalDate.of(2026, 11, 29));   // Sunday

        FinancePeriodHelper.PeriodWindow monthly = FinancePeriodHelper.resolveWindow(FinancePeriodType.MONTHLY, wednesday);
        assertThat(monthly.startDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(monthly.endDate()).isEqualTo(LocalDate.of(2026, 11, 30));

        FinancePeriodHelper.PeriodWindow yearly = FinancePeriodHelper.resolveWindow(FinancePeriodType.YEARLY, wednesday);
        assertThat(yearly.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(yearly.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
