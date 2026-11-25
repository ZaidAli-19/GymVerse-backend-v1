package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ForbiddenException;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

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

    @Override
    public Pageable validateAndBuildPageable(
            final Integer page,
            final Integer size,
            final String sortBy,
            final String sortDirection,
            final String defaultSortBy,
            final Set<String> allowedSortFields
    ) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;

        if (resolvedPage < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        if (resolvedSize < 1 || resolvedSize > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }

        String resolvedSortBy = (sortBy != null && !sortBy.trim().isBlank())
                ? sortBy.trim()
                : defaultSortBy;

        if (allowedSortFields != null && !allowedSortFields.contains(resolvedSortBy)) {
            throw new BadRequestException("Invalid sortBy field: '" + resolvedSortBy + "'. Allowed fields: " + allowedSortFields);
        }

        String resolvedDir = (sortDirection != null && !sortDirection.trim().isBlank())
                ? sortDirection.trim()
                : "asc";

        if (!"asc".equalsIgnoreCase(resolvedDir) && !"desc".equalsIgnoreCase(resolvedDir)) {
            throw new BadRequestException("sortDirection must be 'asc' or 'desc'");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortBy));
    }
}
