package com.company.GymVerse.tenantAdmin.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for tenant onboarding and SaaS subscription trial management.
 * Bound to prefix: gymverse.saas.trial
 */
@Component
@ConfigurationProperties(prefix = "gymverse.saas.trial")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfigProperties {

    /**
     * Default free trial duration in days offered to newly registered gym tenants.
     */
    @Builder.Default
    private int durationDays = 14;

    /**
     * Default SaaS subscription plan ID attached to new tenants during their free trial.
     */
    @Builder.Default
    private Long defaultPlanId = 1L;

    public void setDurationDays(int durationDays) {
        if (durationDays < 1) {
            throw new IllegalArgumentException("Trial durationDays must be at least 1 day");
        }
        this.durationDays = durationDays;
    }

    public void setDefaultPlanId(Long defaultPlanId) {
        if (defaultPlanId != null && defaultPlanId <= 0) {
            throw new IllegalArgumentException("Default plan ID must be a positive number");
        }
        this.defaultPlanId = defaultPlanId;
    }
}
