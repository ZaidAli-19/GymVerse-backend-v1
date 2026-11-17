package com.company.GymVerse.auth.validations;

import com.company.GymVerse.auth.request.SignupRequest;

/**
 * Validation interface defining security, format, and business rule constraints for authentication flows.
 */
public interface AuthValidations {

    void validateEmail(String email);

    void validateName(String firstName, String lastName);

    void validatePhoneNumber(String phoneNumber);

    void validatePasswordStrength(String password);

    void validateOtp(String otp);

    void validateSignupInitiate(SignupRequest request);

    void validateSignupVerify(SignupRequest request);

    void validateSignupComplete(SignupRequest request);
}
