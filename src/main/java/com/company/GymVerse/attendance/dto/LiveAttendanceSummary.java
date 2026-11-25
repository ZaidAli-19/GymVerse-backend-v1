package com.company.GymVerse.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable live-traffic snapshot shared across Admin, Member, Trainer, and Staff views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAttendanceSummary {

    private long liveMembersCount;
    private long liveExpiringSoonCount;
    private long liveExpiredCount;
    private long liveTrialCount;
    private double avgTimeSpentMinutes;
}
