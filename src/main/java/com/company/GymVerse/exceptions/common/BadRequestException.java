package com.company.GymVerse.exceptions.common;

/**
 * Exception thrown when a client request is malformed, invalid, or violates domain preconditions.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
