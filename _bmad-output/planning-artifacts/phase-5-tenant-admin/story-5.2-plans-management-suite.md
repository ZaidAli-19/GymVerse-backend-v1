---
id: "story-5.2"
epic_id: "epic-phase-5-tenant-admin"
title: "Plans Management Suite (Summary, Filter Options, Catalog, Custom Tiers, Facilities & Themes)"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/specifications/plans/MembershipPlanSpecification.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/plans/FetchPlansRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/plans/CreatePlanRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/plans/UpdatePlanAvailabilityRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/plans/PlansSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/plans/PlanFilterOptionsResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/plans/PlanItemResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/plans/TierCountResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/plans/PlanTierOptionResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/plans/FetchPlansSummaryService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/plans/FetchPlanFilterOptionsService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/plans/FetchPlansService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/plans/CreatePlanService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/plans/UpdatePlanAvailabilityService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/plans/PlansServiceTest.java"
---

# Story 5.2: Plans Management Suite (Summary, Filter Options, Catalog, Custom Tiers, Facilities & Themes)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Single Controller Gateway**: All 5 operations route via `@PostMapping("/api/tenant-admin")` using `TenantAdminRequestType`.
> 4. **Subfolder Classification**: Services live in `service/plans/`, Requests in `request/plans/`, Responses in `response/plans/`, Specifications in `specifications/plans/`.
> 5. **Pagination & JPA Specifications**: `FETCH_PLANS` MUST use `MembershipPlanSpecification` + `Pageable` + `PagedResponse<PlanItemResponse>`.

---

## 1. User Story

**As a** Tenant Administrator managing my gym's membership packages on the Plans page,  
**I want** a plans mini-dashboard, dynamic tier/facility/theme filter options, paginated/filtered plan catalog, and an Add Plan workflow supporting default tiers (`ELITE`, `LEGENDARY`), custom tiers with UI-matched themes, and custom facilities,  
**So that** I can organize, discount, and style membership plans cleanly for my gym.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_PLANS_SUMMARY`
- Enforces `validateTenantAdminContext()` and `tenantTransactionManager` (`readOnly = true`).
- Returns `PlansSummaryResponse`:
  - `totalActivePlans` (`long`)
  - `tierBreakdown`: list of `TierCountResponse { tierName, themeKey, activePlanCount, totalPlanCount }` combining persisted `plan_tiers` with active plan counts.

### 2.2 `FETCH_PLAN_FILTER_OPTIONS`
- Returns `PlanFilterOptionsResponse` for populating both the Plans filter bar and the Add Plan modal:
  - `tiers`: list of `PlanTierOptionResponse { id, name, themeKey, isDefault }` (includes `ELITE` -> `GOLDEN`, `LEGENDARY` -> `PURPLE`, plus any admin-created custom tiers).
  - `facilities`: sorted list of persisted `gym_facilities` names.
  - `customThemes`: list of the 20 canonical custom `PlanTheme` string keys (`EMERALD_GREEN`, `CRIMSON_RED`, ..., `ROYAL_COBALT`) so UI and backend stay 100% synchronized.

### 2.3 `FETCH_PLANS`
- Request: `FetchPlansRequest { tier (optional, null/"ALL" = all), hasOffer (optional Boolean), active (optional Boolean), isTrial (optional Boolean), search (optional String), page, size, sortBy, sortDirection }`.
- Validation: `TenantAdminValidations.validateFetchPlans(...)` validates pagination bounds and allowed `sortBy` fields (`id`, `name`, `tier`, `basePrice`, `finalPrice`, `durationDays`, `active`).
- Specification (`MembershipPlanSpecification.withFilters(...)`):
  - `tier` (ignore `"ALL"`, case-insensitive match)
  - `hasOffer` (`true` -> `discountType IS NOT NULL AND discountValue > 0`; `false` -> `discountType IS NULL`)
  - `active`, `isTrial`
  - `search` (case-insensitive `LIKE` across `name`, `description`, `facility`)
- Response: `PagedResponse<PlanItemResponse>` with `hasOffer` boolean flag, `themeKey`, `facilities`, pricing, discount, duration, `isTrial`, `active`.

### 2.4 `CREATE_PLAN`
- Request (`CreatePlanRequest`):
  - `name` (required, <= 100 chars)
  - `description` (optional)
  - `tier` (required: `"ELITE"`, `"LEGENDARY"`, or a custom tier name)
  - `customTheme` (required if `tier` is a brand-new custom tier; validated against the 20 custom `PlanTheme` values! Ignored/auto-resolved to `GOLDEN`/`PURPLE` for `ELITE`/`LEGENDARY`)
  - `facilities` (`List<String>`, required non-empty — e.g. `["Weightlifting", "Cardio"]`; any new facility name not yet in `gym_facilities` is normalized and persisted to `gym_facilities`)
  - `basePrice` (`BigDecimal > 0`)
  - `discountType` (optional: `"PERCENTAGE"` or `"FLAT"`)
  - `discountValue` (optional `BigDecimal > 0` when `discountType` set; percentage `<= 100`, flat `<= basePrice`)
  - `durationDays` (`Integer > 0`)
  - `isTrial` (`Boolean`, default `false`)
- Orchestration:
  - Validation layer verifies all fields, tier/theme rules, and duplicate plan name check.
  - Helper resolves or creates `PlanTier` (fixing `ELITE -> GOLDEN`, `LEGENDARY -> PURPLE`, or persisting new custom tier with chosen custom theme), persists any new `GymFacility` entries, and computes `finalPrice` via `MembershipCalculationHelper`.
  - Saves `MembershipPlan` and returns HTTP `201 Created` with `PlanItemResponse`.

### 2.5 `UPDATE_PLAN_AVAILABILITY`
- Request (`UpdatePlanAvailabilityRequest { planId, active }`): toggles `MembershipPlan.active` and returns updated `PlanItemResponse`.

---

## 3. Acceptance Criteria

- [ ] All 5 operations registered in `TenantAdminRequestType` and dispatched cleanly via `TenantAdminFactory`.
- [ ] `ELITE` resolves to `GOLDEN`, `LEGENDARY` resolves to `PURPLE`, and custom tiers persist with one of the 20 custom `PlanTheme` keys.
- [ ] New facilities entered during `CREATE_PLAN` are saved in `gym_facilities` and returned by `FETCH_PLAN_FILTER_OPTIONS`.
- [ ] `FETCH_PLANS` enforces `MembershipPlanSpecification`, pagination, and whitelisted sorting; `hasOffer` filters discounted plans accurately.
- [ ] Unit tests in `PlansServiceTest` verify summary counts, filter options, JPA Specification filtering, tier/theme validation, discount calculation, and availability toggle.
