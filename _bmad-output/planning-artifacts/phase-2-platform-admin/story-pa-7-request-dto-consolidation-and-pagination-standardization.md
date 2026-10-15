---
id: "story-pa-7"
epic_id: "epic-pa"
title: "Platform Admin Request DTO Consolidation and Pagination Standardization"
status: "planning"
type: "platform-admin-refactoring-and-standardization"
created_at: "2026-10-15"
dependencies:
  - "story-pa-2"
  - "story-pa-3"
  - "story-pa-4"
  - "story-pa-5"
  - "story-pa-6"
files_to_modify:
  - "src/main/java/com/company/GymVerse/platformAdmin/request/PlatformAdminRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/request/SaasPlanFilterRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/request/GymTenantFilterRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/GetAllSaasPlansService.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/GetGymTenantsService.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/UpdatePlanAvailabilityService.java"
  - "docs/payloads.md"
  - "src/test/java/com/company/GymVerse/platformAdmin/GetAllSaasPlansIntegrationTest.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/GetGymTenantsIntegrationTest.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/UpdatePlanAvailabilityIntegrationTest.java"
files_to_create:
  - "src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminDtoConsolidationIntegrationTest.java"
---

# Story PA-7: Platform Admin Request DTO Consolidation and Pagination Standardization

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. **NO ARCHITECTURE REPLACEMENT:** You must strictly preserve the existing module-level factory architecture (`PlatformAdminFactory`, `PlatformAdminFactoryOperation`, single controller gateway `@PostMapping("/api/platform-admin")`). Do NOT introduce operation-specific factories, generic `<T, R>` handler registries, or separate REST controllers.  
> 3. **REFERENCE MODULE FIDELITY:** Follow the exact request DTO and pagination conventions established in ConnectSouq's `franchiseOperator` reference module (`D:\projects\ConnectSouq\src\main\java\com\bxb\ConnectSouq\franchiseOperator`).  
> 4. **ELIMINATE DUPLICATION & AMBIGUITY:** Centralize pagination (`page`, `size`) and sorting (`sortBy`, `sortDirection`) exclusively on the outer shared request `PlatformAdminRequest`. Remove these duplicate fields from nested filter DTOs (`SaasPlanFilterRequest`, `GymTenantFilterRequest`).  
> 5. **ELIMINATE DUAL-PATH FALLBACKS:** Eliminate conflicting flat and nested fields so that each operation reads its criteria from exactly one unambiguous location.  
> 6. **ZERO REGRESSIONS:** All 61 existing automated integration tests must pass cleanly without behavioral regressions.  
> 7. **ZERO TENANT ROUTING:** This service runs exclusively on `centralTransactionManager` (read-only where applicable). It must never query individual tenant databases or touch `TenantContext`.  
> 8. **PROTECTED MULTITENANCY CLASSES:** Preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 9. You must update `/docs/payloads.md` for all affected operations with accurate payloads, validation rules, and cURL commands.  
> 10. You must log all changes, technical rationale, and verification commands in `antigravity/story-pa-7-request-dto-consolidation-and-pagination-standardization.md`.

---

## 1. User Story

**As a** GymVerse Platform Developer and Platform Administrator,  
**I want to** consolidate the `platformAdmin` module's request DTOs by centralizing common pagination and sorting parameters at the root of `PlatformAdminRequest` and organizing operation-specific criteria into clean, dedicated nested DTOs,  
**So that** the API contract eliminates field duplication, removes parameter precedence ambiguities, aligns strictly with ConnectSouq's `franchiseOperator` reference conventions, and provides deterministic, single-source validation and querying across all administrative operations.

---

## 2. Business Context & Problem Statement

Across Stories PA-2 through PA-6, the Platform Admin module implemented five distinct operations through the single unified gateway endpoint `POST /api/platform-admin`:
- `CREATE_SAAS_PLAN`
- `FETCH_ALL_SAAS_PLANS`
- `FETCH_SAAS_PLAN_BY_ID`
- `UPDATE_PLAN_AVAILABILITY`
- `FETCH_ALL_GYM_TENANTS`

