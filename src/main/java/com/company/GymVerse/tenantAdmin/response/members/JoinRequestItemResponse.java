package com.company.GymVerse.tenantAdmin.response.members;

import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Join request view item returned in paginated FETCH_JOIN_REQUESTS and REJECT_JOIN_REQUEST responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestItemResponse {

    private Long id;
    private Long centralUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private JoinRequestStatus status;
    private String requestNotes;
    private String adminNotes;
    private Instant createdAt;
    private Instant reviewedAt;
    private Long reviewedByAdminId;
}
