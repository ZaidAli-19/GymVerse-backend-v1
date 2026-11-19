package com.company.GymVerse.user.controller;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.user.factory.UserFactory;
import com.company.GymVerse.user.factory.UserOperation;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified REST controller gateway for Central Master User operations.
 * Exposes strictly one POST endpoint delegating commands and queries via {@link UserFactory}.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class UserController {

    private final UserFactory factory;

    /**
     * Unified execution gateway for central master user operations.
     *
     * @param request the unified request envelope
     * @return the unified response envelope
     */
    @PostMapping
    public ResponseEntity<UserResponse> execute(@RequestBody final UserRequest request) {
        log.info("Incoming User request: {}", request != null ? request.getRequestType() : "null");

        if (request == null || request.getRequestType() == null) {
            throw new BadRequestException("UserRequestType must not be null");
        }

        UserRequestType requestType = request.getRequestType();
        UserOperation operation = factory.getOperation(requestType);
        if (operation == null) {
            throw new BadRequestException("Unsupported user request type: " + requestType);
        }

        return ResponseEntity.ok(operation.execute(request));
    }
}
