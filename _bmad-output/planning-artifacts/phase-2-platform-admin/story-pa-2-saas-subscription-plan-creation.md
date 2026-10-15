---
id: "story-pa-2"
epic_id: "epic-pa"
title: "SaaS Subscription Plan Creation"
status: "implemented"
type: "platform-admin-feature"
created_at: "2026-10-15"
dependencies: []
files_to_modify:
  - "src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/exceptions/BadRequestException.java"
  - "src/main/java/com/company/GymVerse/exceptions/ConflictException.java"
  - "src/main/java/com/company/GymVerse/exceptions/ErrorResponse.java"
  - "src/main/java/com/company/GymVerse/exceptions/GlobalExceptionHandler.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/factory/CreateSaasPlanFactory.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/request/CreateSaasPlanRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/response/CreateSaasPlanResponse.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/mapper/SaasPlanMapper.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/mapper/SaasPlanMapperImpl.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/CreateSaasPlanService.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/CreateSaasPlanIntegrationTest.java"
---

# Story PA-2: SaaS Subscription Plan Creation

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.SaasSubscriptionPlan` and `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`.  
> 3. You must follow the modular folder structure (`platformAdmin/{controller, factory, service, request, response, validations, mapper}`) and centralized exception handling (`exceptions/`).  
> 4. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 5. You must document the API contract in `/docs/payloads.md` with all 13 mandatory fields and copy-pasteable cURL commands (`PROMPT.md` Section 10).  
> 6. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** GymVerse Platform Administrator,  
**I want to** create new SaaS subscription plans specifying pricing, billing cycles, capacity limits, and platform feature entitlements,  
**So that** gym facilities can subscribe to standardized, tiered offerings when registering or renewing their subscriptions on the platform.

---

## 2. Business & Technical Context

In the GymVerse multi-tenant architecture ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 4.1 & 7), the central platform database maintains the catalog of available SaaS tiers in table `saas_subscription_plans`:
- `id` (BIGINT PK)
- `name` (VARCHAR(100) NOT NULL)
- `description` (TEXT)
- `price` (DECIMAL(10,2) NOT NULL) — Strict `BigDecimal` representation per `PROMPT.md` Section 4.1.
- `billing_cycle` (VARCHAR(30) NOT NULL: `MONTHLY`, `QUARTERLY`, `ANNUAL`)
- `max_members` (INT NOT NULL)
- `max_trainers` (INT NOT NULL)
- `analytics_enabled` (BOOLEAN NOT NULL DEFAULT FALSE)
- `multi_trainer_enabled` (BOOLEAN NOT NULL DEFAULT FALSE)
- `custom_branding_enabled` (BOOLEAN NOT NULL DEFAULT FALSE)
- `active` (BOOLEAN NOT NULL DEFAULT TRUE)

### The Architectural Fit:
1. **Concrete Operation-Specific Factory Pattern**: In alignment with `RULES.md` Rule 4 and `PROMPT.md` Section 3.1 & Section 9, this mutation is handled by a concrete operation-specific factory (`CreateSaasPlanFactory`) that accepts `CreateSaasPlanRequest` and returns `CreateSaasPlanResponse`. The factory orchestrates validation (`PlatformAdminValidations`) and delegates persistence to single-responsibility service (`CreateSaasPlanService`).
2. **Separated Validation**: Domain checks (e.g. duplicate plan name detection, positive non-null price invariants, member limit minimums) reside in `PlatformAdminValidationsImpl`.
3. **Handwritten Mapper**: DTO transformation is executed by `SaasPlanMapperImpl`.
4. **Centralized Exception Handling**: Validation failures and collisions throw `BadRequestException` or `ConflictException`, handled uniformly by `@RestControllerAdvice` in `exceptions/GlobalExceptionHandler`.

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Phase 1 central persistence foundation active (`SaasSubscriptionPlan` entity and `SaasSubscriptionPlanRepository`).
  - Central database connection pool active.
- **Dependencies**:
  - Foundational story for the SaaS plan lifecycle. Stories PA-3, PA-4, and PA-5 depend on plan records created here.

---

## 4. Scope & Boundaries

### In Scope
1. **Request & Response DTOs**:
   - `CreateSaasPlanRequest`: contains `name`, `description`, `price` (BigDecimal), `billingCycle` (`BillingCycle`), `maxMembers`, `maxTrainers`, `analyticsEnabled`, `multiTrainerEnabled`, `customBrandingEnabled`.
   - `CreateSaasPlanResponse`: contains resulting plan ID, all plan attributes, active status, and a confirmation message.
2. **Domain Validations (`PlatformAdminValidations`)**:
   - `validateCreatePlan(CreateSaasPlanRequest request)`:
     - Rejects blank or null plan names (max 100 characters).
     - Checks if a plan with the same name already exists (case-insensitive) -> throws `ConflictException("A plan with name '" + name + "' already exists")`.
     - Validates that `price` is non-null, scale <= 2, and >= 0.00 -> throws `BadRequestException`.
     - Validates that `billingCycle` is non-null.
     - Validates that `maxMembers >= 1` and `maxTrainers >= 0`.
3. **Service Operation (`CreateSaasPlanService`)**:
   - Dedicated service method: `CreateSaasPlanResponse createPlan(CreateSaasPlanRequest request)`.
   - Executes under `@Transactional("centralTransactionManager")`.
   - Maps request to entity, sets `active = true`, persists to `saas_subscription_plans`, and logs milestone via `@Slf4j`.
4. **Factory & Controller Integration**:
   - `CreateSaasPlanFactory` orchestrates `PlatformAdminValidations` and delegates to `CreateSaasPlanService`.
   - `PlatformAdminController` exposes `POST /api/platform-admin/plans` and invokes `CreateSaasPlanFactory.execute(request)`.
5. **Standardized Error Handling**:
   - `GlobalExceptionHandler` returning `ErrorResponse` schema matching `PROMPT.md` Section 4.3.
6. **API Contract Documentation**:
   - Add endpoint documentation to `docs/payloads.md` with all 13 mandatory fields.

### Out of Scope
- Implementing client authentication, JWT signing, or login flows (Story Auth).
- Modifying gym tenant operational tables or physical databases.
- Modifying the 8 protected multitenancy classes.

---

## 5. Detailed Acceptance Criteria

### AC-PA2.1: Successful SaaS Plan Creation
- **Given** valid plan details:
  - `name`: "Enterprise Gold"
  - `description`: "Full tier gym plan with unlimited members and custom branding"
  - `price`: `199.99`
  - `billingCycle`: `MONTHLY`
  - `maxMembers`: `1000`
  - `maxTrainers`: `25`
  - `analyticsEnabled`: `true`
  - `multiTrainerEnabled`: `true`
  - `customBrandingEnabled`: `true`
- **When** the platform admin submits the plan creation request,
- **Then** the service persists the plan to `saas_subscription_plans` under `centralTransactionManager`,
- **And** returns HTTP `201 Created` (or `200 OK` via Factory) with `CreateSaasPlanResponse` containing:
  - `id`: Generated non-null `Long`.
  - `name`: "Enterprise Gold".
  - `price`: `199.99` (BigDecimal with scale 2).
  - `active`: `true`.
  - `message`: "SaaS subscription plan created successfully".

### AC-PA2.2: Duplicate Plan Name Rejection
- **Given** a SaaS plan named "Starter" already exists in the central database,
- **When** a request attempts to create another plan with name "Starter" (or "starter"),
- **Then** `PlatformAdminValidations` detects the name collision,
- **And** throws `ConflictException("A plan with name 'Starter' already exists")`,
- **And** the endpoint returns HTTP `409 Conflict` with the standardized error response.

### AC-PA2.3: Monetary Precision & Range Validation
- **Given** a plan creation request with `price = -10.00` or `price = null`,
- **When** validation executes,
- **Then** it throws `BadRequestException("Price must be a non-negative decimal value")`,
- **And** returns HTTP `400 Bad Request`.

### AC-PA2.4: Capacity Limits Validation
- **Given** a plan creation request with `maxMembers = 0` or `maxTrainers = -1`,
- **When** validation executes,
- **Then** it throws `BadRequestException("Maximum members must be at least 1 and trainers must be non-negative")`,
- **And** returns HTTP `400 Bad Request`.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java`:
   - Add: `boolean existsByNameIgnoreCase(String name);`
