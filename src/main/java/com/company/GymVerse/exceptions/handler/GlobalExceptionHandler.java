package com.company.GymVerse.exceptions.handler;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.exceptions.otp.OtpExpiredException;
import com.company.GymVerse.exceptions.otp.OtpInvalidException;
import com.company.GymVerse.exceptions.otp.OtpRateLimitException;
import com.company.GymVerse.exceptions.payload.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralized exception handler mapping domain and system exceptions to standardized ErrorResponse payloads.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request encountered at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict encountered at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(com.company.GymVerse.exceptions.common.ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(com.company.GymVerse.exceptions.common.ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden operation at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OtpRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleOtpRateLimitException(OtpRateLimitException ex, HttpServletRequest request) {
        log.warn("OTP rate limit exceeded at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({OtpInvalidException.class, OtpExpiredException.class})
    public ResponseEntity<ErrorResponse> handleOtpValidationException(RuntimeException ex, HttpServletRequest request) {
        log.warn("OTP validation failure at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, HttpServletRequest request) {
        log.warn("JWT failure at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String combinedErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (combinedErrors.isBlank()) {
            combinedErrors = "Validation failed for request payload";
        }
        log.warn("Validation error at [{}]: {}", request.getRequestURI(), combinedErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, combinedErrors, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON payload or unreadable body at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request payload or invalid parameter format", request.getRequestURI());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalStateAndArgument(RuntimeException ex, HttpServletRequest request) {
        log.warn("Illegal argument/state at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(com.company.GymVerse.user.exceptions.TenantProvisioningException.class)
    public ResponseEntity<ErrorResponse> handleTenantProvisioningException(com.company.GymVerse.user.exceptions.TenantProvisioningException ex, HttpServletRequest request) {
        log.error("Tenant provisioning failure at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error at [{}]: ", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
