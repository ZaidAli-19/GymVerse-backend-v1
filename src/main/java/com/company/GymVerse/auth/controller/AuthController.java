package com.company.GymVerse.auth.controller;

import com.company.GymVerse.auth.factory.AuthFactory;
import com.company.GymVerse.auth.factory.AuthOperation;
import com.company.GymVerse.auth.request.AuthRequest;
import com.company.GymVerse.auth.request.enums.AuthRequestType;
import com.company.GymVerse.auth.response.AuthResponse;
import com.company.GymVerse.exceptions.common.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified REST controller for all platform authentication operations.
 * Single entry gateway delegating all auth requests via {@link AuthFactory}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class AuthController {

    private final AuthFactory authFactory;

    /**
     * Unified execution gateway for authentication operations.
     *
     * @param request the shared module request envelope
     * @return the shared module response envelope
     */
    @PostMapping
    public ResponseEntity<AuthResponse> processAuth(@RequestBody final AuthRequest request) {
        log.info("Processing auth request: {}", request != null ? request.getRequestType() : "null");
        if (request == null || request.getRequestType() == null) {
            throw new BadRequestException("AuthRequestType must not be null");
        }
        AuthRequestType type = request.getRequestType();
        AuthOperation operation = authFactory.getOperation(type);
        if (operation == null) {
            throw new BadRequestException("Unsupported auth request type: " + type);
        }
        return ResponseEntity.ok(operation.execute(request));
    }
}
