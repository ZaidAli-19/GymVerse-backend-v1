---
title: 'Story 4.5: User Tenants Directory & Token Exchange Gateway'
type: 'feature'
created: '2026-11-19'
status: 'done'
route: 'oneshot'
review_loop_iteration: 1
context:
  - '_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/story-4.5-user-tenants-directory-and-token-exchange-gateway.md'
  - 'docs/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Authenticated master users need visibility into all gym tenants where they hold associations (Admin, Member, Trainer, Staff) and need the ability to exchange their master identity token for a zero-lookup tenant-scoped access/refresh token pair bound to an isolated tenant database.

**Approach:** Implement `FETCH_MY_TENANTS` under the unified Central User module gateway (`POST /api/user`) querying user tenant associations and active SaaS subscription statuses, and implement `SWITCH_TENANT` under the Auth module gateway (`POST /api/auth`) to validate tenant membership, active standing, and issue scoped `TENANT_ACCOUNT` tokens.

</frozen-after-approval>

## Implementation Notes

### Delivered Components
1. **User Module Operation: `FETCH_MY_TENANTS` (`POST /api/user`)**:
   - `UserRequestType.java`: Added `FETCH_MY_TENANTS`.
   - `FetchMyTenantsRequest.java`: Filter options supporting optional `role` and `gymStatus`.
   - `UserTenantSummaryResponse.java`: Detail record with tenant ID, gym code, gym name, logoUrl, location, role, admin authority, membership status, gym status, SaaS subscription status, and joined timestamp.
   - `FetchMyTenantsResponse.java`: Payload wrapper containing `tenants` list and `totalTenants` count.
   - `FetchMyTenantsService.java`: `UserOperation` implementation resolving authenticated caller identity, verifying zero `TenantContext` presence (`MASTER_ACCOUNT` token required), querying tenant associations, and populating summaries via `UserMapper`.
   - `UserMapper.java` / `UserMapperImpl.java`: Added `toUserTenantSummaryResponse` and `toFetchMyTenantsUserResponse`.

2. **Auth Module Operation: `SWITCH_TENANT` (`POST /api/auth`)**:
   - `AuthRequestType.java`: Added `SWITCH_TENANT`.
   - `SwitchTenantRequest.java`: Supports `gymTenantId`, `gymCode`, and optional `role` disambiguation.
   - `SwitchTenantResponse.java`: Carries scoped `tenantAccessToken`, `tenantRefreshToken`, `tenantId`, `gymCode`, `gymName`, `tenantDbName`, `role`, and `adminAuthority`.
   - `SwitchTenantService.java`: `AuthOperation` token exchange airlock. Validates `TenantContext == null`, cross-checks `gymTenantId` vs `gymCode` if both supplied, verifies `gymTenant.status == GymStatus.ACTIVE`, queries roles via `findAllByUserAccountIdAndGymTenantId`, performs role disambiguation (`TENANT_ADMIN > TRAINER > STAFF > MEMBER` or explicit role), enforces `userTenantRole.status == TenantMembershipStatus.ACTIVE`, issues scoped tokens with embedded `tenantDbName`, and maps via `AuthMapper`.
   - `AuthMapper.java` / `AuthMapperImpl.java`: Added `toSwitchTenantResponse` and `toSwitchTenantAuthResponse`.

3. **Database Repository Enhancements**:
   - `UserTenantRoleRepository.java`: Added `@EntityGraph(attributePaths = {"gymTenant"})` to optimize N+1 queries on `findByUserAccountId`, and added `findAllByUserAccountIdAndGymTenantId` for disambiguation queries.

4. **Documentation**:
   - `docs/payloads.md`: Documented Section 2.11 (`SWITCH_TENANT`) and Section 3.2 (`FETCH_MY_TENANTS`).

5. **Code Review Triage & Fixes Applied**:
   - Jackson `@JsonAlias({"userRequestType", "requestType"})` on `UserRequest` and `AuthRequest`.
   - Mapping delegation through `UserMapper` and `AuthMapper` rather than inline builder creation.
   - Guarded master-scoped operations against tenant-scoped tokens via `TenantContext.getCurrentTenant() == null`.
   - Enforced strict `GymStatus.ACTIVE` and `TenantMembershipStatus.ACTIVE`.
   - Multi-role collision handling via priority resolution and optional disambiguation parameter.

## Verification Evidence
- 72 unit and integration tests passing (`BUILD SUCCESS`):
  - `SwitchTenantIntegrationTest.java`: 13/13 passing (ID exchange, code exchange, 404 unauthorized gym, 403 suspended gym, 403 inactive gym, 403 suspended role, 403 pending role, 400 conflicting target, explicit role disambiguation, multiple roles without role throws 400 Bad Request, unowned role throws 404 Not Found, 401 tenant-scoped token rejection, 401 unauthenticated).
  - `FetchMyTenantsIntegrationTest.java`: 6/6 passing (multi-tenant listing, empty listing, role filtering, alias payload parsing, tenant-scoped token rejection, unauthenticated rejection).
  - `RegisterGymIntegrationTest.java`: 4/4 passing regression test.
  - Unit tests for user and provisioning modules (49/49 passing).
- Zero modifications to `com.company.GymVerse.multitanancy` package.
