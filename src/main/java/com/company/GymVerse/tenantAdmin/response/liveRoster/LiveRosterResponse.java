package com.company.GymVerse.tenantAdmin.response.liveRoster;

import com.company.GymVerse.tenantAdmin.response.PagedResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Live Roster summary and paginated checked-in member address book returned by FETCH_LIVE_ROSTER.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveRosterResponse {

    private long totalLiveCount;
    private double avgTimeSpentMinutes;
    private long liveExpiredCount;
    private long liveTrialCount;
    private PagedResponse<LiveRosterMemberResponse> roster;
}
