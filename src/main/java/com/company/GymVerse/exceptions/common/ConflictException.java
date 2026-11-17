package com.company.GymVerse.exceptions.common;

/**
 * Exception thrown when a resource already exists or a unique constraint is violated.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
