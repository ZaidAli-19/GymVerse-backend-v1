package com.company.GymVerse.platformAdmin.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformAdminResponse {

    private String message;
    private Long planId;
    private SaasPlanResponse saasPlan;

    // Direct scalar fields for backward compatibility and convenient client consumption
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean active;
}
