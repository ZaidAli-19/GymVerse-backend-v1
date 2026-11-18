---
id: "story-4.6"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Subscription Insights & Entitlements Management"
status: "planned"
type: "gateway-operation-and-documentation"
created_at: "2026-11-18"
dependencies:
  - "story-4.1"
  - "story-4.4"
  - "story-4.5"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "docs/payloads.md"
  - "antigravity/project-context.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/FetchTenantSubscriptionRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantSubscriptionDetailsResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/FetchTenantSubscriptionService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/TenantSubscriptionIntegrationTest.java"
---

# Story 4.6: Subscription Insights & Entitlements Management

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Unified single-endpoint controller gateway: `@PostMapping("/api/tenant-admin")`.
> 4. Update [`docs/payloads.md`](file:///D:/projects/GymVerse-Backend-v2/docs/payloads.md) with all Phase 4 endpoints, request/response contracts, and cURL snippets.
> 5. Update [`antigravity/project-context.md`](file:///D:/projects/GymVerse-Backend-v2/antigravity/project-context.md).
> 6. Maintain task logging in `antigravity/story-4.6-subscription-insights-and-entitlements-management.md`.

---

## 1. User Story

**As a** gym owner viewing the Subscription page of my tenant dashboard or my master dashboard,  
**I want** to query my gym's active SaaS subscription details, trial countdown, and feature entitlement limits via `FETCH_TENANT_SUBSCRIPTION`,  
**So that** I have full transparency into available platform features and trial status without cluttering my daily operational views.

---

## 2. Technical Context & Requirements

1. **Subscription Insights Operation (`FETCH_TENANT_SUBSCRIPTION`)**:
   - Dispatched via `@PostMapping("/api/tenant-admin")` with `TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION`.
   - Accessible using either:
     - Master Bearer Token (`MASTER_ACCOUNT`): passes `tenantId` in `FetchTenantSubscriptionRequest`. Validates that caller is `ADMIN` of that tenant.
     - Tenant-Scoped Bearer Token (`TENANT_ACCOUNT`): extracts `tenantId` automatically from the token claim. Requires `ROLE_TENANT_ADMIN`.
   - Queries `GymSubscriptionRepository` for the latest active/trial subscription.
   - Queries `SaasSubscriptionPlanRepository` for plan specifications and feature flags.
   - Calculates trial/subscription countdown via `SubscriptionDateUtil`.
   - Returns `TenantSubscriptionDetailsResponse` containing:
     - `subscriptionId`
     - `tenantId`
     - `gymName`
     - `planName`
     - `planPrice`
     - `billingCycle`
     - `status` (`TRIAL`, `ACTIVE`, `EXPIRED`, `CANCELLED`)
     - `startDate`
     - `endDate`
     - `daysRemaining`
     - `isTrial`
     - Feature Entitlements:
       - `maxMembers`
       - `maxTrainers`
       - `analyticsEnabled`
       - `multiTrainerEnabled`
       - `customBrandingEnabled`

2. **API Payload Documentation Update (`docs/payloads.md`)**:
   - Document all Phase 4 endpoints with full 13-field contract specifications and valid cURL snippets:
     - `REGISTER_GYM` (`/api/tenant-admin`)
     - `FETCH_MY_TENANTS` (`/api/tenant-admin`)
     - `FETCH_TENANT_SUBSCRIPTION` (`/api/tenant-admin`)
     - `SWITCH_TENANT` (`/api/auth`)

---

## 3. Verification & Acceptance Criteria

1. **Integration Test (`TenantSubscriptionIntegrationTest.java`)**:
   - Verifies querying subscription via Master Token for owned gym returns accurate subscription and plan entitlements.
   - Verifies querying subscription via Tenant Token (`TENANT_ACCOUNT`) returns accurate data.
   - Verifies non-admin users attempting to query subscription receive HTTP 403 Forbidden.
2. **Documentation & Context Invariant**:
   - `docs/payloads.md` updated with all 4 Phase 4 operations.
   - `antigravity/project-context.md` updated with Phase 4 completion status.
3. **Context Compilation Check**:
   - Full test suite execution passes cleanly with zero failures.
