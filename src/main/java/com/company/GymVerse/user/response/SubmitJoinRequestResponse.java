package com.company.GymVerse.user.response;

import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response payload confirming submission of a gym join request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitJoinRequestResponse {

    private Long requestId;
    private Long gymTenantId;
    private String gymCode;
    private String gymName;
    private JoinRequestStatus status;
    private String requestNotes;
    private Instant createdAt;
}
