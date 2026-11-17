package com.company.GymVerse.exceptions.otp;

/**
 * Thrown when an invalid OTP code is provided or verification attempts are exceeded.
 */
public class OtpInvalidException extends RuntimeException {

    public OtpInvalidException(String message) {
        super(message);
    }
}
