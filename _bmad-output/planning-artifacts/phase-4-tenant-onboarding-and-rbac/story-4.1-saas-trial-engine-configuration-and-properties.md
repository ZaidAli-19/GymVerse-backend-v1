---
id: "story-4.1"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "SaaS Trial Engine Configuration & Properties"
status: "implemented"
type: "configuration-foundation"
created_at: "2026-11-18"
dependencies: []
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/config/TenantConfigProperties.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/util/SubscriptionDateUtil.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/config/TenantConfigPropertiesTest.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/util/SubscriptionDateUtilTest.java"
---

# Story 4.1: SaaS Trial Engine Configuration & Properties

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Preserve all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.
> 4. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 5. Maintain task logging in `antigravity/story-4.1-saas-trial-engine-configuration-and-properties.md`.

---

## 1. User Story

**As a** SaaS platform administrator,  
**I want** to externalize SaaS trial period duration and default plan configuration properties into `application.yaml` with a strongly typed configuration properties bean and date calculation utilities,  
**So that** trial days can be adjusted easily without modifying codebase logic, and subscription end dates can be computed accurately.

---

## 2. Technical Context & Requirements

1. **Configuration Properties (`TenantConfigProperties.java`)**:
   - Location: `com.company.GymVerse.tenantAdmin.config.TenantConfigProperties`.
   - Annotated with `@ConfigurationProperties(prefix = "gymverse.saas.trial")`.
   - Fields:
     - `int durationDays` (default: 14, min: 1).
     - `Long defaultPlanId` (default: 1L, nullable or positive).
   - Validations: `@Min(1)` on durationDays.
   - Register or enable in Spring configuration via `@Configuration` / `@EnableConfigurationProperties`.

2. **Application YAML Updates (`application.yaml` & `application-test.yaml`)**:
   - Add property block:
     ```yaml
     gymverse:
       saas:
         trial:
           duration-days: ${GYMVERSE_TRIAL_DAYS:14}
           default-plan-id: ${GYMVERSE_TRIAL_DEFAULT_PLAN_ID:1}
     ```

3. **Date Utility (`SubscriptionDateUtil.java`)**:
   - Helper methods:
     - `LocalDate calculateTrialEndDate(LocalDate startDate, int durationDays)`: returns `startDate.plusDays(durationDays)`.
     - `long calculateDaysRemaining(LocalDate endDate, LocalDate currentDate)`: returns days remaining until expiration (0 if past).
     - `boolean isTrialActive(LocalDate endDate, LocalDate currentDate)`: returns `!currentDate.isAfter(endDate)`.

---

## 3. Verification & Acceptance Criteria

1. **Unit Tests**:
   - `TenantConfigPropertiesTest`: Verifies defaults (14 days, default plan 1L) and property binding overrides.
   - `SubscriptionDateUtilTest`: Verifies trial end date math, remaining days calculation (active, expired, expiring today).
2. **Context Compilation Check**:
   - `./mvnw.cmd test-compile` passes with zero errors.
