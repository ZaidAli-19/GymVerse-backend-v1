---
id: "story-pa-4"
epic_id: "epic-pa"
title: "Single SaaS Subscription Plan Retrieval by Identifier"
status: "planning"
type: "platform-admin-feature"
created_at: "2026-10-15"
dependencies:
  - "story-pa-2"
files_to_modify:
  - "src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/exceptions/ResourceNotFoundException.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/GetSaasPlanByIdService.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/GetSaasPlanByIdIntegrationTest.java"
---

# Story PA-4: Single SaaS Subscription Plan Retrieval by Identifier

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.SaasSubscriptionPlan` and `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`.  
> 3. You must throw a domain-specific `ResourceNotFoundException` when a target plan is not found, mapped to HTTP `404 Not Found` by `GlobalExceptionHandler`.  
> 4. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 5. You must document the API contract in `/docs/payloads.md` with all 13 mandatory fields and copy-pasteable cURL commands (`PROMPT.md` Section 10).  
> 6. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** GymVerse Platform Administrator,  
**I want to** retrieve the full details of a specific SaaS subscription plan by its identifier,  
**So that** I can inspect its complete pricing structure, capacity limits, feature toggles, and status for auditing or operational review.

---

## 2. Business & Technical Context

Platform management frequently requires drilling into individual plan configurations ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 4.1 & 7). 
- An administrator needs to look up a plan by its primary key ID (`id`).
- If the plan exists, the application returns full details formatted as `SaasPlanResponse` with HTTP `200 OK`.
- If the plan does not exist, the application must throw a clean `ResourceNotFoundException`, which `GlobalExceptionHandler` converts to standard HTTP `404 Not Found` with a descriptive error message matching `PROMPT.md` Section 4.3.
- The query executes against the central MySQL database under `centralTransactionManager` (read-only mode).

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Phase 1 central persistence foundation active (`SaasSubscriptionPlanRepository`).
  - Story PA-2 completed (`SaasPlanResponse` and `SaasPlanMapper` created).
- **Dependencies**:
  - Depends on Story PA-2 for DTOs and base exception handlers.

---

## 4. Scope & Boundaries

### In Scope
1. **Domain Exception (`ResourceNotFoundException`)**:
   - `ResourceNotFoundException extends RuntimeException` annotated or mapped in `GlobalExceptionHandler` to HTTP `404 Not Found`.
2. **Service Component (`GetSaasPlanByIdService`)**:
   - Single-responsibility read service executing under `@Transactional(transactionManager = "centralTransactionManager", readOnly = true)`.
   - Method: `SaasPlanResponse getPlanById(Long planId)`.
   - Uses `saasSubscriptionPlanRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("SaasSubscriptionPlan not found with id: " + planId))`.
   - Maps entity to response DTO via `SaasPlanMapper`.
3. **Controller Endpoint**:
   - Exposes `GET /api/platform-admin/plans/{id}` with path variable `id`.
   - Delegates directly to `GetSaasPlanByIdService` following the read query service pattern.
4. **API Documentation**:
   - Append 13-field API specification with path parameter details and 404 error example to `/docs/payloads.md`.

### Out of Scope
- Mutating plan records or database states.
- Client authentication or JWT parsing.
- Accessing tenant databases.

---

## 5. Detailed Acceptance Criteria

### AC-PA4.1: Successful Plan Lookup by ID
- **Given** an existing SaaS plan with `id = 5`, `name = "Growth Tier"`, and `price = 79.50`,
- **When** the platform admin calls `GET /api/platform-admin/plans/5`,
- **Then** the service retrieves the plan entity,
- **And** returns HTTP `200 OK` with `SaasPlanResponse`:
  - `id`: `5`
  - `name`: "Growth Tier"
  - `price`: `79.50`
  - All feature flags and status matching database state.

### AC-PA4.2: Non-Existent Plan ID (404 Error)
- **Given** no plan exists with `id = 9999`,
- **When** the platform admin calls `GET /api/platform-admin/plans/9999`,
- **Then** `GetSaasPlanByIdService` throws `ResourceNotFoundException("SaasSubscriptionPlan not found with id: 9999")`,
- **And** `GlobalExceptionHandler` returns HTTP `404 Not Found` with body:
  ```json
  {
    "timestamp": "...",
    "status": 404,
    "error": "Not Found",
    "message": "SaasSubscriptionPlan not found with id: 9999",
    "path": "/api/platform-admin/plans/9999"
  }
  ```

### AC-PA4.3: Invalid Plan ID Format (400 Error)
- **Given** a request with an invalid non-numeric ID `GET /api/platform-admin/plans/abc`,
- **When** Spring MVC parses the path variable,
- **Then** `MethodArgumentTypeMismatchException` is caught by `GlobalExceptionHandler` returning HTTP `400 Bad Request`.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java`:
   - Add `GET /api/platform-admin/plans/{id}` endpoint.
2. `src/main/java/com/company/GymVerse/exceptions/GlobalExceptionHandler.java`:
   - Add handler for `ResourceNotFoundException`.
3. `docs/payloads.md`:
   - Append 13-field API documentation, path parameters, and cURL commands for plan retrieval by ID.

### Files to Create:
1. `src/main/java/com/company/GymVerse/exceptions/ResourceNotFoundException.java`
2. `src/main/java/com/company/GymVerse/platformAdmin/service/GetSaasPlanByIdService.java`
3. `src/test/java/com/company/GymVerse/platformAdmin/GetSaasPlanByIdIntegrationTest.java`

---

## 7. Error Handling & Security Considerations

1. **Security Context**:
   - In production, protected by Platform Admin role check (`ROLE_PLATFORM_ADMIN`).
2. **Defensive Coding**:
   - Path variable `id` is strictly checked for null/negative values before repository invocation.

---

## 8. Test Requirements

1. **`GetSaasPlanByIdIntegrationTest`**:
   - Seed a test plan in MySQL central database.
   - **Test 1**: Verify retrieving valid ID returns HTTP 200 with matching entity attributes.
   - **Test 2**: Verify non-existent ID returns HTTP 404 Not Found with standardized error schema.
   - **Test 3**: Verify invalid non-numeric ID returns HTTP 400 Bad Request.

---

## 9. Definition of Done

- Code compiles cleanly (`./mvnw compile`).
- Endpoint returns plan details with correct DTO structure and HTTP 200.
- Missing resources return clean HTTP 404.
- Integration tests pass against live MySQL central database.
- `/docs/payloads.md` updated with 13 mandatory fields.
- Task log maintained in `antigravity/`.
