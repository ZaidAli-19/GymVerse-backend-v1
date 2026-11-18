---
id: "story-4.5"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "User Tenants Directory & Token Exchange Gateway"
status: "planned"
type: "gateway-operation"
created_at: "2026-11-18"
dependencies:
  - "story-4.2"
  - "story-4.4"
files_to_modify:
  - "src/main/java/com/company/GymVerse/auth/request/enums/AuthRequestType.java"
  - "src/main/java/com/company/GymVerse/auth/request/AuthRequest.java"
  - "src/main/java/com/company/GymVerse/auth/response/AuthResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/FetchMyTenantsRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/FetchMyTenantsResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/UserTenantSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/FetchMyTenantsService.java"
  - "src/main/java/com/company/GymVerse/auth/request/SwitchTenantRequest.java"
  - "src/main/java/com/company/GymVerse/auth/response/SwitchTenantResponse.java"
  - "src/main/java/com/company/GymVerse/auth/service/SwitchTenantService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/FetchMyTenantsIntegrationTest.java"
  - "src/test/java/com/company/GymVerse/auth/SwitchTenantIntegrationTest.java"
---

# Story 4.5: User Tenants Directory & Token Exchange Gateway

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Unified single-endpoint controller gateways: `@PostMapping("/api/tenant-admin")` and `@PostMapping("/api/auth")`.
> 4. Factory pattern dispatching to operation beans.
> 5. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 6. Maintain task logging in `antigravity/story-4.5-user-tenants-directory-and-token-exchange-gateway.md`.

---

## 1. User Story

**As an** authenticated user logging in at the master level,  
**I want** to view all gym tenants where I am associated (`ADMIN`, `MEMBER`, `TRAINER`, `STAFF`) via `FETCH_MY_TENANTS` and select a gym to obtain tenant-scoped tokens via `SWITCH_TENANT`,  
**So that** I can seamlessly navigate between multiple gyms using my single Master Account identity and enter any gym's scoped operational dashboard.

---

## 2. Technical Context & Requirements

1. **User Tenant Directory (`FETCH_MY_TENANTS`)**:
   - Dispatched via `@PostMapping("/api/tenant-admin")` with `TenantAdminRequestType.FETCH_MY_TENANTS`.
   - Caller identity extracted from `SecurityContextHolder`.
   - Queries `UserTenantRoleRepository.findByUserAccountId(userAccountId)`.
   - Joins with `GymTenant` and latest `GymSubscription` to provide:
     - `tenantId`
     - `gymCode`
     - `gymName`
     - `logoUrl`
     - `city`
     - `userRole` (`TENANT_ADMIN`, `MEMBER`, `TRAINER`, `STAFF`)
     - `adminAuthority` (`PRIMARY`, `DELEGATED`, `NONE`)
     - `roleStatus` (`ACTIVE`, `PENDING`, `SUSPENDED`)
     - `gymStatus` (`ACTIVE`, `SUSPENDED`)
     - `subscriptionStatus` (`TRIAL`, `ACTIVE`, `EXPIRED`)
   - Returns `FetchMyTenantsResponse` with a list of `UserTenantSummaryResponse`.

2. **Tenant Switching & Token Exchange (`SWITCH_TENANT`)**:
   - Dispatched via `@PostMapping("/api/auth")` with `AuthRequestType.SWITCH_TENANT`.
   - Injects `SwitchTenantRequest`: contains `gymTenantId` (or `gymCode`).
   - Requires caller to be authenticated with valid `MASTER_ACCOUNT` Bearer token.
   - Validates that caller has an active record in `user_tenant_roles` for that `gym_tenant_id`:
     - If record not found: throws `ResourceNotFoundException("Tenant association not found")`.
     - If user role is suspended: throws `ForbiddenException("Your access to this gym is suspended")`.
     - If gym tenant is suspended: throws `ForbiddenException("This gym tenant is currently suspended")`.
   - Generates and returns `TENANT_ACCOUNT` access and refresh tokens.
   - Response contains: `tenantAccessToken`, `tenantRefreshToken`, `role`, `adminAuthority`, `gymCode`, `tenantDbName`.

---

## 3. Verification & Acceptance Criteria

1. **Integration Test (`FetchMyTenantsIntegrationTest.java`)**:
   - Verifies user with multiple gym roles (e.g. Admin in Gym 1, Member in Gym 2) receives full list with accurate roles and subscription statuses.
   - Verifies user with zero gym associations receives an empty list cleanly.
2. **Integration Test (`SwitchTenantIntegrationTest.java`)**:
   - Verifies valid master token + valid `gymTenantId` returns `TENANT_ACCOUNT` tokens with correct claims.
   - Verifies attempting to switch to an unauthorized gym returns HTTP 404 or 403.
   - Verifies switching to a suspended gym returns HTTP 403 Forbidden.
3. **Context Compilation Check**:
   - Clean build via `./mvnw.cmd test-compile`.