### The Problem:
1. **Triplicate Pagination and Sorting Fields**:
   - `page`, `size`, `sortBy`, and `sortDirection` are declared on `PlatformAdminRequest`.
   - The exact same four fields are also declared on `SaasPlanFilterRequest`.
   - The exact same four fields are also declared on `GymTenantFilterRequest`.
2. **Parameter Precedence Ambiguity**:
   - If a caller supplies `page = 0` at the root and `planFilter.page = 5`, which one takes precedence?
   - In current code, `GetAllSaasPlansService` and `GetGymTenantsService` are forced to implement defensive fallback cascades:
     ```java
     int page = request != null && request.getPage() != null ? request.getPage()
             : (request != null && request.getPlanFilter() != null && request.getPlanFilter().getPage() != null
             ? request.getPlanFilter().getPage() : 0);
     ```
3. **Cross-Operation Field Sprawl & Collision on `PlatformAdminRequest`**:
   - `minPrice` and `maxPrice` sit loosely on the root `PlatformAdminRequest`, yet they are only applicable when querying SaaS plans.
   - `gymStatus`, `city`, `gymName`, and `gymCode` sit loosely on the root `PlatformAdminRequest`, yet they are only applicable when querying gym tenants.
   - Field names collide across operations: `name` on root represents the plan name to create in `CREATE_SAAS_PLAN`, but was also checked as a search keyword filter in `GetAllSaasPlansService`!
   - `active` on root represents the target state for `UPDATE_PLAN_AVAILABILITY`, but was also checked as a filter flag in `GetAllSaasPlansService`!
4. **Dual-Path Operation Payloads**:
   - `UPDATE_PLAN_AVAILABILITY` supports both flat fields (`planId`, `active`) and a nested DTO (`UpdatePlanAvailabilityRequest planAvailability`), requiring duplicate validation logic in `PlatformAdminValidationsImpl`.
   - `FETCH_ALL_GYM_TENANTS` has naming mismatches between flat fields (`gymStatus`) and nested fields (`status`).
5. **Divergence from ConnectSouq Reference Module Standard**:
   - In ConnectSouq's `FranchiseOperatorRequest` (`D:\projects\ConnectSouq\src\main\java\com\bxb\ConnectSouq\franchiseOperator\request\FranchiseOperatorRequest.java`), common pagination parameters (`page`, `size`) exist **exclusively at the root level**. They are never duplicated inside nested filter objects.
   - Domain-specific complex payloads are encapsulated into dedicated nested objects (e.g., `OperatorOnboardingRequest onboardingRequest;`), keeping the shared request clean, cohesive, and deterministic.

---

## 3. Goals & Expected Outcomes

1. **Centralize Pagination & Sorting**:
   - `page`, `size`, `sortBy`, and `sortDirection` reside exclusively at the root of `PlatformAdminRequest`.
   - Remove `page`, `size`, `sortBy`, and `sortDirection` from `SaasPlanFilterRequest` and `GymTenantFilterRequest`.
