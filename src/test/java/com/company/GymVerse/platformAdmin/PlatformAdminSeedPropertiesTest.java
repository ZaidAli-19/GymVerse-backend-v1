package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.config.PlatformAdminSeedProperties;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlatformAdminSeedProperties Unit Tests")
class PlatformAdminSeedPropertiesTest {

    @Test
    @DisplayName("Binds all properties correctly from configuration source")
    void testPropertyBinding() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("gymverse.platform-admin.seed.enabled", "true");
        properties.put("gymverse.platform-admin.seed.email", "admin@gymverse.com");
        properties.put("gymverse.platform-admin.seed.first-name", "Platform");
        properties.put("gymverse.platform-admin.seed.last-name", "Superadmin");
        properties.put("gymverse.platform-admin.seed.password", "SuperSecretPass123!");
        properties.put("gymverse.platform-admin.seed.phone-number", "+1234567890");
        properties.put("gymverse.platform-admin.seed.email-verified", "true");
        properties.put("gymverse.platform-admin.seed.is-platform-admin", "true");
        properties.put("gymverse.platform-admin.seed.status", "ACTIVE");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        PlatformAdminSeedProperties seedProperties = binder.bind("gymverse.platform-admin.seed", PlatformAdminSeedProperties.class)
                .orElseThrow(() -> new AssertionError("Failed to bind PlatformAdminSeedProperties"));

        assertThat(seedProperties.isEnabled()).isTrue();
        assertThat(seedProperties.getEmail()).isEqualTo("admin@gymverse.com");
        assertThat(seedProperties.getFirstName()).isEqualTo("Platform");
        assertThat(seedProperties.getLastName()).isEqualTo("Superadmin");
        assertThat(seedProperties.getPassword()).isEqualTo("SuperSecretPass123!");
        assertThat(seedProperties.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(seedProperties.isEmailVerified()).isTrue();
        assertThat(seedProperties.isPlatformAdmin()).isTrue();
        assertThat(seedProperties.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Default values are preserved when omitted")
    void testDefaultValues() {
        PlatformAdminSeedProperties defaultProps = new PlatformAdminSeedProperties();
        assertThat(defaultProps.isEnabled()).isTrue();
        assertThat(defaultProps.isEmailVerified()).isTrue();
        assertThat(defaultProps.isPlatformAdmin()).isTrue();
        assertThat(defaultProps.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    }
}
