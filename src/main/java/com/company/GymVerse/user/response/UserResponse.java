package com.company.GymVerse.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified response envelope for all central master user operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private String message;
    private int status;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private RegisterGymResponse registerGymResponse;
    private FetchMyTenantsResponse fetchMyTenantsResponse;
    private FetchGymByCodeResponse fetchGymByCodeResponse;
    private SubmitJoinRequestResponse submitJoinRequestResponse;
}