2. **Encapsulate Domain Filters into Nested DTOs**:
   - SaaS plan query filters (`active`, `billingCycle`, `name`, `minPrice`, `maxPrice`) reside cleanly in `SaasPlanFilterRequest planFilter;`.
   - Gym tenant query filters (`status`, `city`, `gymName`, `gymCode`) reside cleanly in `GymTenantFilterRequest tenantFilter;`.
   - Remove flat `minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, and `gymCode` from the root of `PlatformAdminRequest`.
3. **Canonical Single-Source Reading**:
   - Services and validation components read pagination and sorting strictly from `request.getPage()`, `request.getSize()`, `request.getSortBy()`, and `request.getSortDirection()`.
   - Services and validation components read filter criteria strictly from `request.getPlanFilter()` or `request.getTenantFilter()`.
   - Eliminate all ternary fallback cascades in `GetAllSaasPlansService`, `GetGymTenantsService`, and `PlatformAdminValidationsImpl`.
4. **Standardize Operation-Specific Payloads**:
   - Single plan retrieval (`FETCH_SAAS_PLAN_BY_ID`) uses root `planId`.
   - Availability management (`UPDATE_PLAN_AVAILABILITY`) standardizes on root `planId` and `active` (or dedicated nested object; see Section 14 Decision).
5. **100% Backward Verification**:
   - Ensure all 61 existing test cases pass or are cleanly updated to the canonical payload standard.
   - Add dedicated consolidation tests verifying that root pagination applies uniformly across both plan and tenant queries.

---

## 4. Current-State Findings (Code Inspection Proof)

| Component | File Path | Current State & Defect |
| :--- | :--- | :--- |
| **`PlatformAdminRequest`** | `src/.../platformAdmin/request/PlatformAdminRequest.java` | Declares root pagination (`page`, `size`, `sortBy`, `sortDirection`), flat plan filters (`minPrice`, `maxPrice`), flat tenant filters (`gymStatus`, `city`, `gymName`, `gymCode`), dual availability fields (`planId`, `active` AND `planAvailability`), and nested filter objects (`planFilter`, `tenantFilter`). Total 63 lines of mixed flat/nested fields. |
| **`SaasPlanFilterRequest`** | `src/.../platformAdmin/request/SaasPlanFilterRequest.java` | Lines 27–30 declare duplicate `page`, `size`, `sortBy`, `sortDirection` alongside filters `active`, `billingCycle`, `name`, `minPrice`, `maxPrice`. |
| **`GymTenantFilterRequest`** | `src/.../platformAdmin/request/GymTenantFilterRequest.java` | Lines 24–27 declare duplicate `page`, `size`, `sortBy`, `sortDirection` alongside filters `status`, `city`, `gymName`, `gymCode`. |
| **`GetAllSaasPlansService`** | `src/.../platformAdmin/service/GetAllSaasPlansService.java` | Lines 55–80 contain 10 fallback ternary checks checking both root fields and `planFilter` fields for every single parameter. |
| **`GetGymTenantsService`** | `src/.../platformAdmin/service/GetGymTenantsService.java` | Lines 53–78 contain 8 fallback ternary checks checking both root fields and `tenantFilter` fields for every single parameter. |
| **`PlatformAdminValidationsImpl`** | `src/.../platformAdmin/validations/PlatformAdminValidationsImpl.java` | Lines 78–89 (`validateFetchPlans`) and lines 173–180 (`validateFetchGymTenants`) duplicate fallback resolution logic before executing validation assertions. Lines 144–156 (`validateUpdatePlanAvailability`) check both flat fields and `planAvailability`. |
| **ConnectSouq Reference (`FranchiseOperatorRequest`)** | `D:\projects\ConnectSouq\...\FranchiseOperatorRequest.java` | Lines 37–38 declare `page` and `size` at the root. No nested filter DTO duplicates pagination. Complex actions use nested DTOs (`onboardingRequest`). Action IDs sit at root (`applicationId`, `commissionLedgerEntryId`). |

---

## 5. Proposed Request & Response DTO Structure

### 5.1 Consolidated `PlatformAdminRequest`
```java
package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Shared module-level request DTO for Platform Administration.
 * Modeled after ConnectSouq's FranchiseOperatorRequest standard.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAdminRequest {

    // Discriminator
    private PlatformAdminRequestType platformAdminRequestType;

    // ==========================================
    // Unified Pagination & Sorting (Root Level)
    // Applies across all multi-record fetch queries
    // ==========================================
    private Integer page;          // default: 0, min: 0
    private Integer size;          // default: 10, min: 1, max: 100
    private String sortBy;        // whitelist-guarded per operation
    private String sortDirection; // "asc" | "desc"

    // ==========================================
    // CREATE_SAAS_PLAN fields
    // ==========================================
    private String name;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private Integer maxMembers;
    private Integer maxTrainers;
    private Boolean analyticsEnabled;
    private Boolean multiTrainerEnabled;
    private Boolean customBrandingEnabled;

    // ==========================================
    // FETCH_SAAS_PLAN_BY_ID target identifier
    // ==========================================
    private Long planId;

    // ==========================================
    // UPDATE_PLAN_AVAILABILITY fields
    // ==========================================
    private Boolean active;
    private UpdatePlanAvailabilityRequest planAvailability; // Optional nested alternative

    // ==========================================
    // FETCH_ALL_SAAS_PLANS domain filter criteria
    // ==========================================
    private SaasPlanFilterRequest planFilter;

    // ==========================================
    // FETCH_ALL_GYM_TENANTS domain filter criteria
    // ==========================================
    private GymTenantFilterRequest tenantFilter;
}
```

### 5.2 Cleaned `SaasPlanFilterRequest` (Pure Filter Criteria Only)
```java
package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Filter criteria for querying SaaS subscription plans.
 * Pagination and sorting are managed at the root PlatformAdminRequest level.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaasPlanFilterRequest {

    private Boolean active;
    private BillingCycle billingCycle;
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
```

### 5.3 Cleaned `GymTenantFilterRequest` (Pure Filter Criteria Only)
```java
package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.GymStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter criteria for querying registered gym tenants.
 * Pagination and sorting are managed at the root PlatformAdminRequest level.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymTenantFilterRequest {

    private GymStatus status;
    private String city;
    private String gymName;
    private String gymCode;
}
```

### 5.4 `PlatformAdminResponse` (Retained Standard Schema)
```java
// Retains existing structure conforming to ConnectSouq FranchiseOperatorResponse standard:
// - Direct scalar convenience fields: message, planId, id, name, price, active
// - Nested domain results: saasPlan (SaasPlanResponse), plans (List<SaasPlanResponse>), tenants (List<GymTenantSummaryResponse>)
// - Standard pagination envelope: pagedPlans (PagedResponse<SaasPlanResponse>), pagedTenants (PagedResponse<GymTenantSummaryResponse>)
// - Root pagination metadata: currentPage, totalPages, totalRecords, pageSize, last
```

---

## 6. Exact Scope of Changes

### 1. DTO Simplification:
- Remove `page`, `size`, `sortBy`, `sortDirection` from `SaasPlanFilterRequest`.
- Remove `page`, `size`, `sortBy`, `sortDirection` from `GymTenantFilterRequest`.
- Remove flat filter fields (`minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, `gymCode`) from `PlatformAdminRequest`.

