---
id: "story-pa-5"
epic_id: "epic-pa"
title: "SaaS Subscription Plan Availability Management"
status: "planning"
type: "platform-admin-feature"
created_at: "2026-10-15"
dependencies:
  - "story-pa-2"
  - "story-pa-4"
files_to_modify:
  - "src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/platformAdmin/factory/UpdatePlanAvailabilityFactory.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/request/UpdatePlanAvailabilityRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/response/UpdatePlanAvailabilityResponse.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/UpdatePlanAvailabilityService.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/UpdatePlanAvailabilityIntegrationTest.java"
---

# Story PA-5: SaaS Subscription Plan Availability Management

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.SaasSubscriptionPlan` and `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`.  
> 3. **CRITICAL IMMUTABILITY INVARIANT:** This operation must strictly update **only** the plan's availability status (`active = true/false`). It must **never** modify the plan's price, billing cycle, max members, max trainers, or feature entitlements, as doing so would corrupt historical and active tenant subscription agreements.  
> 4. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 5. You must document the API contract in `/docs/payloads.md` with all 13 mandatory fields and copy-pasteable cURL commands (`PROMPT.md` Section 10).  
> 6. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** GymVerse Platform Administrator,  
**I want to** activate or deactivate a SaaS subscription plan's availability without altering any other plan attributes,  
**So that** I can retire obsolete tiers or introduce newly finalized tiers to prospective gyms while guaranteeing absolute price and capacity immutability for existing subscribers.

---

## 2. Business & Technical Context

In the GymVerse multi-tenant architecture ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 4.1 & 7):
- When gyms register, they subscribe to a plan from `saas_subscription_plans`, and an agreement is recorded in `gym_subscriptions` referencing `plan_id` and recording `price_paid`.
- If a generic "update plan" endpoint (e.g. `PUT /plans/{id}`) accepts all attributes, a client error could overwrite the `price`, `billingCycle`, or capacity limits of a plan already in active use by dozens of gyms.
- Therefore, enterprise SaaS safety mandates an **isolated, surgical availability management operation**:
  - The request payload accepts exclusively the plan ID and the target availability flag (`active: boolean`).
  - The service updates *only* `saas_subscription_plans.active`.
  - All other fields (`price`, `billing_cycle`, `name`, `max_members`, `max_trainers`, etc.) remain completely untouched.

### The Architectural Fit:
1. **Concrete Operation-Specific Factory Pattern**: Handled by a dedicated operation-specific factory (`UpdatePlanAvailabilityFactory`) that accepts `UpdatePlanAvailabilityRequest` and returns `UpdatePlanAvailabilityResponse`. The factory orchestrates validation (`PlatformAdminValidations`) and delegates persistence to single-responsibility service (`UpdatePlanAvailabilityService`).
2. **Dedicated DTO**: `UpdatePlanAvailabilityRequest` contains only `planId` and `active`.
3. **Transaction Boundary**: Executes under `@Transactional("centralTransactionManager")`.

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Phase 1 central persistence foundation active (`SaasSubscriptionPlanRepository`).
  - Story PA-2 and PA-4 completed (`ResourceNotFoundException` and `SaasPlanMapper` available).
- **Dependencies**:
  - Depends on Story PA-2 for mapper/validation baseline and Story PA-4 for `ResourceNotFoundException`.

---

## 4. Scope & Boundaries

### In Scope
1. **Request DTO (`UpdatePlanAvailabilityRequest`)**:
   - Fields: `Long planId`, `Boolean active`.
   - Annotated with `@NotNull` validation.
2. **Domain Validations (`PlatformAdminValidations`)**:
   - `validateUpdatePlanAvailability(UpdatePlanAvailabilityRequest request)`:
     - Verifies `request.getPlanId()` is non-null and positive.
     - Verifies `request.getActive()` is non-null.
3. **Service Operation (`UpdatePlanAvailabilityService`)**:
   - Retrieves existing entity using `saasSubscriptionPlanRepository.findById(request.getPlanId())`.
   - Throws `ResourceNotFoundException` if plan does not exist.
   - Sets *only* `plan.setActive(request.getActive())`.
   - Persists updated entity under `@Transactional("centralTransactionManager")`.
   - Logs status change at `INFO` level.
   - Returns updated `UpdatePlanAvailabilityResponse` with message: `"SaaS subscription plan availability updated to: " + (active ? "ACTIVE" : "INACTIVE")`.
4. **Controller Integration**:
   - Exposed as `PATCH /api/platform-admin/plans/{id}/availability`.
   - Delegates directly to `UpdatePlanAvailabilityFactory.execute(request)`.
5. **API Documentation**:
   - Append 13-field API specification with sample payload and cURL commands to `/docs/payloads.md`.

### Out of Scope
- Modifying plan prices, limits, names, or feature flags.
- Deleting plan records (hard deletion is prohibited to maintain relational integrity with `gym_subscriptions`).
- Interacting with tenant databases.

---

## 5. Detailed Acceptance Criteria

### AC-PA5.1: Successful Deactivation (Retirement) of a Plan
- **Given** an existing active SaaS plan:
  - `id`: `3`
  - `name`: "Summer Special"
  - `price`: `49.99`
  - `maxMembers`: `50`
  - `active`: `true`
- **When** the platform admin submits `PATCH /api/platform-admin/plans/3/availability` with `{ "active": false }`,
- **Then** the service updates `active` to `false` in `saas_subscription_plans`,
- **And** returns HTTP `200 OK` with `SaasPlanResponse`:
  - `id`: `3`
  - `active`: `false`
  - `price`: `49.99` (UNMODIFIED)
  - `maxMembers`: `50` (UNMODIFIED)
  - `message`: "SaaS subscription plan availability updated to: INACTIVE"
- **And** subsequent calls to tenant-facing `findByActiveTrue()` exclude this plan.

### AC-PA5.2: Successful Reactivation of a Plan
- **Given** an inactive plan with `id = 3` (`active = false`),
- **When** the platform admin submits `PATCH /api/platform-admin/plans/3/availability` with `{ "active": true }`,
- **Then** the service updates `active` to `true`,
- **And** returns HTTP `200 OK` with `active: true` and message: `"SaaS subscription plan availability updated to: ACTIVE"`.

### AC-PA5.3: Immutability Guarantee Against Attribute Pollution
- **Given** an existing plan with `price = 99.00` and `maxMembers = 500`,
- **When** an availability update is executed,
- **Then** the database inspection confirms that `price`, `max_members`, `billing_cycle`, and feature flags remain 100% identical to their pre-update values.

### AC-PA5.4: Non-Existent Plan Error (404 Not Found)
- **Given** no plan exists with `id = 8888`,
- **When** an availability update is requested for ID `8888`,
- **Then** `UpdatePlanAvailabilityService` throws `ResourceNotFoundException("SaasSubscriptionPlan not found with id: 8888")`,
- **And** returns HTTP `404 Not Found`.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java`:
   - Add `PATCH /api/platform-admin/plans/{id}/availability` endpoint delegating to `UpdatePlanAvailabilityFactory`.
2. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java`:
   - Add `void validateUpdatePlanAvailability(UpdatePlanAvailabilityRequest request);`
3. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java`:
   - Implement validation logic.
4. `docs/payloads.md`:
   - Append 13-field API documentation and cURL commands for plan availability management.

### Files to Create:
1. `src/main/java/com/company/GymVerse/platformAdmin/factory/UpdatePlanAvailabilityFactory.java`
2. `src/main/java/com/company/GymVerse/platformAdmin/request/UpdatePlanAvailabilityRequest.java`
3. `src/main/java/com/company/GymVerse/platformAdmin/response/UpdatePlanAvailabilityResponse.java`
4. `src/main/java/com/company/GymVerse/platformAdmin/service/UpdatePlanAvailabilityService.java`
5. `src/test/java/com/company/GymVerse/platformAdmin/UpdatePlanAvailabilityIntegrationTest.java`

---

## 7. Error Handling & Security Considerations

1. **Security Context**:
   - In production, protected by Platform Admin role check (`ROLE_PLATFORM_ADMIN`).
2. **Defensive Validation**:
   - If payload `active` is null, throw `BadRequestException("Plan availability status must be specified as true or false")`.

---

## 8. Test Requirements

1. **`UpdatePlanAvailabilityIntegrationTest`**:
   - Seed an active plan in MySQL central database.
   - **Test 1**: Verify deactivation sets `active = false` without changing price or limits.
   - **Test 2**: Verify reactivation sets `active = true`.
   - **Test 3**: Verify non-existent plan ID returns HTTP 404 Not Found.
   - **Test 4**: Verify null `active` returns HTTP 400 Bad Request.

---

## 9. Definition of Done

- Code compiles cleanly (`./mvnw compile`).
- Plan availability is toggled cleanly with guaranteed price/attribute immutability.
- Integration tests pass against live MySQL central database.
- `/docs/payloads.md` updated with 13 mandatory fields.
- Task log maintained in `antigravity/`.
