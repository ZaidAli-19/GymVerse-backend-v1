package com.company.GymVerse.tenantAdmin.response.trainers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trainers & Staff mini-dashboard metrics returned by FETCH_TRAINERS_SUMMARY.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainersSummaryResponse {

    private long totalTrainersCount;
    private long activeTrainersCount;
    private long activeLiveCount;
    private double avgAttendanceRate;
}
