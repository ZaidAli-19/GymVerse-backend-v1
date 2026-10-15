package com.company.GymVerse.exceptions;

/**
 * Exception thrown when client input violates domain invariants, format constraints, or business rules.
 * Mapped to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
