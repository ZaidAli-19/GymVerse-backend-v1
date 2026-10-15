package com.company.GymVerse.platformAdmin.response;

import com.company.GymVerse.models.central.enums.BillingCycle;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaasPlanResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private Integer maxMembers;
    private Integer maxTrainers;
    private Boolean analyticsEnabled;
    private Boolean multiTrainerEnabled;
    private Boolean customBrandingEnabled;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
