package com.company.GymVerse.config;

import com.company.GymVerse.models.central.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Externalized configuration properties for default SaaS trial subscription plan seeding
 * bound from defaultTrialPlanSeed.yml with prefix gymverse.saas.trial.plan.seed.
 */
@Component
@ConfigurationProperties(prefix = "gymverse.saas.trial.plan.seed")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultTrialPlanSeedProperties {

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String name = "Starter (Free Trial)";

    @Builder.Default
    private String description = "Default introductory trial plan with standard feature entitlements";

    @Builder.Default
    private BigDecimal price = new BigDecimal("29.99");

    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Builder.Default
    private int maxMembers = 100;

    @Builder.Default
    private int maxTrainers = 5;

    @Builder.Default
    private boolean analyticsEnabled = true;

    @Builder.Default
    private boolean multiTrainerEnabled = true;

    @Builder.Default
    private boolean customBrandingEnabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        if (name != null && name.trim().isEmpty()) {
            throw new IllegalArgumentException("Default SaaS trial plan name must not be blank");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Default SaaS trial plan price must not be negative");
        }
        this.price = price;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle != null ? billingCycle : BillingCycle.MONTHLY;
    }

    public void setMaxMembers(int maxMembers) {
        if (maxMembers < 1) {
            throw new IllegalArgumentException("Default SaaS trial plan maxMembers must be at least 1");
        }
        this.maxMembers = maxMembers;
    }

    public void setMaxTrainers(int maxTrainers) {
        if (maxTrainers < 1) {
            throw new IllegalArgumentException("Default SaaS trial plan maxTrainers must be at least 1");
        }
        this.maxTrainers = maxTrainers;
    }

    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }

    public void setMultiTrainerEnabled(boolean multiTrainerEnabled) {
        this.multiTrainerEnabled = multiTrainerEnabled;
    }

    public void setCustomBrandingEnabled(boolean customBrandingEnabled) {
        this.customBrandingEnabled = customBrandingEnabled;
    }
}
