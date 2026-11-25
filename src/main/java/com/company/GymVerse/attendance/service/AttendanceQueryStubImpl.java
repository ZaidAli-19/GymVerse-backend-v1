package com.company.GymVerse.attendance.service;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.dto.LiveRosterEntry;
import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.attendance.dto.TrainerAttendanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Temporary stub implementation of {@link AttendanceQueryPort}.
 * Returns safe zero/empty values until the reusable geofence Attendance & Live Traffic module
 * is implemented alongside the Tenant Member module.
 */
@Service
public class AttendanceQueryStubImpl implements AttendanceQueryPort {

    @Override
    public LiveAttendanceSummary getLiveAttendanceSummary(final LocalDate expiringSoonCutoff) {
        // TODO [ATTENDANCE-MODULE]: Query live checked-in attendance records (check_out_time IS NULL)
        // and cross-reference active, expiring-soon, expired, and trial memberships.
        return LiveAttendanceSummary.builder()
                .liveMembersCount(0L)
                .liveExpiringSoonCount(0L)
                .liveExpiredCount(0L)
                .liveTrialCount(0L)
                .avgTimeSpentMinutes(0.0)
                .build();
    }

    @Override
    public MemberAttendanceSnapshot getMemberAttendanceSnapshot(final Long memberId) {
        // TODO [ATTENDANCE-MODULE]: Query attendance history for memberId to compute total visits,
        // consecutive workout streak, and today's active check-in session & elapsed minutes.
        return MemberAttendanceSnapshot.builder()
                .totalVisitCount(0L)
                .currentStreak(0)
                .checkedInToday(false)
                .checkInTime(null)
                .elapsedMinutes(null)
                .build();
    }

    @Override
    public TrainerAttendanceSummary getTrainerAttendanceSummary() {
        // TODO [ATTENDANCE-MODULE]: Query live trainer presence and historical trainer attendance rate.
        return TrainerAttendanceSummary.builder()
                .activeLiveTrainersCount(0L)
                .avgAttendanceRate(0.0)
                .build();
    }

    @Override
    public List<Long> getLiveMemberIds() {
        // TODO [ATTENDANCE-MODULE]: Return member IDs with an open attendance session for today.
        return Collections.emptyList();
    }

    @Override
    public Page<LiveRosterEntry> getLiveRoster(
            final String search,
            final String membershipFilter,
            final Pageable pageable
    ) {
        // TODO [ATTENDANCE-MODULE]: Query live checked-in members with JPA Specifications + pagination.
        return Page.empty(pageable);
    }
}
