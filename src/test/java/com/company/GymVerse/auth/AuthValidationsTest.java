package com.company.GymVerse.auth;

import com.company.GymVerse.auth.request.SignupRequest;
import com.company.GymVerse.auth.validations.AuthValidationsImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValidationsTest {

    private AuthValidationsImpl validations;

    @BeforeEach
    void setUp() {
        validations = new AuthValidationsImpl();
    }

    @Test
    @DisplayName("validateEmail accepts valid RFC email and rejects malformed formats")
    void validateEmail_ValidAndInvalid() {
        assertThatCode(() -> validations.validateEmail("user@example.com")).doesNotThrowAnyException();
        assertThatCode(() -> validations.validateEmail("user.name+tag@sub.domain.org")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validations.validateEmail(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email address is required");

        assertThatThrownBy(() -> validations.validateEmail("invalid-email"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email format");

        assertThatThrownBy(() -> validations.validateEmail("missing@domain"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("validateName enforces length constraints between 2 and 50 chars")
    void validateName_EnforcesLength() {
        assertThatCode(() -> validations.validateName("John", "Doe")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validations.validateName("J", "Doe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("First name must be between 2 and 50 characters");

        assertThatThrownBy(() -> validations.validateName("John", ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Last name is required");
    }

    @Test
    @DisplayName("validatePasswordStrength requires 8+ chars with uppercase, lowercase, digit, and special char")
    void validatePasswordStrength_ValidAndInvalid() {
        assertThatCode(() -> validations.validatePasswordStrength("StrongP@ss1")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validations.validatePasswordStrength("short1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8 characters long");

        assertThatThrownBy(() -> validations.validatePasswordStrength("alllowercase1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("uppercase letter");

        assertThatThrownBy(() -> validations.validatePasswordStrength("ALLUPPERCASE1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lowercase letter");

        assertThatThrownBy(() -> validations.validatePasswordStrength("NoDigitsHere!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one number");

        assertThatThrownBy(() -> validations.validatePasswordStrength("NoSpecialChar123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("special character");
    }

    @Test
    @DisplayName("validateOtp requires exactly 6 numeric digits")
    void validateOtp_ValidAndInvalid() {
        assertThatCode(() -> validations.validateOtp("123456")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validations.validateOtp("12345"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("6-digit numeric code");

        assertThatThrownBy(() -> validations.validateOtp("1234567"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("6-digit numeric code");

        assertThatThrownBy(() -> validations.validateOtp("abcdef"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("6-digit numeric code");
    }

    @Test
    @DisplayName("validateSignupInitiate checks whole request envelope")
    void validateSignupInitiate_Success() {
        SignupRequest request = SignupRequest.builder()
                .email("valid@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .phoneNumber("+1234567890")
                .build();

        assertThatCode(() -> validations.validateSignupInitiate(request)).doesNotThrowAnyException();
    }
}
