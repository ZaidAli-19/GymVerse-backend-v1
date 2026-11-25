package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Shared context extractor for tenant-scoped administrative operations.
 * Eliminates repetitive JWT header parsing and local admin resolution across services.
 */
@Component
@RequiredArgsConstructor
public class TenantAdminContextHelper {

    private final JwtUtil jwtUtil;
    private final HttpServletRequest httpServletRequest;
    private final TenantUserRepository tenantUserRepository;

    /**
     * Extracts the authenticated central tenant ID from the current Bearer JWT.
     */
    public Long extractTenantId() {
        String token = extractBearerToken();
        Long tenantId = jwtUtil.extractTenantId(token);
        if (tenantId == null) {
            throw new BadCredentialsException("Tenant token missing tenantId claim");
        }
        return tenantId;
    }

    /**
     * Extracts the authenticated central user account ID from SecurityContextHolder.
     */
    public Long extractCentralUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BadCredentialsException("Authenticated principal is missing");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("Invalid authenticated user identifier", ex);
        }
    }

    /**
     * Resolves the local TenantUser record corresponding to the authenticated admin.
     * Must be invoked inside an active TenantContext.
     */
    public TenantUser resolveCurrentAdminUser() {
        Long centralUserId = extractCentralUserId();
        return tenantUserRepository.findByCentralUserId(centralUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Local tenant admin profile not found for centralUserId: " + centralUserId));
    }

    private String extractBearerToken() {
        String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Bearer token is required");
        }
        return authHeader.substring(7).trim();
    }
}
