package com.company.GymVerse.platformAdmin.response;

import com.company.GymVerse.models.central.enums.GymStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Summary representation of a registered gym tenant for directory retrieval.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GymTenantSummaryResponse {

    private Long id;
    private String gymName;
    private String contactEmail;
    private String phoneNumber;
    private String address;
    private String city;
    private String logoUrl;
    private String gymCode;
    private String dbName;
    private GymStatus status;
    private Instant registeredAt;
    private Instant suspendedAt;
    private String suspensionReason;
}
