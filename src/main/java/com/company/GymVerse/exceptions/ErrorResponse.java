package com.company.GymVerse.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard uniform error response payload conforming to PROMPT.md Section 4.3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant timestamp;

    private int status;

    private String error;

    private String message;

    private String path;
}
