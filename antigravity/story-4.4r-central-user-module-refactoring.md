# Story 4.4R: Central User Module Refactoring & Master-Scoped Gym Registration Gateway

## 1. Executive Summary
- **Story ID:** `STORY-4.4R` (Refactoring & Architectural Realignment of Story 4.4)
- **Module:** Central Master User (`com.company.GymVerse.user`)
- **Gateway Endpoint:** `POST /api/user` (Unified Single-Endpoint Gateway per Module Standard)
- **Scope Tier:** Tier 1 Central / Master-Scoped (Executed by authenticated `MASTER_ACCOUNT` user before any tenant context is bound)
- **Status:** ✅ Completed & Verified

---

## 2. Problem Statement & Architectural Motivation
Prior to this refactoring, Gym Registration (`REGISTER_GYM`) was provisionally routed through `/api/tenant-admin`. This introduced conceptual ambiguity and violated domain boundaries:
1. **Tier Inversion:** A user registering a new gym does not yet belong to that gym and has no active tenant context. Placing registration under `tenantAdmin` conflated central account capabilities with in-gym operational workflows.
2. **Client Platform Policing:** Enforcing `X-Client-Platform` headers on the backend added unnecessary friction. The backend should provide a uniform, role-neutral API contract, while client presentation (e.g., hiding or showing onboarding wizards) is strictly delegated to client UI layers.
3. **Module Responsibility Clarity:** Clear separation between Tier 1 (Central/Master-Scoped operations: `user`, `auth`, `platformAdmin`) and Tier 2 (Tenant-Scoped In-Gym operations: `tenantAdmin`, `member`, `trainer`, `staff`).

---

## 3. Implemented Components

### 3.1 New Central User Gateway & Provisioning Engine (`com.company.GymVerse.user`)
- **Controller:**
  - `UserController` (`@RestController`, `@RequestMapping("/api/user")`, `@PostMapping`) - single entry-point routing via `UserFactory`.
- **Factory & Operations:**
  - `UserFactory` - Spring bean registry executing `UserOperation` beans keyed by `UserRequestType`.
  - `UserOperation` interface: `UserResponse execute(UserRequest request, Authentication authentication)`.
  - `RegisterGymService` - coordinates validation (`UserValidations`), extracts authenticated `UserAccount`, delegates to `TenantProvisioningService`, and returns zero-lookup tenant token pair.
  - `TenantProvisioningService` - moved to `com.company.GymVerse.user.service`, orchestrating physical database creation, schema migrations, tenant seed, and central persistence.
- **DTOs & Discriminators:**
  - `UserRequestType` enum: `REGISTER_GYM` (and extensible for `FETCH_MY_TENANTS` in Story 4.5).
  - `UserRequest` & `RegisterGymRequest`: Clean payloads with zero `clientPlatform` fields.
  - `UserResponse` & `RegisterGymResponse`: Uniform response envelope returning tenant tokens, database identifier, trial period, and owner role details.
  - `TenantProvisioningCommand` & `TenantProvisioningResult`: Relocated under `com.company.GymVerse.user.dto`.
- **Config & Utilities:**
  - `TenantConfigProperties`: Relocated under `com.company.GymVerse.user.config`.
  - `SubscriptionDateUtil`: Relocated under `com.company.GymVerse.user.util`.
- **Validations & Mappers:**
  - `UserValidations` / `UserValidationsImpl`: Validates payload existence, required gym metadata, coordinate bounds (-90..90, -180..180), and custom gym code formatting (`^[A-Za-z0-9-]{4,10}$`) and uniqueness.
  - `TenantProvisioningValidations` / `TenantProvisioningValidationsImpl`: Validates `TenantProvisioningCommand` domain invariants and uniqueness.
  - `UserMapper` / `UserMapperImpl`: Seamless domain-to-DTO mappings between central/tenant models and API response structures.

### 3.2 Complete Cleanup & Re-Scoping of `tenantAdmin`
- Removed all provisional registration and provisioning classes from `tenantAdmin` so the `tenantAdmin` package is completely clear.
- Reserved `/api/tenant-admin` exclusively for Tier 2 In-Gym operational workflows (Story 4.6 subscription insights & management).

### 3.3 Security & Documentation Updates
- **Security Baseline:** Configured `.requestMatchers("/api/user/**").authenticated()` in `SecurityConfig.java`.
- **Architecture Reference:** Updated `docs/architecture.md` Section 2.2 with comprehensive Two-Tier Architectural Paradigm (Central/Master vs Tenant-Scoped Operational).
- **API Payloads Reference:** Updated `docs/payloads.md` Section 3 with full `/api/user` contract and cURL examples (no client platform headers).
- **Planning Artifacts:** Updated `story-4.4r`, `story-4.5`, `story-4.6`, `phase-4-epic.md`, `dependency-matrix-and-ordering.md`.

---

## 4. Verification & Testing

### 4.1 Unit Tests (48/48 Passing)
- `UserValidationsTest`: 5/5 passing (null checks, missing fields, coordinate boundaries, custom code format and uniqueness).
- `TenantAdminValidationsTest`: 10/10 passing (null guards, format, collision handling).
- `TenantProvisioningServiceTest`: 7/7 passing (orchestration, rollback compensation, custom code, Flyway failure rollback).
- `TenantConfigPropertiesTest`: 5/5 passing.
- `SubscriptionDateUtilTest`: 9/9 passing.
- `TenantJwtTokenTest`: 7/7 passing.
- `JwtUtilTest`: 5/5 passing.

### 4.2 Integration Tests (4/4 Passing)
- `RegisterGymIntegrationTest` (`com.company.GymVerse.user`):
  - `registerGym_Success`: Registers tenant, provisions DB, seeds admin, generates tenant token pair with `tenantDbName`.
  - `registerGym_Unauthenticated_ThrowsUnauthorized`: Verifies 401 on unauthenticated access.
  - `registerGym_MissingRequiredFields_ThrowsBadRequest`: Verifies 400 validation guards.
  - `registerGym_DuplicateCustomGymCode_ThrowsBadRequest`: Verifies 400 conflict guards.

---

## 5. Architectural Invariants Preserved
- `RULES.md` Rule 1: No autonomous `git commit` or `git push` executed.
- All 8 multitenancy core classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
- Single unified POST gateway per module maintained (`POST /api/user`).
