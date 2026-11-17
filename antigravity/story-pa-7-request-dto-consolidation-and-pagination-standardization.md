# Task Log: Story PA-7 — Platform Admin Request DTO Consolidation and Pagination Standardization

## Objective
Consolidate and standardize the `platformAdmin` module's request DTOs by centralizing pagination (`page`, `size`) and sorting (`sortBy`, `sortDirection`) exclusively at the root of `PlatformAdminRequest`, removing duplicate fields from nested filter DTOs (`SaasPlanFilterRequest`, `GymTenantFilterRequest`), removing spurious flat filter sprawl from `PlatformAdminRequest`, and eliminating all dual-path fallback resolution logic across services and validations.

---

## Technical Decisions & Implementation Highlights

1. **Root-Level Pagination & Sorting Consolidation**:
   - Centralized `page`, `size`, `sortBy`, and `sortDirection` exclusively on `PlatformAdminRequest`.
   - Purged duplicate `page`, `size`, `sortBy`, and `sortDirection` fields from `SaasPlanFilterRequest` and `GymTenantFilterRequest`.
   - Guaranteed single-source reading: all query services and validations now read pagination strictly from root fields, eliminating ambiguity over parameter precedence.

2. **Clean Filter Criteria Encapsulation**:
   - Removed flat filter fields (`minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, `gymCode`) from the root of `PlatformAdminRequest`.
   - Plan query criteria (`active`, `billingCycle`, `name`, `minPrice`, `maxPrice`) are now encapsulated strictly inside `request.getPlanFilter()`.
   - Gym tenant query criteria (`status`, `city`, `gymName`, `gymCode`) are now encapsulated strictly inside `request.getTenantFilter()`.
   - Eliminated field collision where `name` on root was ambiguous between plan creation vs plan search filtering.

3. **Elimination of Dual-Path Fallback Chains**:
   - Streamlined `GetAllSaasPlansService`: reads pagination and sorting from root; reads search/filter criteria from `planFilter` without ternary fallback logic.
   - Streamlined `GetGymTenantsService`: reads pagination and sorting from root; reads search/filter criteria from `tenantFilter` without ternary fallback logic.
   - Refactored `PlatformAdminValidationsImpl`: introduced private reusable helper `validatePagination(page, size, sortBy, sortDirection, allowedSortFields)` used uniformly across both plan and tenant query validations.

4. **Preservation of Single Controller Gateway & Factory Architecture**:
   - Controller entry point remains strictly `@PostMapping("/api/platform-admin")`.
   - Operations dispatch cleanly via `PlatformAdminFactory` through `PlatformAdminFactoryOperation`.
   - Zero modifications to protected multitenancy classes in `com.company.GymVerse.multitanancy`.
   - Central transaction boundaries preserved (`centralTransactionManager`, `readOnly = true`, zero tenant DB routing).

5. **API Contract Documentation Updated**:
   - Updated `docs/payloads.md` Section 1.2 (`FETCH_ALL_SAAS_PLANS`) and Section 1.5 (`FETCH_ALL_GYM_TENANTS`) with standardized JSON payloads and copy-pasteable cURL commands.

---

## Files Created / Modified

- **Created**:
  - `src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminDtoConsolidationIntegrationTest.java`
  - `antigravity/story-pa-7-request-dto-consolidation-and-pagination-standardization.md`
- **Modified**:
  - `src/main/java/com/company/GymVerse/platformAdmin/request/PlatformAdminRequest.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/request/SaasPlanFilterRequest.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/request/GymTenantFilterRequest.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/GetAllSaasPlansService.java`
  - `src/main/java/com/company/GymVerse/platformAdmin/service/GetGymTenantsService.java`
  - `src/test/java/com/company/GymVerse/platformAdmin/GetAllSaasPlansIntegrationTest.java`
  - `src/test/java/com/company/GymVerse/platformAdmin/GetGymTenantsIntegrationTest.java`
  - `docs/payloads.md`
  - `antigravity/project-context.md`

---

## Verification & Status
- Created `PlatformAdminDtoConsolidationIntegrationTest` covering:
  - Root pagination on plan queries.
  - Root pagination on tenant queries.
  - Encapsulated `planFilter` criteria.
  - Encapsulated `tenantFilter` criteria.
  - Reusable pagination boundaries & sort whitelist validation.
  - Default execution with null filter objects.
- All 8 protected multitenancy classes remain untouched.
- Zero autonomous git commits or pushes performed (`RULES.md` Rule 1).
