package com.company.GymVerse.exceptions.common;

/**
 * Exception thrown when an authenticated caller is forbidden from performing an operation.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
