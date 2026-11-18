---
id: "story-4.2"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Tenant-Scoped JWT Engine & Security Context Resolution"
status: "implemented"
type: "security-foundation"
created_at: "2026-11-18"
dependencies:
  - "story-4.1"
files_to_modify:
  - "src/main/java/com/company/GymVerse/security/jwt/JwtUtil.java"
  - "src/main/java/com/company/GymVerse/security/jwt/JwtFilter.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/security/jwt/TenantTokenClaims.java"
  - "src/test/java/com/company/GymVerse/security/jwt/TenantJwtTokenTest.java"
---

# Story 4.2: Tenant-Scoped JWT Engine & Security Context Resolution

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Preserve all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.
> 4. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 5. Maintain task logging in `antigravity/story-4.2-tenant-scoped-jwt-engine-and-security-context-resolution.md`.

---

## 1. User Story

**As a** multi-tenant security engineer,  
**I want** to enhance `JwtUtil` to generate and parse `TENANT_ACCOUNT` tokens and update `JwtFilter` to dynamically establish `TenantContext` and Spring Security authorities,  
**So that** tenant-scoped requests automatically route to the correct tenant database with appropriate role-based authorization and zero ThreadLocal leaks.

---

## 2. Technical Context & Requirements

1. **Token Claims Carrier (`TenantTokenClaims.java`)**:
   - Encapsulates: `userId` (central user ID), `email`, `tenantId`, `gymCode`, `tenantDbName`, `role` (`TenantRole`), `adminAuthority` (`AdminAuthority`).

2. **Enhance `JwtUtil.java`**:
   - Add constants:
     - `TOKEN_TYPE_TENANT_ACCOUNT = "TENANT_ACCOUNT"`
   - Methods:
     - `String generateTenantAccessToken(TenantTokenClaims claims)`: JJWT 0.12.x token with subject `userId`, claims `tokenType: TENANT_ACCOUNT`, `type: ACCESS`, `tenantId`, `gymCode`, `dbName`, `role`, `adminAuthority`, `email`.
     - `String generateTenantRefreshToken(TenantTokenClaims claims)`: Refresh token with `type: REFRESH`.
     - `TenantTokenClaims extractTenantClaims(String token)`: extracts strongly-typed claims.
     - `Long extractTenantId(String token)`
     - `String extractGymCode(String token)`
     - `String extractTenantDbName(String token)`

3. **Enhance `JwtFilter.java`**:
   - Inspect token claim `tokenType`:
     - If `MASTER_ACCOUNT`:
       - Operates as before: sets `SecurityContextHolder` with central authentication details and `ROLE_USER_ACCOUNT` / `ROLE_PLATFORM_ADMIN`.
     - If `TENANT_ACCOUNT`:
       - Extracts `tenantDbName`, `role`, and `adminAuthority`.
       - Sets `TenantContext.setCurrentTenant(tenantDbName)`.
       - Populates `SecurityContextHolder` with `ROLE_<ROLE>` (e.g. `ROLE_TENANT_ADMIN`, `ROLE_MEMBER`) and `AUTHORITY_<AUTHORITY>` (e.g. `AUTHORITY_PRIMARY`).
   - In `finally` block:
     - Preserves `TenantContext.clear()` to prevent any ThreadLocal leak across thread pools.

---

## 3. Verification & Acceptance Criteria

1. **Unit & Filter Tests (`TenantJwtTokenTest.java`)**:
   - Verifies tenant token generation with valid claims and JJWT signature verification.
   - Verifies `extractTenantClaims` retrieves exact `tenantId`, `gymCode`, `dbName`, `role`, and `adminAuthority`.
   - Verifies `JwtFilter` binds `TenantContext` when presented with a valid `TENANT_ACCOUNT` token and clears it after request completion.
   - Verifies refresh tokens (`type: REFRESH`) are blocked from invoking APIs.
2. **Context Compilation Check**:
   - Clean build via `./mvnw.cmd test-compile`.
