package com.company.GymVerse.user.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Payload DTO containing all necessary domain information to register and provision
 * a new gym facility and its isolated tenant database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterGymRequest {

    private String gymName;
    private String address;
    private String city;

    /**
     * Optional custom gym code (e.g. "FIT99").
     * Must be 4-10 alphanumeric characters. If omitted, code will be generated.
     */
    private String customGymCode;

    private String contactEmail;
    private String phoneNumber;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private LocalTime openingTime;
    private LocalTime closingTime;
}
