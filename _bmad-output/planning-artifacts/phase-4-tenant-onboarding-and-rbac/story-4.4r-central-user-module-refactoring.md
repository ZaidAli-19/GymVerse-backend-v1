---
id: "story-4.4r"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Central User Module Gateway Architecture & Onboarding Refactoring"
status: "ready-for-implementation"
type: "architectural-refactoring"
created_at: "2026-11-19"
dependencies:
  - "story-4.4"
files_to_modify:
  - "src/main/java/com/company/GymVerse/config/SecurityConfig.java"
  - "docs/payloads.md"
  - "antigravity/project-context.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/user/controller/UserController.java"
  - "src/main/java/com/company/GymVerse/user/factory/UserFactory.java"
  - "src/main/java/com/company/GymVerse/user/factory/UserOperation.java"
  - "src/main/java/com/company/GymVerse/user/request/UserRequest.java"
  - "src/main/java/com/company/GymVerse/user/request/enums/UserRequestType.java"
  - "src/main/java/com/company/GymVerse/user/response/UserResponse.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidations.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapper.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapperImpl.java"
  - "src/main/java/com/company/GymVerse/user/service/RegisterGymService.java"
  - "src/test/java/com/company/GymVerse/user/RegisterGymIntegrationTest.java"
  - "src/test/java/com/company/GymVerse/user/validations/UserValidationsTest.java"
files_to_remove:
  - "src/main/java/com/company/GymVerse/tenantAdmin/controller/TenantAdminController.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminFactory.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminOperation.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/RegisterGymService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/RegisterGymIntegrationTest.java"
---

# Story 4.4R: Central User Module Gateway Architecture & Onboarding Refactoring

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Master-Scoped Gateway Invariant**:
>    - All master-level operations (`REGISTER_GYM`, `FETCH_MY_TENANTS`) belong strictly to the **common `user` module** (`@PostMapping("/api/user")`).
>    - They strictly require a Bearer token with `tokenType: "MASTER_ACCOUNT"` and execute against the **Central Database** (`GymVerse`).
>    - The in-gym module (`@PostMapping("/api/tenant-admin")`) is strictly reserved for **tenant-scoped operations** requiring `tokenType: "TENANT_ACCOUNT"` and authority `ROLE_TENANT_ADMIN`.
> 4. Unified single-endpoint controller gateway: `@PostMapping("/api/user")`.
> 5. Factory pattern dispatching to `UserOperation` beans via `UserRequestType`.
> 6. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 7. Maintain task logging in `antigravity/story-4.4r-central-user-module-refactoring.md`.
> 8. Keep all 8 protected multitenancy classes 100% byte-for-byte untouched.

---

## 1. User Story

**As a** system architect and software engineer,  
**I want** to decouple central master user operations (`REGISTER_GYM`, `FETCH_MY_TENANTS`, and future `ENROLL_INTO_GYM`) from in-gym tenant administration by establishing a dedicated **Central User Module** (`com.company.GymVerse.user`) with a unified gateway `@PostMapping("/api/user")`,  
**So that** all master identity operations operate under a single role-neutral gateway using `MASTER_ACCOUNT` tokens against the Central Database, while reserving `/api/tenant-admin` strictly for in-gym operations with `TENANT_ACCOUNT` tokens.

---

## 2. Technical Context & Architectural Rationale

### 2.1 The Architectural Need
In Story 4.4, `REGISTER_GYM` was initially placed under `@PostMapping("/api/tenant-admin")`. However:
1. `REGISTER_GYM` is called by a **Master User** before any gym exists, using a `MASTER_ACCOUNT` token and writing to the central database.
2. Similarly, `FETCH_MY_TENANTS` (Story 4.5) and `ENROLL_INTO_GYM` (Story 4.7) are also called by users before entering any gym.
3. Placing both central master operations and tenant-scoped operational logic (such as staff management or gym subscription insights) under the same `/api/tenant-admin` URL creates token scope confusion and prevents Spring Security from enforcing `ROLE_TENANT_ADMIN` at the HTTP ingress layer.

