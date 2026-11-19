package com.company.GymVerse.config;

import com.company.GymVerse.models.central.enums.BillingCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 4.7: DefaultTrialPlanSeedProperties Unit Tests")
class DefaultTrialPlanSeedPropertiesTest {

    @Test
    @DisplayName("Default properties values are initialized properly")
    void defaultValues() {
        DefaultTrialPlanSeedProperties properties = new DefaultTrialPlanSeedProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getName()).isEqualTo("Starter (Free Trial)");
        assertThat(properties.getDescription()).isEqualTo("Default introductory trial plan with standard feature entitlements");
        assertThat(properties.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(properties.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(properties.getMaxMembers()).isEqualTo(100);
        assertThat(properties.getMaxTrainers()).isEqualTo(5);
        assertThat(properties.isAnalyticsEnabled()).isTrue();
        assertThat(properties.isMultiTrainerEnabled()).isTrue();
        assertThat(properties.isCustomBrandingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Setters validate bounds properly")
    void setterValidations() {
        DefaultTrialPlanSeedProperties properties = new DefaultTrialPlanSeedProperties();

        assertThatThrownBy(() -> properties.setName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");

        assertThatThrownBy(() -> properties.setPrice(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price must not be negative");

        assertThatThrownBy(() -> properties.setMaxMembers(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxMembers must be at least 1");

        assertThatThrownBy(() -> properties.setMaxTrainers(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTrainers must be at least 1");

        properties.setBillingCycle(null);
        assertThat(properties.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);

        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();

        properties.setPrice(BigDecimal.ZERO);
        assertThat(properties.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
