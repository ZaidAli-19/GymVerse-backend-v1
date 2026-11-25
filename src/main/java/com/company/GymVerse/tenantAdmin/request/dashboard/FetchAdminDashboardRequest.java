package com.company.GymVerse.tenantAdmin.request.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Optional request payload for FETCH_ADMIN_DASHBOARD operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchAdminDashboardRequest {

    /**
     * Optional anchor date for expiry window evaluation (defaults to today).
     */
    private LocalDate referenceDate;
}
