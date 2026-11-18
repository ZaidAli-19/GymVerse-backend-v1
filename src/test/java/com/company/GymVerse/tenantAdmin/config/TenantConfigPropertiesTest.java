package com.company.GymVerse.tenantAdmin.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantConfigPropertiesTest {

    @Test
    @DisplayName("Default properties are populated with 14 trial days and plan ID 1")
    void defaultValuesAreProperlySet() {
        TenantConfigProperties properties = new TenantConfigProperties();

        assertThat(properties.getDurationDays()).isEqualTo(14);
        assertThat(properties.getDefaultPlanId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Properties can be overridden via spring binder from property map")
    void propertiesCanBeOverriddenViaBinding() {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("gymverse.saas.trial.duration-days", "30");
        propertyMap.put("gymverse.saas.trial.default-plan-id", "5");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(propertyMap);
        Binder binder = new Binder(source);

        TenantConfigProperties properties = binder.bind(
                "gymverse.saas.trial",
                Bindable.of(TenantConfigProperties.class)
        ).orElse(new TenantConfigProperties());

        assertThat(properties.getDurationDays()).isEqualTo(30);
        assertThat(properties.getDefaultPlanId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Lombok builder pattern instantiates properties accurately")
    void builderPatternWorksProperly() {
        TenantConfigProperties properties = TenantConfigProperties.builder()
                .durationDays(21)
                .defaultPlanId(2L)
                .build();

        assertThat(properties.getDurationDays()).isEqualTo(21);
        assertThat(properties.getDefaultPlanId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Setting durationDays less than 1 throws IllegalArgumentException")
    void invalidDurationDaysThrowsException() {
        TenantConfigProperties properties = new TenantConfigProperties();

        assertThatThrownBy(() -> properties.setDurationDays(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trial durationDays must be at least 1 day");

        assertThatThrownBy(() -> properties.setDurationDays(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trial durationDays must be at least 1 day");
    }

    @Test
    @DisplayName("Setting non-positive defaultPlanId throws IllegalArgumentException")
    void invalidPlanIdThrowsException() {
        TenantConfigProperties properties = new TenantConfigProperties();

        assertThatThrownBy(() -> properties.setDefaultPlanId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Default plan ID must be a positive number");

        assertThatThrownBy(() -> properties.setDefaultPlanId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Default plan ID must be a positive number");
    }
}
