# Story 4.7 Task Log: Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine

## 1. Context & Operational Goals
- **Objective:** Eliminate clean-installation failures where `saas_subscription_plans` is empty on a fresh database, leading `TenantProvisioningService` to fail on `REGISTER_GYM` looking for `defaultPlanId = 1L`.
- **Solution:** 
  1. Add Flyway central migration `V2__add_is_trial_to_saas_subscription_plans.sql` adding `is_trial` boolean flag and index.
  2. Externalize default trial plan specifications into dedicated `defaultTrialPlanSeed.yml` and `DefaultTrialPlanSeedProperties`.
  3. Strip `default-plan-id` and plan properties from `TenantConfigProperties`.
  4. Implement `DefaultSaasPlanSeeder` (`ApplicationRunner` at `@Order(2)`) to seed the trial tier with `is_trial = true` and `active = true`.
  5. Streamline `TenantProvisioningService` to query `findFirstByIsTrialTrueAndActiveTrue().orElseGet(seeder::seedDefaultTrialPlan)`.

## 2. Changes Made
1. **Flyway Migration (`V2__add_is_trial_to_saas_subscription_plans.sql`)**:
   - Added `is_trial BOOLEAN NOT NULL DEFAULT FALSE` to `saas_subscription_plans`.
   - Added composite index `idx_ssp_trial_active (is_trial, active)`.
2. **Entity & Repository**:
   - `SaasSubscriptionPlan.java`: Added `@Builder.Default @Column(name = "is_trial", nullable = false) private boolean isTrial = false;`.
   - `SaasSubscriptionPlanRepository.java`: Added `Optional<SaasSubscriptionPlan> findFirstByIsTrialTrueAndActiveTrue();`.
3. **YAML & Configuration**:
   - `defaultTrialPlanSeed.yml`: Created dedicated seed configuration matching `platformAdminSeed.yml` pattern.
   - `DefaultTrialPlanSeedProperties.java`: Component bound to `gymverse.saas.trial.plan.seed`.
   - `application.yaml` & `application-test.yaml`: Added `- classpath:defaultTrialPlanSeed.yml` to `spring.config.import`.
   - `TenantConfigProperties.java`: Removed `defaultPlanId` and plan properties; retained only `durationDays = 14`.
4. **Startup Seeder (`DefaultSaasPlanSeeder.java`)**:
   - Checks `findFirstByIsTrialTrueAndActiveTrue()`.
   - If missing, builds and saves active trial plan with `isTrial = true`.
   - Thread-safe (`synchronized`) and transactional (`@Transactional("centralTransactionManager")`).
5. **Tenant Provisioning Service (`TenantProvisioningService.java`)**:
   - Replaced verbose fallback with 1-liner:
     ```java
     SaasSubscriptionPlan plan = saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()
             .orElseGet(() -> defaultSaasPlanSeeder.seedDefaultTrialPlan());
     ```
6. **Tests**:
   - `DefaultSaasPlanSeederTest.java`: 6 unit tests.
   - `DefaultTrialPlanSeedPropertiesTest.java`: 2 unit tests.
   - `TenantConfigPropertiesTest.java`: 3 unit tests.
   - `TenantProvisioningServiceTest.java`: 8 unit tests.

## 3. Verification
- Maven test run: `89 tests run, 0 failures, 0 errors, 0 skipped` (`BUILD SUCCESS`).
- Invariant check: All 8 core multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched.
- Git policy: All changes kept local; zero unauthorized commits or pushes executed.
