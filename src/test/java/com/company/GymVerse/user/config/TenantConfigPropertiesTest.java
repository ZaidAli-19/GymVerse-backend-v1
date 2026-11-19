package com.company.GymVerse.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantConfigProperties Unit Tests")
class TenantConfigPropertiesTest {

    @Test
    @DisplayName("Default properties values are initialized properly")
    void defaultValues() {
        TenantConfigProperties properties = new TenantConfigProperties();
        assertThat(properties.getDurationDays()).isEqualTo(14);
    }

    @Test
    @DisplayName("Set valid durationDays successfully")
    void setValidDurationDays() {
        TenantConfigProperties properties = new TenantConfigProperties();
        properties.setDurationDays(30);
        assertThat(properties.getDurationDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Set invalid durationDays throws IllegalArgumentException")
    void setInvalidDurationDays() {
        TenantConfigProperties properties = new TenantConfigProperties();
        assertThatThrownBy(() -> properties.setDurationDays(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trial durationDays must be at least 1 day");

        assertThatThrownBy(() -> properties.setDurationDays(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trial durationDays must be at least 1 day");
    }
}
