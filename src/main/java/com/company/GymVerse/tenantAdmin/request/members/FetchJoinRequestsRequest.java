package com.company.GymVerse.tenantAdmin.request.members;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated and filtered query request for FETCH_JOIN_REQUESTS operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchJoinRequestsRequest {

    /**
     * Optional status filter: ALL, PENDING, APPROVED, or REJECTED.
     */
    private String status;

    /**
     * Optional case-insensitive search across applicant firstName, lastName, email, or phoneNumber.
     */
    private String search;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
