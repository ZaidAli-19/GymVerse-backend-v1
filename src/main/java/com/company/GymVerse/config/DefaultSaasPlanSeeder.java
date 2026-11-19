package com.company.GymVerse.config;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Startup bootstrapper responsible for ensuring an active SaaS trial subscription plan exists
 * in the central database for newly registered gym tenants.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DefaultSaasPlanSeeder implements ApplicationRunner {

    private final SaasSubscriptionPlanRepository saasSubscriptionPlanRepository;
    private final DefaultTrialPlanSeedProperties seedProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.isEnabled()) {
            log.info("Default SaaS trial plan seeding is disabled via configuration");
            return;
        }

        log.info("Executing default SaaS trial plan startup verification...");
        seedDefaultTrialPlan();
    }

    /**
     * Idempotently seeds the default SaaS trial subscription plan if it does not already exist.
     * Can also be called on-demand as a self-healing fallback during tenant provisioning.
     * Thread-safe within JVM to prevent concurrent check-then-act duplication.
     *
     * @return the existing or newly persisted active trial SaasSubscriptionPlan
     * @throws IllegalStateException if seeding is disabled or fails
     */
    @Transactional("centralTransactionManager")
    public synchronized SaasSubscriptionPlan seedDefaultTrialPlan() {
        if (!seedProperties.isEnabled()) {
            throw new IllegalStateException("Active SaaS trial subscription plan not found and seeding is disabled via configuration");
        }

        // 1. Check if an active trial plan already exists in database
        Optional<SaasSubscriptionPlan> existingTrialPlan = saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue();
        if (existingTrialPlan.isPresent()) {
            log.debug("Active SaaS trial subscription plan already exists with ID: {}", existingTrialPlan.get().getId());
            return existingTrialPlan.get();
        }

        // 2. Check if a plan exists with configured name that can be reactivated / marked as trial
        Optional<SaasSubscriptionPlan> planByName = saasSubscriptionPlanRepository.findByNameIgnoreCase(seedProperties.getName());
        if (planByName.isPresent()) {
            SaasSubscriptionPlan plan = planByName.get();
            plan.setTrial(true);
            plan.setActive(true);
            log.info("Reactivating existing SaaS plan '{}' (ID: {}) as active trial plan", plan.getName(), plan.getId());
            return saasSubscriptionPlanRepository.save(plan);
        }

        // 3. Create fresh trial plan from externalized seed properties
        log.info("Seeding default SaaS trial subscription plan: [Name='{}', Price={}, MaxMembers={}, MaxTrainers={}]",
                seedProperties.getName(), seedProperties.getPrice(), seedProperties.getMaxMembers(), seedProperties.getMaxTrainers());

        SaasSubscriptionPlan newPlan = SaasSubscriptionPlan.builder()
                .name(seedProperties.getName())
                .description(seedProperties.getDescription())
                .price(seedProperties.getPrice())
                .billingCycle(seedProperties.getBillingCycle() != null ? seedProperties.getBillingCycle() : BillingCycle.MONTHLY)
                .maxMembers(seedProperties.getMaxMembers())
                .maxTrainers(seedProperties.getMaxTrainers())
                .analyticsEnabled(seedProperties.isAnalyticsEnabled())
                .multiTrainerEnabled(seedProperties.isMultiTrainerEnabled())
                .customBrandingEnabled(seedProperties.isCustomBrandingEnabled())
                .isTrial(true)
                .active(true)
                .build();

        SaasSubscriptionPlan saved = saasSubscriptionPlanRepository.save(newPlan);
        log.info("Default SaaS trial subscription plan successfully seeded with ID: {}", saved.getId());
        return saved;
    }
}
