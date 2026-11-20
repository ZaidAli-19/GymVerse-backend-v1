package com.company.GymVerse.user.response;

import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload carrying discovered gym metadata for member discovery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchGymByCodeResponse {

    private Long tenantId;
    private String gymCode;
    private String gymName;
    private String address;
    private String city;
    private String phoneNumber;
    private String contactEmail;
    private String logoUrl;
    private GymStatus status;
    private GymSubscriptionStatus subscriptionStatus;
}
