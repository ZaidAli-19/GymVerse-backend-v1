package com.company.GymVerse.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Member-specific attendance summary and today's live check-in status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAttendanceSnapshot {

    private long totalVisitCount;
    private int currentStreak;
    private boolean checkedInToday;
    private Instant checkInTime;
    private Long elapsedMinutes;
}
