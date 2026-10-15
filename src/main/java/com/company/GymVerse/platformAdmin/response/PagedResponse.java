package com.company.GymVerse.platformAdmin.response;

import java.util.List;

/**
 * Standard paginated envelope conforming to PROMPT.md Section 4.5.
 *
 * @param content       list of payload items in the current page
 * @param page          zero-based current page index
 * @param size          page size
 * @param totalElements total number of records matching the query
 * @param totalPages    total pages available
 * @param last          true if this is the final page
 * @param <T>           type of item in the page
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {}
