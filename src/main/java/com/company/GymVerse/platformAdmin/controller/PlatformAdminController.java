package com.company.GymVerse.platformAdmin.controller;

import com.company.GymVerse.exceptions.BadRequestException;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Platform Administration.
 * Single command gateway delegating module operations via PlatformAdminFactory.
 */
@RestController
@RequestMapping("/api/platform-admin")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class PlatformAdminController {

    private final PlatformAdminFactory factory;

    /**
     * Unified execution endpoint matching the franchiseOperator module convention.
     *
     * @param request the shared module request DTO
     * @return the shared module response DTO
     */
    @PostMapping
    public PlatformAdminResponse execute(@RequestBody PlatformAdminRequest request) {
        log.info("Executing platform admin request: {}", request != null ? request.getPlatformAdminRequestType() : "null");
        if (request == null || request.getPlatformAdminRequestType() == null) {
            throw new BadRequestException("PlatformAdminRequestType must not be null");
        }
        PlatformAdminFactoryOperation operation = factory.getOperation(request.getPlatformAdminRequestType());
        if (operation == null) {
            throw new BadRequestException("Unsupported platform admin request type: " + request.getPlatformAdminRequestType());
        }
        return operation.execute(request);
    }
}
