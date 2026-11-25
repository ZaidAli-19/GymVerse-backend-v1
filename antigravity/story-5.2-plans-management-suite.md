# Story 5.2 Task Log: Plans Management Suite

## 1. Context & Operational Goals
- **Objective:** Implement the full Plans navigation page operations under `@PostMapping("/api/tenant-admin")` organized in page-specific subfolders (`service/plans/`, `request/plans/`, `response/plans/`, `specifications/plans/`).
- **Architectural Invariants:**
  - Single unified controller gateway `/api/tenant-admin` via `TenantAdminRequestType`.
  - Services act strictly as clean orchestrators; tier/facility resolution and single-pass tier counting live in `PlanHelper`; domain rules live in `TenantAdminValidationsImpl`; DTO conversions live in handwritten `TenantAdminMapperImpl`.
  - Dynamic filtering, pagination, and whitelisted sorting use `MembershipPlanSpecification` (`JpaSpecificationExecutor`).
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched; zero autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Request & Response DTOs (`request/plans/`, `response/plans/`)**:
   - Requests: `FetchPlansRequest`, `CreatePlanRequest`, `UpdatePlanAvailabilityRequest`.
   - Responses: `PlansSummaryResponse`, `TierCountResponse`, `PlanFilterOptionsResponse`, `PlanTierOptionResponse`, `PlanItemResponse`.
   - Updated `TenantAdminRequestType`, `TenantAdminRequest`, and `TenantAdminResponse`.
2. **Specifications, Helpers, Validations & Mapper**:
   - `MembershipPlanSpecification`: Dynamic JPA criteria joining `planTier` and `facilities` (`distinct(true)` for accurate pagination counts), supporting `tier`, `hasOffer`, `active`, `isTrial`, and `search`.
   - `PlanHelper`: `resolveOrCreatePlanTier(...)` (auto-mapping `ELITE -> GOLDEN`, `LEGENDARY -> PURPLE`, or custom tier with chosen custom `PlanTheme`), `resolveOrCreateFacilities(...)` (case-insensitive deduplication & auto-persistence to `gym_facilities`), and single-pass `buildTierCounts(...)`.
   - `TenantAdminValidationsImpl`: Added `validateFetchPlans`, `validateCreatePlan`, and `validateUpdatePlanAvailability`.
   - `TenantAdminMapper` / `TenantAdminMapperImpl`: Handwritten mappings for all Plans payloads.
3. **Services (`service/plans/`)**:
   - `FetchPlansSummaryService` (`FETCH_PLANS_SUMMARY`)
   - `FetchPlanFilterOptionsService` (`FETCH_PLAN_FILTER_OPTIONS`)
   - `FetchPlansService` (`FETCH_PLANS`)
   - `CreatePlanService` (`CREATE_PLAN`, HTTP 201)
   - `UpdatePlanAvailabilityService` (`UPDATE_PLAN_AVAILABILITY`, bean name `tenantUpdatePlanAvailabilityService` to avoid collision with `platformAdmin.UpdatePlanAvailabilityService`).
4. **Tests**:
   - `PlansServiceTest` (3 unit tests covering `CREATE_PLAN` default/custom tiers & facilities, `FETCH_PLANS_SUMMARY`, `FETCH_PLAN_FILTER_OPTIONS`, `FETCH_PLANS`, `UPDATE_PLAN_AVAILABILITY`).
   - `PlansSpecificationIntegrationTest` (end-to-end MySQL 9.1 integration test verifying `V2` schema, `MembershipPlanSpecification` joins, facility search, tier sort, summary, and filter options).
   - Updated `TenantAdminValidationsImplTest` (4 tests) and `FetchTenantSubscriptionServiceTest` (2 tests).

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 20/20 tests passed (0 failures, 0 errors) across `PlansServiceTest` (3), `PlansSpecificationIntegrationTest` (1), `TenantAdminValidationsImplTest` (4), `FetchTenantSubscriptionServiceTest` (2), and `TenantSubscriptionIntegrationTest` (10).
- **Protected Core Invariant:** `com.company.GymVerse.multitanancy` remains 100% untouched.
- **Git Safety:** Zero commits or pushes executed.
