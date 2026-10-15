# Platform Admin Module: Master Execution Plan (Amended)

This document defines the end-to-end execution sequence, environmental prerequisites, verification strategy, and operational guardrails for implementing the **Platform Admin Module** of `GymVerse-Backend-v2`, amended for `platformAdminSeed.yml` configuration and JPA Specifications fetch services.

---

## 1. Prerequisites & Environmental Readiness

Before any implementing agent executes code in this epic, the following prerequisites must be confirmed:

1. **Development Environment**:
   - Java Development Kit (JDK) 21 installed and active.
   - Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` on Linux/macOS) verified operational.
   - MySQL 8.0+ running on `localhost:3306` with database `GymVerse` accessible using credentials in `application.yaml`.
2. **Phase 1 Persistence Integrity**:
   - Central database schema migration `V1__init_central_schema.sql` applied successfully.
   - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% bit-for-bit unchanged.
   - Existing integration tests pass cleanly:
     ```powershell
     ./mvnw test -Dtest=ApplicationTests,CentralPersistenceIntegrationTest,TenantPersistenceIntegrationTest,TenantIsolationIntegrationTest
     ```
3. **Engineering Directives**:
   - The implementing agent must read [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before beginning work.

---

## 2. Phased Execution Sequence

The module implementation is divided into four cohesive execution stages:

```
┌─────────────────────────────────────────────────────────────┐
│ STAGE 1: ADMINISTRATIVE BOOTSTRAP (Story PA-1)              │
│ - Dedicated configuration file: platformAdminSeed.yml       │
│ - Mandatory import in application.yaml                      │
│ - Security crypto config (BCryptPasswordEncoder)            │
│ - Configuration binding (PlatformAdminSeedProperties)       │
│ - PlatformAdminProvisioningService (Safe conflict guard)    │
│ - Verification: Properties binding & Idempotency tests      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ STAGE 2: MODULE SCAFFOLDING & MUTATIONS (Story PA-2)        │
│ - Package layout & GlobalExceptionHandler hierarchy         │
│ - Concrete CreateSaasPlanFactory & DTOs                     │
│ - SaasPlanMapper, Validations & CreateSaasPlanService       │
│ - Documentation: Update /docs/payloads.md                   │
│ - Verification: CreateSaasPlanIntegrationTest               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ STAGE 3: PLAN LOOKUP & IMMUTABILITY (Stories PA-4 & PA-5)   │
│ - ResourceNotFoundException & GetSaasPlanByIdService        │
│   (Retains direct single-resource retrieval semantics)      │
│ - UpdatePlanAvailabilityService (Isolated active toggle)    │
│ - Immutability verification of price/capacity attributes    │
│ - Documentation: Update /docs/payloads.md                   │
│ - Verification: GetSaasPlanById & UpdateAvailability Tests  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ STAGE 4: DYNAMIC FETCH SERVICES VIA JPA SPECS (PA-3 & PA-6) │
│ - Repositories extend JpaSpecificationExecutor              │
│ - SaasPlanSpecification & GymTenantSpecification            │
│ - Whitelist-guarded sorting & validated pagination          │
│ - Standardized PagedResponse<T> envelope                    │
│ - Documentation: Update /docs/payloads.md                   │
│ - Verification: GetAllPlans & GetGymTenants Tests           │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Stage Breakdown & Quality Gates

### Stage 1: Administrative Bootstrap (Story PA-1)
- **Goal:** Enable the platform to bind seed configuration from the dedicated `platformAdminSeed.yml` file and automatically provision an initial platform administrator safely upon boot.
- **Key Artifacts:**
  - `src/main/resources/platformAdminSeed.yml` (`gymverse.platform-admin.seed.*` configuration)
  - `src/main/resources/application.yaml` (`spring.config.import: classpath:platformAdminSeed.yml`)
  - `src/main/java/com/company/GymVerse/config/PlatformAdminSeedProperties.java`
  - `src/main/java/com/company/GymVerse/config/SecurityCryptoConfig.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/PlatformAdminProvisioningService.java`
  - `src/main/java/com/company/GymVerse/config/PlatformAdminBootstrapConfig.java`
- **Verification Commands:**
  ```powershell
  ./mvnw test -Dtest=PlatformAdminSeedPropertiesTest,PlatformAdminProvisioningIntegrationTest
  ```
- **Quality Gate:**
  - `platformAdminSeed.yml` is imported via `spring.config.import`.
  - Missing `platformAdminSeed.yml` causes immediate startup failure (mandatory import).
  - Seed properties bind cleanly via `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")`.
  - Password placeholder `${PLATFORM_ADMIN_PASSWORD}` has no literal default fallback; missing password when `enabled = true` aborts startup.
  - Initial run creates admin with verified BCrypt hash.
  - Subsequent runs perform an idempotent no-op without resetting passwords.
  - Pre-existing non-admin account conflict aborts seeding with `IllegalStateException` without silent promotion.

