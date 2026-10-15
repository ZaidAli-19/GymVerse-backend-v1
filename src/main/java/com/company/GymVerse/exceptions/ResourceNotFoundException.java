package com.company.GymVerse.exceptions;

/**
 * Exception thrown when a requested resource cannot be located.
 * Mapped to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
