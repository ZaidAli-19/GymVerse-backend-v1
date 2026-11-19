package com.company.GymVerse.config;

import com.company.GymVerse.platformAdmin.service.PlatformAdminProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Application startup hook triggering initial Platform Admin provisioning.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminBootstrapConfig {

    private final PlatformAdminProvisioningService provisioningService;

    @Bean
    @Order(1)
    public ApplicationRunner initialPlatformAdminRunner() {
        return args -> {
            log.info("Executing initial platform administrator startup verification...");
            provisioningService.seedInitialPlatformAdmin();
        };
    }
}
