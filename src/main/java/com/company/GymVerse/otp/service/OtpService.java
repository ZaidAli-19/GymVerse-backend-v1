package com.company.GymVerse.otp.service;

import com.company.GymVerse.otp.util.OtpPurpose;

/**
 * Service contract for Redis-backed OTP generation, rate-limited delivery,
 * brute-force protected verification, and verified state lifecycle management.
 */
public interface OtpService {

    /**
     * Generates and dispatches a cryptographically secure numeric OTP to the recipient's email
     * via Kafka, enforcing resend cooldown rate limiting.
     *
     * @param email         target recipient email
     * @param purpose       the functional purpose of the OTP
     * @param recipientName optional display name of the recipient (may be null)
     */
    void sendOtp(String email, OtpPurpose purpose, String recipientName);

    /**
     * Resends an OTP to the specified recipient, enforcing the same cooldown policy.
     *
     * @param email         target recipient email
     * @param purpose       the functional purpose of the OTP
     * @param recipientName optional display name of the recipient (may be null)
     */
    void resendOtp(String email, OtpPurpose purpose, String recipientName);

    /**
     * Verifies a provided OTP against the stored code in Redis.
     * Tracks failed attempts and invalidates the code if the maximum attempt threshold is breached.
     *
     * @param email   target recipient email
     * @param otp     the 6-digit verification code to validate
     * @param purpose the functional purpose of the OTP
     * @return true if OTP is valid and matches
     */
    boolean verifyOtp(String email, String otp, OtpPurpose purpose);

    /**
     * Checks if the OTP for the given email and purpose has already been successfully verified
     * and the verified token window is still active.
     *
     * @param email   target recipient email
     * @param purpose the functional purpose of the OTP
     * @return true if previously verified and active
     */
    boolean isOtpVerified(String email, OtpPurpose purpose);

    /**
     * Consumes (invalidates) the verified state token upon successful completion of the target operation
     * (e.g. signup account creation or password reset).
     *
     * @param email   target recipient email
     * @param purpose the functional purpose of the OTP
     */
    void consumeVerifiedOtp(String email, OtpPurpose purpose);
}
