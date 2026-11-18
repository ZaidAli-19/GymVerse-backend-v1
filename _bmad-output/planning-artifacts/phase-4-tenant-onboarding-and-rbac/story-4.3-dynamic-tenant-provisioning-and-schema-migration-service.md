---
id: "story-4.3"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Dynamic Tenant Provisioning & Schema Migration Service"
status: "completed"
type: "infrastructure-orchestration"
created_at: "2026-11-18"
dependencies:
  - "story-4.1"
files_to_modify: []
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/TenantProvisioningService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/dto/TenantProvisioningCommand.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/dto/TenantProvisioningResult.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/TenantProvisioningServiceTest.java"
---

# Story 4.3: Dynamic Tenant Provisioning & Schema Migration Service

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Preserve all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.
> 4. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 5. Tenant DB interaction must use `TenantRoutingBridge.callInTenantTx(dbName, ...)` or appropriate isolation boundary.
> 6. Maintain task logging in `antigravity/story-4.3-dynamic-tenant-provisioning-and-schema-migration-service.md`.

---

## 1. User Story

**As a** platform infrastructure architect,  
**I want** a dedicated `TenantProvisioningService` that orchestrates physical database creation, Flyway schema migration, tenant admin profile seeding, central registry updates, and transactional compensation rollback on error,  
**So that** new gym tenants are provisioned cleanly without leaving orphan databases or inconsistent database states if an error occurs.

---

## 2. Technical Context & Requirements

1. **Provisioning Command & Result**:
   - `TenantProvisioningCommand`: Carries user account details, gym metadata (`gymName`, `contactEmail`, `phoneNumber`, `address`, `city`, `logoUrl`, `latitude`, `longitude`, `radius`, `openingTime`, `closingTime`, `timeZone`).
   - `TenantProvisioningResult`: Returns `GymTenant`, `UserTenantRole`, `GymSubscription`, and generated database names/codes.

2. **Database & Code Generation Rules**:
   - Sanitized prefix generator: Converts gym name to lowercase alphanumeric with underscores (e.g., `Gold Gym` ➔ `gold_gym`).
   - `db_name`: `gymverse_tenant_<prefix>_<6-digit-random>` (max 64 chars, ensuring uniqueness against `GymTenantRepository`).
   - `gym_code`: `GYM-<8-char-hex>` (or uppercase alphanumeric unique code).

3. **Orchestration Workflow**:
   - Step 1: Generate unique `gym_code` and `db_name`.
   - Step 2: Create physical MySQL database via `TenantDatabaseService.createDatabase(dbName)`.
   - Step 3: Execute tenant schema Flyway migrations via `TenantFlywayService.migrate(dbName)` to apply `V1__init_tenant_schema.sql`.
   - Step 4: Seed tenant-local DB using `TenantRoutingBridge.callInTenantTx(dbName, ...)`:
     - Insert into tenant `users` table: `central_user_id = userAccount.getId()`, `first_name`, `last_name`, `email`, `role: ADMIN`, `active: true`. **ZERO passwords stored**.
     - Insert into tenant `gym_config` table: geofencing and operating hours.
   - Step 5: Save Central DB records:
     - Insert `GymTenant`: `gym_name`, `gym_code`, `db_name`, `status: ACTIVE`, `registered_at: now`.
     - Insert `UserTenantRole`: `user_account_id`, `gym_tenant_id`, `role: TENANT_ADMIN`, `admin_authority: PRIMARY`, `status: ACTIVE`.
     - Insert `GymSubscription`: `status: TRIAL`, `price_paid: 0.00`, `start_date: today`, `end_date: today + trialDays`.

4. **Compensation / Rollback Guard**:
   - If any step fails after database creation, invoke `tenantDatabaseService.dropDatabase(dbName)`.
   - Clear tenant context: `TenantContext.clear()`.
   - Re-throw descriptive `TenantProvisioningException` (or `BadRequestException`).

---

## 3. Verification & Acceptance Criteria

1. **Unit & Mock Tests (`TenantProvisioningServiceTest.java`)**:
   - Verifies sequential execution: DB creation ➔ Flyway migration ➔ local seed ➔ central records.
   - Verifies compensation rollback: if Flyway or local seeding throws an exception, `tenantDatabaseService.dropDatabase(dbName)` is invoked and database is not orphaned.
   - Verifies tenant `users` seed contains `central_user_id` and zero password credentials.
2. **Context Compilation Check**:
   - Clean build via `./mvnw.cmd test-compile`.
