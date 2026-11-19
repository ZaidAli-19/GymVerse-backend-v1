package com.company.GymVerse.config;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Story 4.7: DefaultSaasPlanSeeder Unit Tests")
class DefaultSaasPlanSeederTest {

    @Mock
    private SaasSubscriptionPlanRepository saasSubscriptionPlanRepository;

    private DefaultTrialPlanSeedProperties seedProperties;
    private DefaultSaasPlanSeeder seeder;

    @BeforeEach
    void setUp() {
        seedProperties = new DefaultTrialPlanSeedProperties();
        seeder = new DefaultSaasPlanSeeder(saasSubscriptionPlanRepository, seedProperties);
    }

    @Test
    @DisplayName("run() does nothing when seeding is disabled via configuration")
    void run_WhenDisabled_DoesNotSeed() {
        seedProperties.setEnabled(false);

        seeder.run(new DefaultApplicationArguments());

        verify(saasSubscriptionPlanRepository, never()).findFirstByIsTrialTrueAndActiveTrue();
        verify(saasSubscriptionPlanRepository, never()).findByNameIgnoreCase(any());
        verify(saasSubscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("seedDefaultTrialPlan() throws IllegalStateException when seeding is disabled")
    void seedDefaultTrialPlan_WhenDisabled_ThrowsException() {
        seedProperties.setEnabled(false);

        assertThatThrownBy(() -> seeder.seedDefaultTrialPlan())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Active SaaS trial subscription plan not found and seeding is disabled");

        verify(saasSubscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("seedDefaultTrialPlan() returns existing active trial plan and does not duplicate")
    void seedDefaultTrialPlan_WhenActiveTrialPlanExists_ReturnsExistingWithoutSaving() {
        SaasSubscriptionPlan existingTrial = SaasSubscriptionPlan.builder()
                .id(1L)
                .name("Starter (Free Trial)")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(100)
                .maxTrainers(5)
                .isTrial(true)
                .active(true)
                .build();

        when(saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()).thenReturn(Optional.of(existingTrial));

        SaasSubscriptionPlan result = seeder.seedDefaultTrialPlan();

        assertThat(result).isSameAs(existingTrial);
        assertThat(result.isTrial()).isTrue();
        verify(saasSubscriptionPlanRepository, times(1)).findFirstByIsTrialTrueAndActiveTrue();
        verify(saasSubscriptionPlanRepository, never()).findByNameIgnoreCase(any());
        verify(saasSubscriptionPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("seedDefaultTrialPlan() reactivates existing plan by name as active trial plan")
    void seedDefaultTrialPlan_WhenPlanExistsByName_ReactivatesAndMarksAsTrial() {
        SaasSubscriptionPlan existingPlan = SaasSubscriptionPlan.builder()
                .id(42L)
                .name("Starter (Free Trial)")
                .price(new BigDecimal("29.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(100)
                .maxTrainers(5)
                .isTrial(false)
                .active(false)
                .build();

        when(saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()).thenReturn(Optional.empty());
        when(saasSubscriptionPlanRepository.findByNameIgnoreCase("Starter (Free Trial)")).thenReturn(Optional.of(existingPlan));
        when(saasSubscriptionPlanRepository.save(any(SaasSubscriptionPlan.class))).thenAnswer(i -> i.getArgument(0));

        SaasSubscriptionPlan result = seeder.seedDefaultTrialPlan();

        assertThat(result.isActive()).isTrue();
        assertThat(result.isTrial()).isTrue();
        verify(saasSubscriptionPlanRepository, times(1)).save(existingPlan);
    }

    @Test
    @DisplayName("seedDefaultTrialPlan() creates and persists default trial plan when not found")
    void seedDefaultTrialPlan_WhenMissing_CreatesAndPersistsTrialPlan() {
        when(saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()).thenReturn(Optional.empty());
        when(saasSubscriptionPlanRepository.findByNameIgnoreCase("Starter (Free Trial)")).thenReturn(Optional.empty());

        when(saasSubscriptionPlanRepository.save(any(SaasSubscriptionPlan.class))).thenAnswer(invocation -> {
            SaasSubscriptionPlan plan = invocation.getArgument(0);
            plan.setId(10L);
            return plan;
        });

        SaasSubscriptionPlan result = seeder.seedDefaultTrialPlan();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Starter (Free Trial)");
        assertThat(result.getDescription()).isEqualTo("Default introductory trial plan with standard feature entitlements");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(result.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(result.getMaxMembers()).isEqualTo(100);
        assertThat(result.getMaxTrainers()).isEqualTo(5);
        assertThat(result.isAnalyticsEnabled()).isTrue();
        assertThat(result.isMultiTrainerEnabled()).isTrue();
        assertThat(result.isCustomBrandingEnabled()).isFalse();
        assertThat(result.isTrial()).isTrue();
        assertThat(result.isActive()).isTrue();

        ArgumentCaptor<SaasSubscriptionPlan> captor = ArgumentCaptor.forClass(SaasSubscriptionPlan.class);
        verify(saasSubscriptionPlanRepository).save(captor.capture());
        SaasSubscriptionPlan saved = captor.getValue();
        assertThat(saved.isTrial()).isTrue();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("run() triggers seeding when enabled")
    void run_WhenEnabled_TriggersSeeding() {
        when(saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()).thenReturn(Optional.empty());
        when(saasSubscriptionPlanRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(saasSubscriptionPlanRepository.save(any(SaasSubscriptionPlan.class))).thenAnswer(invocation -> {
            SaasSubscriptionPlan plan = invocation.getArgument(0);
            plan.setId(1L);
            return plan;
        });

        seeder.run(new DefaultApplicationArguments());

        verify(saasSubscriptionPlanRepository).save(any(SaasSubscriptionPlan.class));
    }
}
