package com.company.GymVerse.tenantAdmin.request.liveRoster;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated, sorted, and filtered query request for FETCH_LIVE_ROSTER operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchLiveRosterRequest {

    /**
     * Optional search text across checked-in member identity fields.
     */
    private String search;

    /**
     * Optional membership status filter: ALL, EXPIRED, or TRIAL (defaults to ALL).
     */
    private String membershipFilter;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
