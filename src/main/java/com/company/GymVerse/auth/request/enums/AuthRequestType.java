package com.company.GymVerse.auth.request.enums;

/**
 * Discriminator enum identifying specific operations within the unified Auth module gateway.
 */
public enum AuthRequestType {
    SIGNUP_INITIATE,
    SIGNUP_VERIFY,
    SIGNUP_COMPLETE,
    LOGIN_PASSWORD,
    LOGIN_OTP_INITIATE,
    LOGIN_OTP_VERIFY,
    REFRESH_TOKEN,
    FORGOT_PASSWORD_INITIATE,
    FORGOT_PASSWORD_VERIFY,
    FORGOT_PASSWORD_RESET
}
