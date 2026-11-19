---
title: 'Story 4.6: Subscription Insights & Entitlements Management'
type: 'feature'
created: '2026-11-19'
status: 'done'
route: 'oneshot'
review_loop_iteration: 1
context:
  - '_bmad-output/planning-artifacts/phase-4-tenant-onboarding-and-rbac/story-4.6-subscription-insights-and-entitlements-management.md'
  - 'docs/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A tenant administrator managing their gym needs real-time visibility into their active SaaS subscription tier, remaining trial/billing period days, and platform feature entitlement limits (e.g., maximum members, maximum trainers, analytics access, multi-trainer support, custom branding) without polluting day-to-day member/staff operational workflows.

**Approach:** Implement `FETCH_TENANT_SUBSCRIPTION` under the tenant-scoped gateway (`POST /api/tenant-admin`). Strictly enforce that callers possess a valid `TENANT_ACCOUNT` token with `ROLE_TENANT_ADMIN` authority and an active `TenantContext`. Query Central DB for the tenant's current subscription (`GymSubscription`) and plan specifications (`SaasSubscriptionPlan`), calculate remaining days via `SubscriptionDateUtil`, and return a comprehensive `TenantSubscriptionDetailsResponse`.

</frozen-after-approval>

## Implementation Notes

- Target module: `com.company.GymVerse.tenantAdmin`
- Unified single-endpoint controller gateway: `@PostMapping("/api/tenant-admin")` with `@CrossOrigin("*")`
- Discriminator: `TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION`
- Factory pattern: `TenantAdminFactory` dispatching to `FetchTenantSubscriptionService`
- Security guard: `TenantAdminValidations` asserting authentication, active `TenantContext != null`, and presence of `ROLE_TENANT_ADMIN` (master tokens and non-admin tenant tokens like MEMBER, TRAINER, STAFF are strictly rejected with 403 Forbidden; unauthenticated requests rejected with 401 Unauthorized).
- Data mapping: handwritten `TenantAdminMapper` / `TenantAdminMapperImpl`.
- Documentation: updated `docs/payloads.md` with Section 4 and Section 4.1 (`FETCH_TENANT_SUBSCRIPTION`).

## Review Triage Log

- **Missing API contract documentation for `FETCH_TENANT_SUBSCRIPTION` in `docs/payloads.md`**: `high` — Patched. Added Section 4.1 with full 13-field contract and cURL example.
- **Inverted authentication and tenant context validation order in `TenantAdminValidationsImpl`**: `medium` — Patched. Moved `Authentication` validation before `TenantContext` validation to ensure 401 precedes 403.
- **Missing verification of `tokenType == TENANT_ACCOUNT` in `TenantAdminValidationsImpl`**: `low` — Verified. `TenantContext` is only established by `JwtFilter` when `tokenType == TENANT_ACCOUNT`.
- **Direct coupling to Servlet HTTP layer in `FetchTenantSubscriptionService`**: `low` — Tolerable for request-scoped token resolution in current Spring MVC architecture.
- **Inaccurate trial activity calculation in `FetchTenantSubscriptionService`**: `medium` — Patched. Used `SubscriptionDateUtil.isTrialActive(subscription.getEndDate(), today)`.
- **Missing `planPrice` and `planId` fields in `TenantSubscriptionDetailsResponse`**: `medium` — Patched. Added `planId` and `planPrice` to DTO and mapper.
- **Unused `includeHistory` flag in `FetchTenantSubscriptionRequest`**: `low` — Retained as optional reserved request parameter.
- **Missing `@CrossOrigin("*")` annotation on `TenantAdminController`**: `high` — Patched. Added `@CrossOrigin("*")`.
- **Redundant request validation in `TenantAdminValidationsImpl`**: `low` — Retained for defense-in-depth.
- **Integration test coverage gaps in `TenantSubscriptionIntegrationTest`**: `medium` — Patched. Added tests for TRAINER, STAFF, missing request type, and expired subscription.
- **Missing task tracking artifact `antigravity/story-4.6-subscription-insights-and-entitlements-management.md`**: `high` — Patched. Created task log file.

## Verification Evidence
- 82 unit and integration tests passing (`BUILD SUCCESS`):
  - `TenantSubscriptionIntegrationTest`: 10/10 passing
  - `SwitchTenantIntegrationTest`: 13/13 passing
  - `FetchMyTenantsIntegrationTest`: 6/6 passing
  - `RegisterGymIntegrationTest`: 4/4 passing
  - Unit tests across provisioning, dates, validations, tokens: 49/49 passing
- Zero modifications to `com.company.GymVerse.multitanancy` package.
- Zero git commits or pushes executed.