### 2.2 Target Module Structure: `com.company.GymVerse.user`
```
src/main/java/com/company/GymVerse/user/
├── controller/
│   └── UserController.java                # Unified gateway: @PostMapping("/api/user")
├── factory/
│   ├── UserFactory.java                   # Dispatches operations via UserRequestType
│   └── UserOperation.java                 # Contract interface: execute(UserRequest) -> UserResponse
├── request/
│   ├── UserRequest.java                   # Unified request envelope with clientPlatform field
│   ├── RegisterGymRequest.java            # Gym registration payload (retained)
│   └── enums/
│       └── UserRequestType.java           # REGISTER_GYM, FETCH_MY_TENANTS, ENROLL_INTO_GYM
├── response/
│   ├── UserResponse.java                  # Unified response envelope
│   └── RegisterGymResponse.java           # Provisioning confirmation + tenant tokens
├── mapper/
│   ├── UserMapper.java                    # Handwritten mapper interface
│   └── UserMapperImpl.java                # DTO transformation implementation
├── validations/
│   ├── UserValidations.java               # Validation interface
│   └── UserValidationsImpl.java           # Platform checks (reject MOBILE) & gym constraints
└── service/
    └── RegisterGymService.java            # Migrated business logic implementing UserOperation
```

### 2.3 Preserved Infrastructure
The heavy provisioning logic implemented in Story 4.3 remains completely untouched:
- `TenantProvisioningService` (dynamic DB creation, Flyway migration, user seed, compensation rollback).
- `TenantConfigProperties` & `SubscriptionDateUtil` (trial duration calculation).
- `JwtUtil` & `JwtFilter` (token issuance & security context).

---

## 3. Scope of Changes

1. **New Classes**:
   - `UserController` exposing strictly `@PostMapping("/api/user")`.
   - `UserFactory` maintaining a map of `UserOperation` beans.
   - `UserOperation` interface: `UserResponse execute(UserRequest request)` and `UserRequestType getRequestType()`.
   - `UserRequest` and `UserResponse` envelopes with nested payload getters/setters.
   - `UserRequestType` enum containing `REGISTER_GYM` (and placeholder `FETCH_MY_TENANTS`).
   - `UserValidations` and `UserValidationsImpl` encapsulating platform header validation and gym registration checks.
   - `UserMapper` and `UserMapperImpl` for response mapping.
   - `RegisterGymService` implementing `UserOperation`.

2. **Security Configuration**:
   - Update `SecurityConfig.java`:
     - Allow authenticated access to `/api/user/**`.
     - Reserve `/api/tenant-admin/**` for `hasRole("TENANT_ADMIN")` in Story 4.6.

3. **Cleanup**:
   - Remove obsolete `TenantAdmin*` controller, factory, request, response, and mapper files from `tenantAdmin` that belonged to Story 4.4.
   - Re-route integration tests to verify `@PostMapping("/api/user")`.

---

## 4. Acceptance Criteria & Verification

1. **Compilation**: Clean build with `./mvnw.cmd test-compile` exiting code 0.
2. **API Verification (`/api/user`)**:
   - Calling `@PostMapping("/api/user")` with `UserRequestType.REGISTER_GYM` from `X-Client-Platform: WEB` or `DESKTOP_ELECTRON` provisions gym database, seeds local user, activates free trial, and returns tenant tokens.
   - Calling with `X-Client-Platform: MOBILE` returns `403 Forbidden` (`Gym registration is restricted to Web and Desktop platforms`).
3. **Integration Test**:
   - `RegisterGymIntegrationTest` passes against `@PostMapping("/api/user")`.
   - `UserValidationsTest` passes for platform headers and constraints.
4. **Documentation**:
   - `docs/payloads.md` updated to document `@PostMapping("/api/user")` for `REGISTER_GYM`.
   - Dedicated task log in `antigravity/story-4.4r-central-user-module-refactoring.md`.
