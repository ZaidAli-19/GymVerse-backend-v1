package com.company.GymVerse.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trainer-focused live presence and attendance rate summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerAttendanceSummary {

    private long activeLiveTrainersCount;
    private double avgAttendanceRate;
}