2. `docs/payloads.md`:
   - Append complete 13-field API documentation with cURL example for plan creation.

### Files to Create:
1. `src/main/java/com/company/GymVerse/exceptions/BadRequestException.java`
2. `src/main/java/com/company/GymVerse/exceptions/ConflictException.java`
3. `src/main/java/com/company/GymVerse/exceptions/ErrorResponse.java`
4. `src/main/java/com/company/GymVerse/exceptions/GlobalExceptionHandler.java`
5. `src/main/java/com/company/GymVerse/platformAdmin/factory/CreateSaasPlanFactory.java`
6. `src/main/java/com/company/GymVerse/platformAdmin/request/CreateSaasPlanRequest.java`
7. `src/main/java/com/company/GymVerse/platformAdmin/response/CreateSaasPlanResponse.java`
8. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java`
9. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java`
10. `src/main/java/com/company/GymVerse/platformAdmin/mapper/SaasPlanMapper.java`
11. `src/main/java/com/company/GymVerse/platformAdmin/mapper/SaasPlanMapperImpl.java`
12. `src/main/java/com/company/GymVerse/platformAdmin/service/CreateSaasPlanService.java`
13. `src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java`
14. `src/test/java/com/company/GymVerse/platformAdmin/CreateSaasPlanIntegrationTest.java`

---

## 7. Error Handling & Security Considerations

1. **Security Dependency Note**:
   - In production, this endpoint will require a central access token with `isPlatformAdmin = true`.
   - In accordance with the prompt constraints, live authentication/JWT verification is excluded from this epic. Tests will invoke controller methods using MockMvc with mock authentication context or test profiles.
2. **Transactional Safety**:
   - The operation executes under `@Transactional("centralTransactionManager")`, ensuring database rollbacks on unhandled errors.

---

## 8. Test Requirements

1. **`CreateSaasPlanIntegrationTest`**:
   - Use MockMvc against `PlatformAdminController`.
   - **Test 1**: Verify successful plan creation with valid payload returns HTTP 201/200, persists record to MySQL `saas_subscription_plans`, and verifies exact `BigDecimal` price.
   - **Test 2**: Verify duplicate plan name returns HTTP 409 Conflict with proper JSON error schema.
   - **Test 3**: Verify negative price or zero members returns HTTP 400 Bad Request.
   - **Test 4**: Verify database cleanup in `@AfterEach`.

---

## 9. Definition of Done

- Code compiles cleanly (`./mvnw compile`).
- Factory and operation beans auto-register via Spring dependency injection.
- Unit and integration tests verify successful creation and error handling against live MySQL.
- API payloads documentation updated in `/docs/payloads.md` with all 13 required fields.
- Task log created in `antigravity/` detailing execution and rationale.