### Stage 2: Module Scaffolding & Plan Creation (Story PA-2)
- **Goal:** Establish the `platformAdmin` module package, concrete operation-specific factory, centralized exception handlers, and plan creation operation.
- **Key Artifacts:**
  - `src/main/java/com/company/GymVerse/exceptions/{BadRequestException, ConflictException, GlobalExceptionHandler, ErrorResponse}.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/factory/CreateSaasPlanFactory.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/request/CreateSaasPlanRequest.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/response/CreateSaasPlanResponse.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/CreateSaasPlanService.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/mapper/SaasPlanMapperImpl.java`
  - `docs/payloads.md` (contract documentation)
- **Verification Command:**
  ```powershell
  ./mvnw test -Dtest=CreateSaasPlanIntegrationTest
  ```
- **Quality Gate:**
  - Plan created with exact `BigDecimal` price.
  - Duplicate plan names rejected with HTTP 409 Conflict.
  - Invalid prices or limits rejected with HTTP 400 Bad Request.
  - Documentation updated with all 13 fields.

### Stage 3: Plan Lookup & Isolated Availability Management (Stories PA-4 & PA-5)
- **Goal:** Implement direct single-plan lookup by ID and surgical availability toggling, guaranteeing price immutability.
- **Key Artifacts:**
  - `src/main/java/com/company/GymVerse/exceptions/ResourceNotFoundException.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/GetSaasPlanByIdService.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/request/UpdatePlanAvailabilityRequest.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/UpdatePlanAvailabilityService.java`
  - `docs/payloads.md` (contract documentation)
- **Verification Commands:**
  ```powershell
  ./mvnw test -Dtest=GetSaasPlanByIdIntegrationTest,UpdatePlanAvailabilityIntegrationTest
  ```
- **Quality Gate:**
  - Missing plan IDs return HTTP 404 with standard error body.
  - Single-resource retrieval retains direct lookup semantics without forcing pagination.
  - Toggling `active` updates only the boolean flag; database assertions confirm `price`, `billing_cycle`, and `max_members` are identical byte-for-byte.

### Stage 4: Dynamic Query Services via JPA Specifications (Stories PA-3 & PA-6)
- **Goal:** Implement dynamic filtering, whitelisted sorting, and validated pagination for SaaS plans and registered gym tenants using Spring Data JPA Specifications.
- **Key Artifacts:**
  - `src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java` (extends `JpaSpecificationExecutor`)
  - `src/main/java/com/company/GymVerse/repositories/central/GymTenantRepository.java` (extends `JpaSpecificationExecutor`)
  - `src/main/java/com/company/GymVerse/platformAdmin/specifications/SaasPlanSpecification.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/specifications/GymTenantSpecification.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/response/PagedResponse.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/GetAllSaasPlansService.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/GetGymTenantsService.java`
  - `docs/payloads.md` (contract documentation)
- **Verification Commands:**
  ```powershell
  ./mvnw test -Dtest=GetAllSaasPlansIntegrationTest,GetGymTenantsIntegrationTest
  ```
- **Quality Gate:**
  - Filtering, sorting, and pagination execute directly in MySQL queries via JPA Specifications (zero in-memory filtering).
  - Sort fields are validated against whitelists; invalid fields return HTTP 400 Bad Request.
  - Pagination parameters are validated (`page >= 0`, `1 <= size <= 100`).
  - Responses match standardized `PagedResponse<T>` schema.
  - Zero database context switching into tenant databases.

---

## 4. End-to-End Verification Strategy

At the completion of all stories, the full test suite must be executed to guarantee zero regressions across Phase 1 and Phase 2:

```powershell
# 1. Clean Compilation Check
./mvnw clean compile

# 2. Complete Integration Test Suite Execution
./mvnw test -Dtest=ApplicationTests,CentralPersistenceIntegrationTest,TenantPersistenceIntegrationTest,TenantIsolationIntegrationTest,PlatformAdminSeedPropertiesTest,PlatformAdminProvisioningIntegrationTest,CreateSaasPlanIntegrationTest,GetSaasPlanByIdIntegrationTest,UpdatePlanAvailabilityIntegrationTest,GetAllSaasPlansIntegrationTest,GetGymTenantsIntegrationTest
```

### Verification Checklist:
- [ ] Exit code 0 on all test executions.
- [ ] Central database tables (`user_accounts`, `saas_subscription_plans`, `gym_tenants`) populated and cleaned properly in `@AfterEach`.
- [ ] The 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% bit-for-bit unchanged (hash comparison).
- [ ] `/docs/payloads.md` contains entries for all new endpoints with copy-pasteable cURL commands and 13 mandatory items.
- [ ] A dedicated engineering record is committed in `/antigravity/` detailing all implemented components and verification proof.

---

## 5. Strict Guardrails & Protection Rules

1. **NO APPLICATION CODE EDITS DURING PLANNING**:
   - This plan must be formally reviewed and approved by the user before any implementation begins.
2. **ZERO AUTHENTICATION IMPLEMENTATION**:
   - Do NOT implement login, logout, registration OTPs, JWT issuance, or refresh token filters in this module.
3. **PROHIBITION OF MULTITENANCY CLASS MUTATION**:
   - Under no circumstances may any file in `com.company.GymVerse.multitanancy` be modified.
4. **STOP AND WAIT**:
   - The implementing agent must stop after authoring planning artifacts and await explicit user approval.
