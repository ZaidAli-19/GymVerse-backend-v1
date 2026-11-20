package com.company.GymVerse.user.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for submitting a membership application/join request to a gym tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitJoinRequest {

    private String gymCode;
    private String requestNotes;
}
