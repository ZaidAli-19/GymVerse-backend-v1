# Task Log: Story 4.3 - Dynamic Tenant Provisioning & Schema Migration Service

## Status
- **Status:** COMPLETED
- **Branch:** `GymVerse/dev`
- **Execution Date:** 2026-11-18
- **Epic:** Phase 4 - Tenant Onboarding, Dynamic Database Provisioning, Scoped RBAC, & Trial Engine

---

## 1. Objectives & Scope
Implement the dynamic tenant provisioning and schema migration orchestrator service (`TenantProvisioningService`) that:
1. Validates provisioning commands and parameters.
2. Generates unique tenant identifier code (`GYM-<8-hex>`) and sanitized MySQL database name (`gymverse_tenant_<sanitized_prefix>_<6-digit-random>`).
3. Provisions isolated physical MySQL database using `TenantDatabaseService.createDatabase(dbName)`.
4. Executes tenant-scoped Flyway migrations using `TenantFlywayService.migrate(dbName)` to apply `V1__init_tenant_schema.sql`.
5. Seeds tenant-local database via `TenantRoutingBridge.callInTenantTx(dbName, ...)`:
   - Tenant administrator profile (`TenantUser` with `central_user_id`, role `TENANT_ADMIN`, and **zero passwords**).
   - Facility geofencing and operational hours (`GymConfig`).
6. Persists central registry records within central transaction:
   - `GymTenant`: metadata and discovery codes.
   - `UserTenantRole`: role `TENANT_ADMIN`, admin authority `PRIMARY`, status `ACTIVE`.
   - `GymSubscription`: SaaS plan attachment, status `TRIAL`, zero price paid, trial duration from `TenantConfigProperties`.
7. Guarantees transactional compensation rollback: on any failure during or after database creation, evicts datasource cache (`tenantDataSourceCache.evict`), drops the orphan database (`tenantDatabaseService.dropDatabase`), cleans up `TenantContext.clear()`, and re-throws `TenantProvisioningException`.

---

## 2. Changes Made

### Files Created:
1. [`TenantProvisioningCommand.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/dto/TenantProvisioningCommand.java):
   - Located in `com.company.GymVerse.tenantAdmin.dto`. Encapsulates caller identity (`UserAccount`) and gym metadata (name, email, phone, address, city, logo, coordinates, operating hours, custom code).
2. [`TenantProvisioningResult.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/dto/TenantProvisioningResult.java):
   - Located in `com.company.GymVerse.tenantAdmin.dto`. Value object encapsulating provisioned central entities (`GymTenant`, `UserTenantRole`, `GymSubscription`), tenant entities (`TenantUser`, `GymConfig`), and connection metadata (`gymCode`, `dbName`, `trialEndDate`, `trialDaysRemaining`).
3. [`TenantAdminValidations.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java) & [`TenantAdminValidationsImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java):
   - Dedicated validation layer (`com.company.GymVerse.tenantAdmin.validations`) implementing domain invariants, non-null checks, blank string guards, and custom gym code collision checks.
4. [`TenantProvisioningException.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/exceptions/TenantProvisioningException.java):
   - Domain exception for unrecoverable provisioning failures.
5. [`TenantProvisioningService.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/service/TenantProvisioningService.java):
   - Orchestration workflow with compensation rollback, delegating validations to `TenantAdminValidations`.
6. [`TenantProvisioningServiceTest.java`](file:///D:/projects/GymVerse-Backend-v2/src/test/java/com/company/GymVerse/tenantAdmin/service/TenantProvisioningServiceTest.java):
   - Unit test suite verifying happy paths, custom gym codes, and compensation rollbacks.
7. [`TenantAdminValidationsTest.java`](file:///D:/projects/GymVerse-Backend-v2/src/test/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsTest.java):
   - 10 unit tests verifying all validation invariants and collision guards.

### Files Modified:
1. [`GlobalExceptionHandler.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/handler/GlobalExceptionHandler.java):
   - Added handler for `TenantProvisioningException` returning HTTP 500 `ErrorResponse`.

---

## 3. Verification & Test Execution
- Run command: `.\mvnw.cmd test "-Dtest=TenantAdminValidationsTest,TenantProvisioningServiceTest,TenantConfigPropertiesTest,SubscriptionDateUtilTest"`
- Results: 31 tests run, 0 failures, 0 errors.
- Architectural invariant check: All 8 multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
