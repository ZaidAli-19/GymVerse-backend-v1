# Task Log: Story 4.5 — User Tenants Directory & Token Exchange Gateway

## 1. Overview & Objectives
Implemented Story 4.5 establishing:
1. **User Tenants Directory (`FETCH_MY_TENANTS`)**: Master-scoped query under central User gateway (`POST /api/user`) enabling authenticated master users to query all gym tenants where they hold associations, complete with tenant metadata, functional roles, administrative authority, and current SaaS subscription status.
2. **Token Exchange Gateway (`SWITCH_TENANT`)**: Authentication bridge under Auth gateway (`POST /api/auth`) trading a valid `MASTER_ACCOUNT` token for a scoped `TENANT_ACCOUNT` token pair (access & refresh) with embedded `tenantDbName` claim for zero-lookup tenant request routing.

---

## 2. Delivered Code & Components

### 2.1 Central User Module: `FETCH_MY_TENANTS` (`POST /api/user`)
- **`UserRequestType.java`**: Added enum discriminator `FETCH_MY_TENANTS`.
- **`FetchMyTenantsRequest.java`**: Filter DTO supporting optional filtering by `role` (`TenantRole`) and `gymStatus` (`GymStatus`).
- **`UserTenantSummaryResponse.java`**: DTO containing `tenantId`, `gymCode`, `gymName`, `logoUrl`, `city`, `address`, `userRole`, `adminAuthority`, `roleStatus`, `gymStatus`, `subscriptionStatus`, and `joinedAt`.
- **`FetchMyTenantsResponse.java`**: Payload response encapsulating `tenants` and `totalTenants`.
- **`FetchMyTenantsService.java`**: Implements `UserOperation`. Enforces `TenantContext == null`, validates caller identity, queries roles via optimized `userTenantRoleRepository.findByUserAccountId(...)`, resolves latest subscription status, and maps via `UserMapper`.
- **`UserMapper.java` / `UserMapperImpl.java`**: Added `toUserTenantSummaryResponse` and `toFetchMyTenantsUserResponse`.

### 2.2 Auth Module: `SWITCH_TENANT` (`POST /api/auth`)
- **`AuthRequestType.java`**: Added enum discriminator `SWITCH_TENANT`.
- **`SwitchTenantRequest.java`**: Request DTO supporting `gymTenantId`, `gymCode`, and optional `role` for disambiguation.
- **`SwitchTenantResponse.java`**: Response DTO carrying scoped token pair (`tenantAccessToken`, `tenantRefreshToken`) and target tenant metadata.
- **`SwitchTenantService.java`**: Implements `AuthOperation`. Validates caller authentication, verifies `TenantContext == null`, cross-checks `gymTenantId` vs `gymCode` if both supplied, ensures `GymStatus.ACTIVE`, loads associations via `findAllByUserAccountIdAndGymTenantId`, performs role disambiguation (`TENANT_ADMIN > TRAINER > STAFF > MEMBER` or matching requested `role`), ensures `TenantMembershipStatus.ACTIVE`, generates scoped JWTs with `TenantTokenClaims`, and maps via `AuthMapper`.
- **`AuthMapper.java` / `AuthMapperImpl.java`**: Added `toSwitchTenantResponse` and `toSwitchTenantAuthResponse`.

### 2.3 Repository Optimization
- **`UserTenantRoleRepository.java`**:
  - Added `@EntityGraph(attributePaths = {"gymTenant"})` to `findByUserAccountId` to prevent N+1 query overhead.
  - Added `findAllByUserAccountIdAndGymTenantId(Long userAccountId, Long gymTenantId)` for multi-role disambiguation queries.

### 2.4 API Payloads Documentation
- **`docs/payloads.md`**:
  - Added **Section 2.11**: `SWITCH_TENANT` under `POST /api/auth` with full contract and cURL snippet.
  - Added **Section 3.2**: `FETCH_MY_TENANTS` under `POST /api/user` with full contract and cURL snippet.

---

## 3. Verification & Test Evidence

### Test Suite Execution
Executed unit and integration tests across Auth and User modules:
```
[INFO] Running com.company.GymVerse.auth.SwitchTenantIntegrationTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.FetchMyTenantsIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.RegisterGymIntegrationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.service.TenantProvisioningServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.validations.TenantProvisioningValidationsTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.validations.UserValidationsTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.company.GymVerse.user.util.SubscriptionDateUtilTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Key Invariants Verified
1. **Multi-Tenancy Guard**: Protected classes in `com.company.GymVerse.multitanancy` remain untouched.
2. **Master Scope Enforcement**: Central endpoints (`POST /api/user` and `POST /api/auth` switch tenant) strictly reject requests containing pre-existing `TenantContext` (i.e. tenant-scoped tokens).
3. **Status Guarding**: Suspended and inactive gyms, as well as non-active user memberships, are rejected with 403 Forbidden.
4. **Single-Endpoint Architecture**: All central user operations route through `POST /api/user` and auth through `POST /api/auth`.
