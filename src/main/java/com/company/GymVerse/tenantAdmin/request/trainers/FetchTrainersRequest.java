package com.company.GymVerse.tenantAdmin.request.trainers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated, sorted, and filtered query request for FETCH_TRAINERS operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchTrainersRequest {

    /**
     * Optional active trainer status filter.
     */
    private Boolean active;

    /**
     * Optional case-insensitive partial match against trainer specialization.
     */
    private String specialization;

    /**
     * Optional case-insensitive search across firstName, lastName, email, phoneNumber, or specialization.
     */
    private String search;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
