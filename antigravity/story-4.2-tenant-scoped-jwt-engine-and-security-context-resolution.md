# Story 4.2: Tenant-Scoped JWT Engine & Security Context Resolution — Task Log

## 1. Summary of Changes
- Implemented `TenantTokenClaims` value object encapsulating `userId`, `email`, `tenantId`, `gymCode`, `tenantDbName`, `role`, and `adminAuthority`.
- Extended `JwtUtil` with JJWT 0.12.x methods:
  - `generateTenantAccessToken(TenantTokenClaims)`: embeds claims `tokenType: TENANT_ACCOUNT`, `type: ACCESS`, `tenantId`, `gymCode`, `tenantDbName`, `role`, `adminAuthority`.
  - `generateTenantRefreshToken(TenantTokenClaims)`: embeds claims `tokenType: TENANT_ACCOUNT`, `type: REFRESH`.
  - Added extractors: `extractTenantId`, `extractGymCode`, `extractTenantDbName`, `extractRole`, `extractAdminAuthority`, `extractTenantClaims`.
- Enhanced `JwtFilter` to cleanly handle both `MASTER_ACCOUNT` and `TENANT_ACCOUNT`:
  - **Master Account Tokens (`MASTER_ACCOUNT`):** Authenticates with empty authorities (`Collections.emptyList()`) and zero `TenantContext`, matching `CustomUserDetailsService` baseline.
  - **Tenant Account Tokens (`TENANT_ACCOUNT`):**
    - Validates `tenantDbName`, establishes `TenantContext.setCurrentTenant(tenantDbName)`, and populates granted authorities (`ROLE_<ROLE>`, `AUTHORITY_<AUTHORITY>`).
  - Guaranteed `TenantContext.clear()` in `finally` block preventing any ThreadLocal leaks across thread pools.
  - Refresh tokens (`type: REFRESH`) strictly barred from API access with HTTP 401.
- Implemented comprehensive unit and filter tests in `TenantJwtTokenTest` (7 tests) covering access/refresh token generation, claim extraction, filter authority mapping, active tenant routing during chain execution, automatic cleanup, missing claim rejection, and master token empty authorities verification.

## 2. Artifacts Created & Modified
- **Created**:
  - `src/main/java/com/company/GymVerse/security/jwt/TenantTokenClaims.java`
  - `src/test/java/com/company/GymVerse/security/jwt/TenantJwtTokenTest.java`
- **Modified**:
  - `src/main/java/com/company/GymVerse/security/jwt/JwtUtil.java`
  - `src/main/java/com/company/GymVerse/security/jwt/JwtFilter.java`
  - `_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/story-4.2-tenant-scoped-jwt-engine-and-security-context-resolution.md`

## 3. Verification & Test Evidence
- Maven Surefire Execution for `TenantJwtTokenTest`:
  ```
  [INFO] Running com.company.GymVerse.security.jwt.TenantJwtTokenTest
  [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.757 s
  [INFO] Results:
  [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
- Maven Surefire Execution for `JwtUtilTest`:
  ```
  [INFO] Running com.company.GymVerse.security.jwt.JwtUtilTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.704 s
  [INFO] Results:
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
- Multitenancy protected classes: 100% byte-for-byte untouched.
- Git commit rule: Strictly 0 autonomous commits executed.
