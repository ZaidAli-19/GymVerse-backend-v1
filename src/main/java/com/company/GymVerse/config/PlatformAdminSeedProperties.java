package com.company.GymVerse.config;

import com.company.GymVerse.models.central.enums.UserAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for initial Platform Admin seeding bound from platformAdminSeed.yml.
 */
@Component
@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAdminSeedProperties {

    @Builder.Default
    private boolean enabled = true;

    private String email;

    private String firstName;

    private String lastName;

    private String password;

    private String phoneNumber;

    @Builder.Default
    private boolean emailVerified = true;

    @Builder.Default
    private boolean isPlatformAdmin = true;

    @Builder.Default
    private UserAccountStatus status = UserAccountStatus.ACTIVE;
}
