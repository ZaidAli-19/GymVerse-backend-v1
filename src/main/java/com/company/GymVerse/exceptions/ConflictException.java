package com.company.GymVerse.exceptions;

/**
 * Exception thrown when an operation conflicts with the current state of a resource (e.g. duplicate unique key).
 * Mapped to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
