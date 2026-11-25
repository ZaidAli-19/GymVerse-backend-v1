package com.company.GymVerse.tenantAdmin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for tenant membership operational thresholds.
 */
@Component
@ConfigurationProperties(prefix = "gymverse.tenant.membership")
@Getter
@Setter
public class TenantOperationsProperties {

    /**
     * Number of days ahead used to classify active memberships as expiring soon on the dashboard.
     */
    private int expiringSoonDays = 7;
}
