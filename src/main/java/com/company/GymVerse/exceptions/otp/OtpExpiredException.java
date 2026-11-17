package com.company.GymVerse.exceptions.otp;

/**
 * Thrown when an OTP verification is attempted for an expired or non-existent OTP.
 */
public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException(String message) {
        super(message);
    }
}
