package com.company.GymVerse.user.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for tenant onboarding and SaaS subscription trial duration.
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

    public void setDurationDays(int durationDays) {
        if (durationDays < 1) {
            throw new IllegalArgumentException("Trial durationDays must be at least 1 day");
        }
        this.durationDays = durationDays;
    }
}
