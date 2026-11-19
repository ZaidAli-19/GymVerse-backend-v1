package com.company.GymVerse.user.dto;

import com.company.GymVerse.models.central.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Command DTO carrying authenticated caller details and facility configuration for provisioning a new tenant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantProvisioningCommand {

    private UserAccount userAccount;
    private String gymName;
    private String contactEmail;
    private String phoneNumber;
    private String address;
    private String city;
    private String logoUrl;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private LocalTime autoCheckoutTime;
    private String timeZone;
    private String customGymCode;
}
