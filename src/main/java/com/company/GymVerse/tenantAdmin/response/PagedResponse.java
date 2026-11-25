package com.company.GymVerse.tenantAdmin.response;

import java.util.List;

/**
 * Standard paginated envelope conforming to PROMPT.md Section 4.5.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
