package com.company.GymVerse.tenantAdmin.controller;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminFactory;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified controller gateway exposing all tenant-scoped administrative workflows on /api/tenant-admin.
 * Adheres strictly to the single unified POST endpoint architectural standard.
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant-admin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TenantAdminController {

    private final TenantAdminFactory tenantAdminFactory;

    /**
     * Dispatches tenant administrative operations based on the tenantAdminRequestType discriminator.
     *
     * @param request the unified tenant admin request envelope
     * @return unified {@link TenantAdminResponse}
     */
    @PostMapping
    public ResponseEntity<TenantAdminResponse> handleTenantAdminOperation(
            @RequestBody final TenantAdminRequest request
    ) {
        log.info("Received /api/tenant-admin operation request: type={}", request != null ? request.getTenantAdminRequestType() : null);
        if (request == null || request.getTenantAdminRequestType() == null) {
            throw new BadRequestException("tenantAdminRequestType must not be null");
        }
        TenantAdminOperation operation = tenantAdminFactory.getOperation(request.getTenantAdminRequestType());
        TenantAdminResponse response = operation.execute(request);
        return ResponseEntity.status(response.getStatus() > 0 ? response.getStatus() : 200).body(response);
    }
}
