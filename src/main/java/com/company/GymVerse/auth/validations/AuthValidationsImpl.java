package com.company.GymVerse.auth.validations;

import com.company.GymVerse.auth.request.LoginRequest;
import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.exceptions.common.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Concrete implementation of {@link AuthValidations} enforcing defensive security constraints
 * and input sanity checks across authentication flows.
 */
@Component
public class AuthValidationsImpl implements AuthValidations {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,20}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/].*");

    @Override
    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email address is required");
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Invalid email format: " + trimmed);
        }
    }

    @Override
    public void validateName(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new BadRequestException("First name is required");
        }
        String trimmedFirst = firstName.trim();
        if (trimmedFirst.length() < 2 || trimmedFirst.length() > 50) {
            throw new BadRequestException("First name must be between 2 and 50 characters");
        }

        if (lastName == null || lastName.isBlank()) {
            throw new BadRequestException("Last name is required");
        }
        String trimmedLast = lastName.trim();
        if (trimmedLast.length() < 2 || trimmedLast.length() > 50) {
            throw new BadRequestException("Last name must be between 2 and 50 characters");
        }
    }

    @Override
    public void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return; // Optional field
        }
        String trimmed = phoneNumber.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Invalid phone number format: " + trimmed);
        }
    }

    @Override
    public void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        if (password.length() > 100) {
            throw new BadRequestException("Password cannot exceed 100 characters");
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must contain at least one number");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must contain at least one special character (@$!%*?&#^()_+-=[]{};':\",.<>/)");
        }
    }

    @Override
    public void validateOtp(String otp) {
        if (otp == null || otp.isBlank()) {
            throw new BadRequestException("OTP code is required");
        }
        String trimmed = otp.trim();
        if (!OTP_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("OTP must be a 6-digit numeric code");
        }
    }

    @Override
    public void validateSignupInitiate(SignupRequest request) {
        if (request == null) {
            throw new BadRequestException("SignupRequest must not be null");
        }
        validateEmail(request.getEmail());
        validateName(request.getFirstName(), request.getLastName());
        validatePhoneNumber(request.getPhoneNumber());
    }

    @Override
    public void validateSignupVerify(SignupRequest request) {
        if (request == null) {
            throw new BadRequestException("SignupRequest must not be null");
        }
        validateEmail(request.getEmail());
        validateOtp(request.getOtp());
    }

    @Override
    public void validateSignupComplete(SignupRequest request) {
        if (request == null) {
            throw new BadRequestException("SignupRequest must not be null");
        }
        validateEmail(request.getEmail());
        validatePasswordStrength(request.getPassword());
    }

    @Override
    public void validateLoginPassword(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("LoginRequest must not be null");
        }
        validateEmail(request.getEmail());
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
    }

    @Override
    public void validateLoginOtpInitiate(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("LoginRequest must not be null");
        }
        validateEmail(request.getEmail());
    }

    @Override
    public void validateLoginOtpVerify(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("LoginRequest must not be null");
        }
        validateEmail(request.getEmail());
        validateOtp(request.getOtp());
    }

    @Override
    public void validateRefreshToken(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("LoginRequest must not be null");
        }
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
    }
}
