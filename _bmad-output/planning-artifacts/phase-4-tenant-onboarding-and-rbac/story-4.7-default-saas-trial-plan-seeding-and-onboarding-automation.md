---
id: "story-4.7"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine"
status: "done"
type: "infrastructure-and-bootstrapping"
created_at: "2026-11-19"
dependencies:
  - "story-4.1"
  - "story-4.3"
  - "story-4.4r"
  - "story-4.6"
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
  - "src/main/java/com/company/GymVerse/user/config/TenantConfigProperties.java"
  - "src/main/java/com/company/GymVerse/user/service/TenantProvisioningService.java"
  - "_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/phase-4-epic.md"
  - "_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/dependency-matrix-and-ordering.md"
  - "antigravity/project-context.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/config/DefaultSaasPlanSeeder.java"
  - "src/test/java/com/company/GymVerse/config/DefaultSaasPlanSeederTest.java"
---

# Story 4.7: Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Preserve all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` 100% byte-for-byte untouched.
> 4. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No field `@Autowired`.
> 5. Self-bootstrapping invariant: A brand new installation with an empty database must successfully execute `REGISTER_GYM` without requiring prior manual Platform Admin intervention to create SaaS plans.
> 6. Maintain task logging in `antigravity/story-4.7-default-saas-trial-plan-seeding-and-onboarding-automation.md`.

---

## 1. User Story

**As a** newly registered gym owner on a fresh deployment of GymVerse,  
**I want** the backend platform to automatically bootstrap and seed a default SaaS trial subscription plan on startup using configuration from `application.yaml`,  
**So that** gym registration (`REGISTER_GYM`) succeeds immediately out of the box without crashing on an empty database, and my gym is granted an authentic free trial with realistic feature entitlements.

---

## 2. Technical Context & Operational Problem

### 2.1 The Problem It Solves
1. **Empty Database Vulnerability**:
   In `TenantProvisioningService.java` (lines 247–249):
   ```java
   Long defaultPlanId = tenantConfigProperties.getDefaultPlanId(); // default: 1L
   SaasSubscriptionPlan plan = saasSubscriptionPlanRepository.findById(defaultPlanId)
           .orElseThrow(() -> new IllegalStateException("Default SaaS subscription plan not found with ID: " + defaultPlanId));
   ```
   When the central database schema is first initialized via Flyway (`V1__init_central_schema.sql`), the `saas_subscription_plans` table is completely empty.
2. **Brittle Dependency on Manual Setup**:
   Before a user can register a gym, a Platform Administrator currently *must* log into `/api/platform-admin` and call `CREATE_SAAS_PLAN` to create Plan ID 1. If this manual step is forgotten or in fresh CI/CD environments, `REGISTER_GYM` fails with an HTTP 500 `IllegalStateException`.
3. **Hardcoded Entitlements Disconnect**:
   The trial duration (`durationDays: 14`) is configured in `application.yaml`, but the trial plan's feature entitlements (maximum members, maximum trainers, analytics access, branding) have no central configuration baseline.

---

## 3. Technical Architecture & Requirements

### 3.1 Externalized Default Plan Configuration in `application.yaml`
Extend `gymverse.saas.trial` to configure the default trial tier specifications alongside duration:

```yaml
gymverse:
  saas:
    trial:
      duration-days: ${GYMVERSE_TRIAL_DAYS:14}
      default-plan-id: ${GYMVERSE_TRIAL_DEFAULT_PLAN_ID:1}
      plan:
        name: ${GYMVERSE_TRIAL_PLAN_NAME:Starter (Free Trial)}
        description: ${GYMVERSE_TRIAL_PLAN_DESC:Default introductory trial plan with standard feature entitlements}
        price: ${GYMVERSE_TRIAL_PLAN_PRICE:29.99}
        billing-cycle: ${GYMVERSE_TRIAL_PLAN_BILLING_CYCLE:MONTHLY}
        max-members: ${GYMVERSE_TRIAL_PLAN_MAX_MEMBERS:100}
        max-trainers: ${GYMVERSE_TRIAL_PLAN_MAX_TRAINERS:5}
        analytics-enabled: ${GYMVERSE_TRIAL_PLAN_ANALYTICS:true}
        multi-trainer-enabled: ${GYMVERSE_TRIAL_PLAN_MULTI_TRAINER:true}
        custom-branding-enabled: ${GYMVERSE_TRIAL_PLAN_BRANDING:false}
```

