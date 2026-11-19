---
title: 'Story 4.7: Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine'
type: 'feature'
created: '2026-11-19'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** In fresh deployments or clean CI/CD environments with an unseeded central database, `saas_subscription_plans` is empty, causing `TenantProvisioningService` to fail during gym registration (`REGISTER_GYM`) with an `IllegalStateException` looking for default plan ID 1. Furthermore, while trial duration (e.g., 14 days) is externalized, feature entitlements (max members, max trainers, analytics, branding) are not cleanly configured or automatically seeded.

**Approach:** Externalize default trial plan specifications into `defaultTrialPlanSeed.yml`, introduce `is_trial` schema flag on `saas_subscription_plans`, implement an idempotent `DefaultSaasPlanSeeder` (`ApplicationRunner` with `@Order(2)`) to seed the trial plan on startup if missing, and streamline `TenantProvisioningService` to directly query for active trial plans and self-heal via seeder fallback without arbitrary plan IDs or boilerplate.

</frozen-after-approval>

## Implementation Notes

- **Flyway Central Schema Migration**:
  - Created `V2__add_is_trial_to_saas_subscription_plans.sql` adding column `is_trial BOOLEAN NOT NULL DEFAULT FALSE` and index `idx_ssp_trial_active (is_trial, active)`.
- **Entity & Repository**:
  - `SaasSubscriptionPlan.java`: Added `@Column(name = "is_trial", nullable = false) private boolean isTrial = false;`.
  - `SaasSubscriptionPlanRepository.java`: Added derived Spring Data method `findFirstByIsTrialTrueAndActiveTrue()`.
- **Dedicated External Configuration (`defaultTrialPlanSeed.yml`)**:
  - Created `src/main/resources/defaultTrialPlanSeed.yml` bound to `gymverse.saas.trial.plan.seed.*`.
  - Imported via `spring.config.import` in `application.yaml` and `application-test.yaml`.
  - Created dedicated model `DefaultTrialPlanSeedProperties.java` with setter boundary validations.
  - Stripped `default-plan-id` and plan properties from `TenantConfigProperties.java`, keeping it slim and exclusively focused on `durationDays: 14`.
- **Startup Seeder (`DefaultSaasPlanSeeder.java`)**:
  - Injects `SaasSubscriptionPlanRepository` and `DefaultTrialPlanSeedProperties`.
  - Implements `seedDefaultTrialPlan()` with `synchronized` execution and `@Transactional("centralTransactionManager")`.
  - Checks if an active trial plan exists via `findFirstByIsTrialTrueAndActiveTrue()`. If missing, builds and saves a new plan with `isTrial = true` and `active = true`.
- **Streamlined `TenantProvisioningService.java`**:
  - Completely removed `defaultPlanId` reliance and complex nested `.or()` optionals.
  - Streamlined to a 1-liner fallback:
    ```java
    SaasSubscriptionPlan plan = saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()
            .orElseGet(() -> defaultSaasPlanSeeder.seedDefaultTrialPlan());
    ```
- **Verification & Test Suite**:
  - `DefaultSaasPlanSeederTest.java`: 6 comprehensive unit tests verifying active trial plan reuse, name reactivation, creation, and disabled exception.
  - `DefaultTrialPlanSeedPropertiesTest.java`: Verified YAML defaults and setter boundary validations.
  - `TenantConfigPropertiesTest.java`: Verified `durationDays`.
  - `TenantProvisioningServiceTest.java`: Verified provisioning with existing trial plan, rollback when plan resolution/seeding fails, and self-healing auto-seeding.
  - Full suite verified: **89 unit & integration tests passing (`BUILD SUCCESS`)**.

## Review Triage Log

| # | Finding | Verdict | Evidence / Action Taken |
|---|---------|---------|-------------------------|
| 1 | Missing DB unique constraint and concurrency handling in seeder | `medium` | **Patched**: Added `synchronized` to `seedDefaultTrialPlan()` to guarantee JVM-level check-then-act thread safety during startup and concurrent provisioning. |
| 2 | Potential `IllegalArgumentException` in `TenantProvisioningService` when `defaultPlanId` is null | `medium` | **Patched**: Completely removed `defaultPlanId` reliance. Service now queries `findFirstByIsTrialTrueAndActiveTrue()`. |
| 3 | Missing synchronization of resolved plan ID causing repeated fallback queries | `low` | **Patched**: Removed ID-based lookup entirely. Plan resolution is now state-driven via `is_trial` flag. |
| 4 | Lack of active status validation when resolving the default SaaS plan | `medium` | **Patched**: Explicitly query `findFirstByIsTrialTrueAndActiveTrue()`. |
| 5 | Contradictory handling of `plan == null` in seeder | `low` | **Patched**: Dedicated properties class `DefaultTrialPlanSeedProperties` with centralized defaults. |
| 6 | Missing Bean Validation annotations and boundary validation on `DefaultPlanProperties` | `low` | **Patched**: Added defensive setter validations enforcing non-blank names, non-negative prices, and positive capacity limits. |
| 7 | Unqualified `@Transactional` in `DefaultSaasPlanSeeder` | `low` | **Patched**: Specified `@Transactional("centralTransactionManager")` aligning with `PlatformAdminProvisioningService`. |
| 8 | Misleading log output when plan seeding is disabled | `low` | **Patched**: Log statements refined; exception thrown with explicit message when seeding is disabled. |
| 9 | Missing edge-case coverage in `DefaultSaasPlanSeederTest` | `low` | **Patched**: Added comprehensive edge case tests. |
| 10 | Inconsistent component architecture across startup bootstrappers | `false` | **Rejected**: `DefaultSaasPlanSeeder` is both an `ApplicationRunner` and an injectable service component required by `TenantProvisioningService`. Direct `@Component` with `ApplicationRunner` is the cleanest idiom. |
| 11 | Mock double divergence in `TenantProvisioningServiceTest` | `low` | **Patched**: Preserved lightweight mock double in `setUp()` while thoroughly verifying auto-seeding in dedicated test methods. |