### 2. Service Streamlining:
- In `GetAllSaasPlansService`:
  - Read `page`, `size`, `sortBy`, `sortDirection` directly from `request.getPage()`, `request.getSize()`, `request.getSortBy()`, `request.getSortDirection()`.
  - Read `active`, `billingCycle`, `name`, `minPrice`, `maxPrice` directly from `request.getPlanFilter()`.
  - Remove all dual-path fallback resolution ternary chains.
- In `GetGymTenantsService`:
  - Read `page`, `size`, `sortBy`, `sortDirection` directly from `request.getPage()`, `request.getSize()`, `request.getSortBy()`, `request.getSortDirection()`.
  - Read `status`, `city`, `gymName`, `gymCode` directly from `request.getTenantFilter()`.
  - Remove all dual-path fallback resolution ternary chains.

### 3. Validation Simplification:
- In `PlatformAdminValidationsImpl`:
  - Extract a private reusable helper `validatePagination(Integer page, Integer size, String sortBy, String sortDirection, Set<String> allowedSortFields)`.
  - In `validateFetchPlans`: validate root pagination via helper; validate `planFilter.minPrice` and `planFilter.maxPrice` directly.
  - In `validateFetchGymTenants`: validate root pagination via helper against tenant sort whitelist.
  - Remove all fallback ternary chains from validation methods.

### 4. Integration Test Updates:
- Update requests in `GetAllSaasPlansIntegrationTest` and `GetGymTenantsIntegrationTest` where filter parameters were passed as flat root fields to use `planFilter` or `tenantFilter`.
- Retain root pagination parameters (`page`, `size`, `sortBy`, `sortDirection`).
- Create `PlatformAdminDtoConsolidationIntegrationTest` to explicitly verify consolidated DTO behavior and edge cases.

### 5. Documentation Updates:
- Update `docs/payloads.md` Sections 1.2 (`FETCH_ALL_SAAS_PLANS`) and 1.5 (`FETCH_ALL_GYM_TENANTS`) to show the standardized canonical payload structure.

---

## 7. Explicit Out-of-Scope Items

