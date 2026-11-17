package com.company.GymVerse.exceptions.otp;

/**
 * Thrown when an OTP request is made before the resend cooldown window has elapsed.
 */
public class OtpRateLimitException extends RuntimeException {

    public OtpRateLimitException(String message) {
        super(message);
    }
}
