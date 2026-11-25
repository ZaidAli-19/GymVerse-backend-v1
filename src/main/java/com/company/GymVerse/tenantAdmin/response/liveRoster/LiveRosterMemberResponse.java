package com.company.GymVerse.tenantAdmin.response.liveRoster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Single checked-in member item returned inside LiveRosterResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRosterMemberResponse {

    private Long memberId;
    private String gymId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String membershipStatus;
    private String tierName;
    private String themeKey;
    private Instant checkInTime;
    private Long elapsedMinutes;
}