1. **Factory Architecture Replacement**: Do NOT alter `PlatformAdminFactory` or `PlatformAdminFactoryOperation`. The module-level factory must remain strictly as-is.
2. **Controller Routing Changes**: Do NOT add any `@GetMapping`, `@PutMapping`, or sub-path `@PostMapping` endpoints. `POST /api/platform-admin` remains the sole controller gateway.
3. **Database Changes**: No database schema alterations, Flyway migrations, or entity modifications.
4. **Protected Multitenancy Classes**: All eight classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
5. **Business Logic Changes**: Filtering, sorting, and pagination behavior inside JPA Specifications remains identical; only the DTO ingress mapping is standardized.

---

## 8. Detailed Acceptance Criteria

### AC-PA7.1: Centralized Pagination & Sorting Invariant
- **Given** a multi-record query (`FETCH_ALL_SAAS_PLANS` or `FETCH_ALL_GYM_TENANTS`),
- **When** the client supplies `page = 1`, `size = 5`, `sortBy = "name"`, `sortDirection = "asc"` at the root of `PlatformAdminRequest`,
- **Then** the service strictly applies these pagination and sorting parameters in the resulting `Pageable` query,
- **And** returns `currentPage = 1`, `pageSize = 5`, and properly ordered records.

### AC-PA7.2: Pure Filter Criteria Encapsulation
- **Given** a `FETCH_ALL_SAAS_PLANS` request,
- **When** the client specifies filter criteria (`active = true`, `billingCycle = MONTHLY`, `minPrice = 20.00`, `maxPrice = 100.00`, `name = "pro"`),
- **Then** these criteria are read exclusively from `request.getPlanFilter()`,
- **And** the JPA Specification queries MySQL matching exactly these criteria.

### AC-PA7.3: Gym Tenant Filter Criteria Encapsulation
- **Given** a `FETCH_ALL_GYM_TENANTS` request,
- **When** the client specifies filter criteria (`status = ACTIVE`, `city = "Austin"`, `gymName = "Iron"`, `gymCode = "IR"`),
- **Then** these criteria are read exclusively from `request.getTenantFilter()`,
- **And** the JPA Specification queries MySQL matching exactly these criteria.

### AC-PA7.4: Elimination of Duplicate Filter Fields
- **Given** the source code of `SaasPlanFilterRequest` and `GymTenantFilterRequest`,
- **When** inspecting declared fields,
- **Then** neither class declares `page`, `size`, `sortBy`, or `sortDirection`.

