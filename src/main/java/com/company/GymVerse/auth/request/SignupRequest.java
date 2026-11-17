package com.company.GymVerse.auth.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload carrier for master account signup operations (initiate, verify, complete).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String otp;
    private String password;
}
