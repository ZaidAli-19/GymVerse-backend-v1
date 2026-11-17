package com.company.GymVerse.email.service;

/**
 * Service contract for dispatching transactional and verification emails.
 */
public interface EmailService {

    /**
     * Sends an HTML-formatted OTP verification email.
     *
     * @param toEmail       recipient's destination email address
     * @param otp           the one-time verification code
     * @param purpose       the functional purpose of the OTP (e.g. SIGNUP, LOGIN, FORGOT_PASSWORD)
     * @param recipientName optional display name of the recipient (may be null or blank)
     */
    void sendOtpEmail(String toEmail, String otp, String purpose, String recipientName);
}
