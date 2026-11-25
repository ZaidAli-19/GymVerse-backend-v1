package com.company.GymVerse.attendance.service;

import com.company.GymVerse.models.tenant.enums.PlanTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceQueryStubAndThemeTest {

    @Test
    @DisplayName("AttendanceQueryStubImpl safely returns zero/empty values for all role-shared hooks")
    void stubReturnsSafeZeros() {
        AttendanceQueryPort stub = new AttendanceQueryStubImpl();

        assertThat(stub.getLiveAttendanceSummary(LocalDate.now().plusDays(7)).getLiveMembersCount()).isZero();
        assertThat(stub.getMemberAttendanceSnapshot(1L).getTotalVisitCount()).isZero();
        assertThat(stub.getMemberAttendanceSnapshot(1L).isCheckedInToday()).isFalse();
        assertThat(stub.getTrainerAttendanceSummary().getActiveLiveTrainersCount()).isZero();
        assertThat(stub.getLiveMemberIds()).isEmpty();
        assertThat(stub.getLiveRoster(null, "ALL", PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("PlanTheme defines ELITE->GOLDEN, LEGENDARY->PURPLE, and exactly 20 custom theme keys")
    void planThemeCatalogCheck() {
        assertThat(PlanTheme.resolveDefaultTierTheme("ELITE")).contains(PlanTheme.GOLDEN);
        assertThat(PlanTheme.resolveDefaultTierTheme("LEGENDARY")).contains(PlanTheme.PURPLE);
        assertThat(PlanTheme.resolveDefaultTierTheme("CUSTOM")).isEmpty();
        assertThat(PlanTheme.getCustomThemeKeys()).hasSize(20);
    }
}
