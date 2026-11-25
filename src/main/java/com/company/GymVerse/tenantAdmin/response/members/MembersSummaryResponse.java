package com.company.GymVerse.tenantAdmin.response.members;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Members mini-dashboard counts returned by FETCH_MEMBERS_SUMMARY.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembersSummaryResponse {

    private long totalMembersCount;
    private long liveCount;
    private long expiredCount;
    private long trialCount;
}