### AC-PA7.5: Elimination of Spurious Flat Filter Fields on Root Request
- **Given** the source code of `PlatformAdminRequest`,
- **When** inspecting declared fields,
- **Then** flat filter fields `minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, and `gymCode` are no longer present on `PlatformAdminRequest`.

### AC-PA7.6: Reusable Pagination Validation & Whitelist Guarding
- **Given** any query request with `page < 0`,
- **When** validation executes,
- **Then** `PlatformAdminValidations` throws `BadRequestException("Page index must not be negative")`.
- **Given** any query request with `size < 1` or `size > 100`,
- **When** validation executes,
- **Then** `PlatformAdminValidations` throws `BadRequestException("Page size must be between 1 and 100")`.
- **Given** a sort property not in the operation's whitelist,
- **When** validation executes,
- **Then** `PlatformAdminValidations` throws `BadRequestException` identifying the invalid field and listing allowed fields.

### AC-PA7.7: Zero Regression Across Entire Test Suite
- **Given** the consolidation refactoring is complete,
- **When** running `mvn test`,
- **Then** all 61 existing test cases and newly added consolidation test cases pass with exit code 0.

---

## 9. Validation & Error-Handling Requirements

1. **Root-Level Pagination Defaults**:
   - `page`: default `0` if null; reject if `< 0` with HTTP 400.
   - `size`: default `10` if null; reject if `< 1` or `> 100` with HTTP 400.
   - `sortDirection`: default `"asc"` for plans, `"desc"` for tenants; reject if not `"asc"` or `"desc"` (case-insensitive) with HTTP 400.
2. **Operation-Specific Sort Whitelists**:
   - SaaS Plans: `Set.of("id", "name", "price", "billingCycle", "maxMembers", "maxTrainers")`.
   - Gym Tenants: `Set.of("id", "gymName", "gymCode", "city", "status", "registeredAt")`.
   - Reject any sort field outside the operation whitelist with HTTP 400.
3. **Price Range Validation (`planFilter`)**:
   - If `planFilter != null`:
     - `minPrice` must not be negative (`>= 0.00`).
     - `maxPrice` must not be negative (`>= 0.00`).
     - `minPrice` cannot be greater than `maxPrice`.

---

## 10. Relevant Files & Components Likely to be Affected

```
src/main/java/com/company/GymVerse/
├── platformAdmin/
│   ├── request/
│   │   ├── PlatformAdminRequest.java          # Remove duplicate flat filters; centralize page/size/sort
│   │   ├── SaasPlanFilterRequest.java         # Remove page, size, sortBy, sortDirection
│   │   └── GymTenantFilterRequest.java        # Remove page, size, sortBy, sortDirection
│   ├── service/
│   │   ├── GetAllSaasPlansService.java        # Read pagination from root, filters from planFilter
│   │   └── GetGymTenantsService.java          # Read pagination from root, filters from tenantFilter
│   └── validations/
│       ├── PlatformAdminValidations.java      # Validation method contracts
│       └── PlatformAdminValidationsImpl.java  # Reusable pagination validator; remove fallback checks
docs/
└── payloads.md                                # Update Section 1.2 and Section 1.5 schemas
src/test/java/com/company/GymVerse/platformAdmin/
├── GetAllSaasPlansIntegrationTest.java        # Update test requests to canonical structure
├── GetGymTenantsIntegrationTest.java          # Update test requests to canonical structure
└── PlatformAdminDtoConsolidationIntegrationTest.java # New regression & edge-case test suite
```

---

## 11. Testing Requirements

1. **`PlatformAdminDtoConsolidationIntegrationTest`**:
   - Verify root `page` and `size` apply correctly to `FETCH_ALL_SAAS_PLANS`.
   - Verify root `page` and `size` apply correctly to `FETCH_ALL_GYM_TENANTS`.
   - Verify `planFilter` criteria (`active`, `billingCycle`, `minPrice`, `maxPrice`, `name`) filter plans correctly without root fallbacks.
   - Verify `tenantFilter` criteria (`status`, `city`, `gymName`, `gymCode`) filter tenants correctly without root fallbacks.
   - Verify invalid pagination values (`page = -1`, `size = 0`, `size = 150`) throw HTTP 400.
   - Verify invalid sort fields throw HTTP 400.
   - Verify null `planFilter` or null `tenantFilter` cleanly executes query returning all records with default pagination.
2. **Existing Test Suite Regression**:
   - `CreateSaasPlanIntegrationTest` (8 tests)
   - `GetSaasPlanByIdIntegrationTest` (4 tests)
   - `UpdatePlanAvailabilityIntegrationTest` (8 tests)
   - `GetAllSaasPlansIntegrationTest` (10 tests)
   - `GetGymTenantsIntegrationTest` (12 tests)
   - `PlatformAdminProvisioningIntegrationTest` (1 test)
   - `PlatformAdminSeedPropertiesTest` (1 test)
   - Phase 1 tests (Central & Tenant persistence, isolation, app tests: 17 tests)
   - Total: All 61+ tests passing.

---

## 12. API Documentation Requirements (`docs/payloads.md`)

Update `/docs/payloads.md`:
1. **Section 1.2 (`FETCH_ALL_SAAS_PLANS`)**:
   - Update Request Payload example to show `page`, `size`, `sortBy`, `sortDirection` at root, and filter criteria in `planFilter`.
   - Remove references to flat `minPrice` / `maxPrice` or nested `planFilter.page`.
   - Provide clean copy-pasteable cURL command.
2. **Section 1.5 (`FETCH_ALL_GYM_TENANTS`)**:
   - Update Request Payload example to show `page`, `size`, `sortBy`, `sortDirection` at root, and filter criteria in `tenantFilter`.
   - Remove references to flat `gymStatus` / `city` or nested `tenantFilter.page`.
   - Provide clean copy-pasteable cURL command.

---

## 13. Dependencies & Implementation Ordering

- **Prerequisites**: Stories PA-1, PA-2, PA-3, PA-4, PA-5, PA-6 must be completed.
- **Execution Position**: Story PA-7 is an optimization and refactoring story positioned immediately following Story PA-6.
- **Ordering**:
  1. Refactor DTOs: `SaasPlanFilterRequest`, `GymTenantFilterRequest`, `PlatformAdminRequest`.
  2. Refactor Validations: `PlatformAdminValidations` and `PlatformAdminValidationsImpl`.
  3. Refactor Services: `GetAllSaasPlansService`, `GetGymTenantsService`.
  4. Update existing tests and add `PlatformAdminDtoConsolidationIntegrationTest`.
  5. Update `docs/payloads.md`.
  6. Execute full verification suite (`mvn test`).

---

## 14. Risks, Trade-offs & Unresolved Architectural Questions

### Trade-off 1: Plan Creation Payload — Flat on Root vs. Nested `CreateSaasPlanRequest`
- **Option 1A (Keep Flat on Root — ConnectSouq Event Pattern)**:
  - `name`, `description`, `price`, `billingCycle`, etc., remain at the root of `PlatformAdminRequest`.
  - *Pros:* Matches ConnectSouq where event creation fields (`title`, `eventType`, etc.) sit on `FranchiseOperatorRequest`. Minimizes changes to `CreateSaasPlanService`.
  - *Cons:* Root class retains 9 plan-creation fields that are unused during other operations.
- **Option 1B (Nest in `CreateSaasPlanRequest createPlan`)**:
  - Encapsulate the 9 creation fields in a dedicated `CreateSaasPlanRequest` nested DTO.
  - *Pros:* Ultimate cleanliness; `PlatformAdminRequest` contains only discriminator, pagination, and operation sub-objects.
  - *Cons:* Breaks existing `CreateSaasPlanRequest` callers/tests; adds another nested object.
- **Recommendation:** Option 1A is recommended to preserve existing client contracts for `CREATE_SAAS_PLAN` and mirror ConnectSouq's `createEvent` pattern.

### Trade-off 2: Plan Availability Payload — Flat (`planId`, `active`) vs. Nested `planAvailability`
- **Option 2A (Flat on Root)**:
  - `planId` and `active` sit at root. `planId` is shared with `FETCH_SAAS_PLAN_BY_ID`.
  - Remove `UpdatePlanAvailabilityRequest planAvailability`.
  - *Pros:* Highly concise payload: `{"platformAdminRequestType": "UPDATE_PLAN_AVAILABILITY", "planId": 3, "active": false}`.
- **Option 2B (Nested `planAvailability`)**:
  - Retain `UpdatePlanAvailabilityRequest planAvailability` and remove root `active`.
  - *Pros:* Explicit encapsulation of availability payload.
- **Recommendation:** Option 2A is recommended because `planId` is naturally a top-level entity identifier (like `applicationId` in ConnectSouq), and `active` is a single boolean flag.

---

## 15. Verification Checklist

- [ ] `SaasPlanFilterRequest` has `page`, `size`, `sortBy`, `sortDirection` removed.
- [ ] `GymTenantFilterRequest` has `page`, `size`, `sortBy`, `sortDirection` removed.
- [ ] `PlatformAdminRequest` has flat filter fields removed and pagination centralized at root.
- [ ] `GetAllSaasPlansService` reads pagination from root and filters from `planFilter`.
- [ ] `GetGymTenantsService` reads pagination from root and filters from `tenantFilter`.
- [ ] `PlatformAdminValidationsImpl` validates pagination cleanly without fallback chains.
- [ ] All 61 existing automated integration tests pass with exit code 0.
- [ ] New `PlatformAdminDtoConsolidationIntegrationTest` passes cleanly.
- [ ] Multitenancy protected classes remain 100% byte-for-byte untouched.
- [ ] `/docs/payloads.md` updated with revised payloads and cURL commands.
- [ ] Zero git commits or pushes executed autonomously (Rule 1).
