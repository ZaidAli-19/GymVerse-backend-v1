package com.company.GymVerse.tenantAdmin.request.members;

import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated, sorted, and filtered query request for FETCH_MEMBERS operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchMembersRequest {

    /**
     * Optional directory filter: ALL, LIVE, EXPIRED, or TRIAL (defaults to ALL).
     */
    @JsonAlias({"statusFilter", "status"})
    private MemberFilterStatus statusFilter;

    /**
     * Optional case-insensitive search across firstName, lastName, email, phoneNumber, or gymId.
     */
    private String search;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
