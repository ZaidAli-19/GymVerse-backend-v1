package com.company.GymVerse.tenantAdmin.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Expiring-soon and live-expiring membership card returned in AdminDashboardResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardExpiryCardResponse {

    private long expiringSoonCount;
    private int expiringSoonWindowDays;
    private long liveExpiringSoonCount;
}