### 3.2 Configuration Class Enhancement (`TenantConfigProperties.java`)
Enhance `com.company.GymVerse.user.config.TenantConfigProperties` with a nested `@Getter @Setter @Builder` class `DefaultPlanProperties`:
- `String name` (default: `"Starter (Free Trial)"`)
- `String description` (default: `"Default introductory trial plan with standard feature entitlements"`)
- `BigDecimal price` (default: `new BigDecimal("29.99")`)
- `BillingCycle billingCycle` (default: `BillingCycle.MONTHLY`)
- `int maxMembers` (default: `100`)
- `int maxTrainers` (default: `5`)
- `boolean analyticsEnabled` (default: `true`)
- `boolean multiTrainerEnabled` (default: `true`)
- `boolean customBrandingEnabled` (default: `false`)

### 3.3 Startup Seeder Component (`DefaultSaasPlanSeeder.java`)
- **Package**: `com.company.GymVerse.config`
- **Class**: `public class DefaultSaasPlanSeeder implements ApplicationRunner`
- **Annotation**: `@Component`, `@RequiredArgsConstructor`, `@Slf4j`, `@Order(2)` (executing right after PlatformAdmin seeder).
- **Execution Logic**:
  1. Reads `tenantConfigProperties.getDefaultPlanId()`.
  2. Checks if `saasSubscriptionPlanRepository.findById(defaultPlanId).isPresent()`.
  3. If missing:
     - Constructs a `SaasSubscriptionPlan` from `tenantConfigProperties.getPlan()`.
     - Persists it to `saasSubscriptionPlanRepository`.
     - Logs: `INFO: "Seeded default SaaS trial subscription plan: [ID={}, Name='{}', Price={}, MaxMembers={}]"`
  4. Idempotency: If the plan exists, logs debug and skips execution without mutating existing data.

### 3.4 Self-Healing Provisioning in `TenantProvisioningService.java`
Update `TenantProvisioningService` to be resilient:
- If `saasSubscriptionPlanRepository.findById(defaultPlanId)` is not found, it calls `defaultSaasPlanSeeder.seedDefaultPlanIfNotExists()` before throwing an error.
- Ensures zero downtime and self-healing even if startup seeding was bypassed or disabled in test profiles.

---

## 4. Acceptance Criteria & Test Scenarios

### Scenario 1: Fresh Database Startup Seeding
- **Given** an empty `saas_subscription_plans` table on application startup
- **When** `DefaultSaasPlanSeeder.run()` executes
- **Then** a plan with `name = "Starter (Free Trial)"`, `price = 29.99`, `maxMembers = 100`, `maxTrainers = 5`, and `active = true` is persisted to Central DB.

### Scenario 2: Startup Seeder Idempotency
- **Given** an existing `SaasSubscriptionPlan` with `id = defaultPlanId`
- **When** `DefaultSaasPlanSeeder.run()` executes
- **Then** the seeder skips insertion and leaves the existing plan completely unmodified.

### Scenario 3: End-to-End Gym Registration on Clean Database
- **Given** a user registers a gym via `POST /api/user` (`REGISTER_GYM`) on a newly initialized database
- **When** the registration transaction runs
- **Then** `TenantProvisioningService` successfully binds the seeded trial plan to the gym's initial `gym_subscriptions` record (`status: TRIAL`, `pricePaid: 0.00`, `endDate: today + 14 days`).
- **And** no `IllegalStateException` or 500 error occurs.

### Scenario 4: Subscription Insights Alignment
- **Given** a newly registered gym tenant on trial
- **When** calling `POST /api/tenant-admin` with `FETCH_TENANT_SUBSCRIPTION`
- **Then** the response returns `planName: "Starter (Free Trial)"`, `planPrice: 29.99`, `pricePaid: 0.00`, `isTrial: true`, `maxMembers: 100`, and `maxTrainers: 5`.

---

## 5. File Impact Summary

| File | Nature of Change |
| :--- | :--- |
| `src/main/resources/application.yaml` | Add `gymverse.saas.trial.plan.*` properties |
| `src/test/resources/application-test.yaml` | Mirror trial plan properties in test profile |
| `src/main/java/com/company/GymVerse/user/config/TenantConfigProperties.java` | Add nested `DefaultPlanProperties` model |
| `src/main/java/com/company/GymVerse/config/DefaultSaasPlanSeeder.java` | **New**: `ApplicationRunner` seeding default SaaS plan |
| `src/main/java/com/company/GymVerse/user/service/TenantProvisioningService.java` | Add seeder fallback self-healing check |
| `src/test/java/com/company/GymVerse/config/DefaultSaasPlanSeederTest.java` | **New**: Unit tests for seeder idempotency and properties |
| `_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/phase-4-epic.md` | Add Story 4.7 to Phase 4 epic roadmap |
| `_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/dependency-matrix-and-ordering.md` | Add Story 4.7 to dependency matrix |
| `antigravity/project-context.md` | Update Phase 4 status with Story 4.7 |
