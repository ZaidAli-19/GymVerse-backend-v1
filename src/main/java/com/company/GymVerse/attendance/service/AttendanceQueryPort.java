package com.company.GymVerse.attendance.service;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.dto.LiveRosterEntry;
import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.attendance.dto.TrainerAttendanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Reusable cross-role attendance query contract.
 * Placed outside {@code tenantAdmin} so Member, Trainer, Staff, and Admin modules
 * can share the live gym-traffic USP uniformly once the Attendance module is implemented.
 */
public interface AttendanceQueryPort {

    /**
     * Fetches current live attendance counts across active, expiring-soon, expired, and trial members.
     *
     * @param expiringSoonCutoff inclusive upper date boundary for expiring-soon classification
     * @return live attendance summary metrics
     */
    LiveAttendanceSummary getLiveAttendanceSummary(LocalDate expiringSoonCutoff);

    /**
     * Fetches lifetime visit count, streak, and today's check-in status for a specific member.
     *
     * @param memberId tenant member ID
     * @return attendance snapshot for the member
     */
    MemberAttendanceSnapshot getMemberAttendanceSnapshot(Long memberId);

    /**
     * Fetches active live trainer count and average attendance rate.
     *
     * @return trainer attendance summary
     */
    TrainerAttendanceSummary getTrainerAttendanceSummary();

    /**
     * Returns member IDs currently checked into the gym.
     *
     * @return list of live member IDs
     */
    List<Long> getLiveMemberIds();

    /**
     * Queries a paginated live roster of members currently inside the gym.
     *
     * @param search           optional case-insensitive search term
     * @param membershipFilter optional status filter (ALL, EXPIRED, TRIAL)
     * @param pageable         pagination & sorting specification
     * @return page of live roster entries
     */
    Page<LiveRosterEntry> getLiveRoster(String search, String membershipFilter, Pageable pageable);
}
