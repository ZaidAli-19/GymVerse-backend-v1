package com.company.GymVerse.tenantAdmin.request.members;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query request payload for retrieving full member profile, membership, payment, and attendance details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchMemberDetailsRequest {

    private Long memberId;
}
