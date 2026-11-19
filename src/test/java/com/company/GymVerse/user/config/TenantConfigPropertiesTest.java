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
        assertThat(properties.getDefaultPlanId()).isEqualTo(1L);
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

    @Test
    @DisplayName("Set valid defaultPlanId successfully")
    void setValidDefaultPlanId() {
        TenantConfigProperties properties = new TenantConfigProperties();
        properties.setDefaultPlanId(5L);
        assertThat(properties.getDefaultPlanId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Set non-positive defaultPlanId throws IllegalArgumentException")
    void setInvalidDefaultPlanId() {
        TenantConfigProperties properties = new TenantConfigProperties();
        assertThatThrownBy(() -> properties.setDefaultPlanId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Default plan ID must be a positive number");

        assertThatThrownBy(() -> properties.setDefaultPlanId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Default plan ID must be a positive number");
    }
}
