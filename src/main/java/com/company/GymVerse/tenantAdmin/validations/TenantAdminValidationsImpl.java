package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ForbiddenException;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of {@link TenantAdminValidations} enforcing tenant boundary and role authorization constraints.
 */
@Component
public class TenantAdminValidationsImpl implements TenantAdminValidations {

    @Override
    public void validateTenantAdminContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()))) {
            throw new BadCredentialsException("Full authentication is required to access tenant administration endpoints");
        }

        if (TenantContext.getCurrentTenant() == null) {
            throw new ForbiddenException("Tenant-scoped admin token required; master account tokens cannot access tenant administration endpoints");
        }

        boolean hasTenantAdminRole = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TENANT_ADMIN".equalsIgnoreCase(a.getAuthority()));

        if (!hasTenantAdminRole) {
            throw new ForbiddenException("Access denied: caller does not possess ROLE_TENANT_ADMIN authority");
        }
    }

    @Override
    public void validateRequest(final TenantAdminRequest request) {
        if (request == null) {
            throw new BadRequestException("TenantAdminRequest payload must not be null");
        }
        if (request.getTenantAdminRequestType() == null) {
            throw new BadRequestException("tenantAdminRequestType must not be null");
        }
    }
}
