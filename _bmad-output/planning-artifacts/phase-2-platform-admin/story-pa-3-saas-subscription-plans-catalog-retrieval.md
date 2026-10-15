---
id: "story-pa-3"
epic_id: "epic-pa"
title: "SaaS Subscription Plans Dynamic Query & Catalog Retrieval"
status: "planning"
type: "platform-admin-feature"
created_at: "2026-10-15"
amended_at: "2026-10-15"
dependencies:
  - "story-pa-2"
files_to_modify:
  - "src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/platformAdmin/request/SaasPlanFilterRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/specifications/SaasPlanSpecification.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/response/PagedResponse.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/GetAllSaasPlansService.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/GetAllSaasPlansIntegrationTest.java"
---

# Story PA-3: SaaS Subscription Plans Dynamic Query & Catalog Retrieval

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.SaasSubscriptionPlan` and `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`.  
> 3. **JPA SPECIFICATIONS REQUIREMENT:** Dynamic queries must be constructed using Spring Data JPA Specifications (`JpaSpecificationExecutor<SaasSubscriptionPlan>`). Do NOT retrieve all records into memory and filter in Java.  
> 4. **SORT WHITELIST GUARD:** You must validate sort fields against an explicit whitelist of entity properties to prevent property path injection or SQL runtime errors.  
> 5. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 6. You must document the API contract in `/docs/payloads.md` with all 13 mandatory fields and copy-pasteable cURL commands (`PROMPT.md` Section 10).  
> 7. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** GymVerse Platform Administrator,  
**I want to** dynamically filter, sort, and paginate through the catalog of SaaS subscription plans using Spring Data JPA Specifications,  
**So that** I can efficiently search, inspect, and evaluate plan offerings across active/inactive statuses, billing cycles, pricing ranges, and capacity limits with consistent pagination.

---

## 2. Business & Technical Context

Platform Administrators oversee the entire catalog of SaaS plans ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 4.1 & 7). As the platform matures, dozens of historical, promotional, and active plans exist in `saas_subscription_plans`:
- Administrators need composable query filters (by status, name search, billing cycle, price ranges).
- Standard database performance requires building dynamic SQL queries via JPA `Specification<SaasSubscriptionPlan>` and executing them at the database engine level via `Pageable`.
- Results must adhere strictly to `PROMPT.md` Section 4.5 pagination standards (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`).
- Sort parameters must be verified against an allowed whitelist of entity property names to prevent crashes or unintended property traversal.

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Central persistence foundation active.
  - Story PA-2 completed (`CreateSaasPlanResponse`, `SaasPlanMapper`, and exception hierarchy created).
- **Dependencies**:
  - `SaasSubscriptionPlanRepository` must be updated to extend `JpaSpecificationExecutor<SaasSubscriptionPlan>`.

---

## 4. Scope & Boundaries

### In Scope
1. **Repository Extension (`SaasSubscriptionPlanRepository`)**:
   - Update repository declaration:
     ```java
     public interface SaasSubscriptionPlanRepository extends 
         JpaRepository<SaasSubscriptionPlan, Long>, 
         JpaSpecificationExecutor<SaasSubscriptionPlan> { ... }
     ```
2. **Filter & Query Model (`SaasPlanFilterRequest`)**:
   - Filter criteria:
     - `Boolean active` (optional: filter by active/inactive)
     - `BillingCycle billingCycle` (optional: MONTHLY, QUARTERLY, ANNUAL)
     - `String name` (optional: case-insensitive partial match `%name%`)
     - `BigDecimal minPrice` (optional: `price >= minPrice`)
     - `BigDecimal maxPrice` (optional: `price <= maxPrice`)
   - Pagination & Sorting parameters:
     - `Integer page` (default `0`)
     - `Integer size` (default `10`)
     - `String sortBy` (default `"id"`)
     - `String sortDirection` (default `"asc"`, accepts `"asc"` or `"desc"`)
3. **Specification Builder (`SaasPlanSpecification`)**:
   - Implements `org.springframework.data.jpa.domain.Specification<SaasSubscriptionPlan>`.
   - Combines predicates using `CriteriaBuilder`:
     - If `active != null` -> `cb.equal(root.get("active"), active)`
     - If `billingCycle != null` -> `cb.equal(root.get("billingCycle"), billingCycle)`
     - If `name != null && !name.isBlank()` -> `cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")`
     - If `minPrice != null` -> `cb.greaterThanOrEqualTo(root.get("price"), minPrice)`
     - If `maxPrice != null` -> `cb.lessThanOrEqualTo(root.get("price"), maxPrice)`
4. **Validation Guard (`PlatformAdminValidations`)**:
   - Whitelist of allowed sort fields: `Set.of("id", "name", "price", "billingCycle", "maxMembers", "maxTrainers")`.
   - If `sortBy` is not in whitelist -> throw `BadRequestException("Invalid sort field: '" + sortBy + "'. Allowed fields: [id, name, price, billingCycle, maxMembers, maxTrainers]")`.
   - If `page < 0` -> throw `BadRequestException("Page index must not be negative")`.
   - If `size < 1 || size > 100` -> throw `BadRequestException("Page size must be between 1 and 100")`.
   - If `minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0` -> throw `BadRequestException("minPrice cannot be greater than maxPrice")`.
5. **Standard Paginated Envelope (`PagedResponse<T>`)**:
   - Matching `PROMPT.md` Section 4.5:
     ```java
     public record PagedResponse<T>(
         List<T> content,
         int page,
         int size,
         long totalElements,
         int totalPages,
         boolean last
     ) {}
     ```
6. **Service Operation (`GetAllSaasPlansService`)**:
   - Injected with `SaasSubscriptionPlanRepository`, `SaasPlanMapper`, and `PlatformAdminValidations`.
   - Validates query parameters, builds `Specification` and `Pageable` (`PageRequest.of(page, size, Sort.by(...))`).
   - Executes `saasSubscriptionPlanRepository.findAll(spec, pageable)` under `@Transactional(transactionManager = "centralTransactionManager", readOnly = true)`.
   - Returns `PagedResponse<SaasPlanResponse>`.
7. **Controller Endpoint (`PlatformAdminController`)**:
   - `GET /api/platform-admin/plans` accepting filter, pagination, and sorting query parameters.
8. **Documentation**:
   - Update `docs/payloads.md` with all query parameters, defaults, and paginated response schema.

### Out of Scope
- Mutating plan data.
- Tenant database routing.
- Client authentication implementation.

---

## 5. Detailed Acceptance Criteria

### AC-PA3.1: Dynamic Filtering by Status and Billing Cycle
- **Given** five SaaS plans in the central database:
  - Plan A: `name = "Monthly Pro"`, `billingCycle = MONTHLY`, `active = true`, `price = 49.99`
  - Plan B: `name = "Annual Pro"`, `billingCycle = ANNUAL`, `active = true`, `price = 499.99`
  - Plan C: `name = "Monthly Basic"`, `billingCycle = MONTHLY`, `active = false`, `price = 29.99`
- **When** calling `GET /api/platform-admin/plans?active=true&billingCycle=MONTHLY`,
- **Then** the JPA Specification queries MySQL with `WHERE active = true AND billing_cycle = 'MONTHLY'`,
- **And** returns HTTP `200 OK` with `PagedResponse` containing only Plan A (`totalElements = 1`, `totalPages = 1`).

### AC-PA3.2: Partial Name Search & Price Range Filtering
- **Given** plans with various names and prices,
- **When** calling `GET /api/platform-admin/plans?name=pro&minPrice=40.00&maxPrice=100.00`,
- **Then** the service queries with `LOWER(name) LIKE '%pro%' AND price >= 40.00 AND price <= 100.00`,
- **And** returns HTTP `200 OK` containing matching plans.

### AC-PA3.3: Whitelist-Guarded Sorting
- **Given** a request with `sortBy=price&sortDirection=desc`,
- **When** the query executes,
- **Then** the results are sorted by `price` descending.
- **Given** a request with an illegal sort property `sortBy=secretField` or `sortBy=password`,
- **When** validation executes,
- **Then** `PlatformAdminValidations` throws `BadRequestException("Invalid sort field: 'secretField'. Allowed fields: ...")`,
- **And** returns HTTP `400 Bad Request`.

### AC-PA3.4: Validated Pagination Boundaries
- **Given** 25 plans exist,
- **When** calling `GET /api/platform-admin/plans?page=1&size=10`,
- **Then** the response returns items 11 through 20 with `page = 1`, `size = 10`, `totalElements = 25`, `totalPages = 3`, `last = false`.
- **Given** an invalid page request `page = -1` or `size = 0` or `size = 200`,
- **When** validation executes,
- **Then** it throws `BadRequestException` returning HTTP `400 Bad Request`.

### AC-PA3.5: Empty Results Structure
- **Given** query filters that match zero records,
- **When** the request executes,
- **Then** the response returns HTTP `200 OK` with `content = []`, `totalElements = 0`, `totalPages = 0`, `last = true`.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java`:
   - Extend `JpaSpecificationExecutor<SaasSubscriptionPlan>`.
2. `src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java`:
   - Add `GET /api/platform-admin/plans` with query parameters.
3. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java` & `Impl`:
   - Add sort and pagination parameter validation methods.
4. `docs/payloads.md`:
   - Update endpoint documentation with all query params and paginated schema.

### Files to Create:
1. `src/main/java/com/company/GymVerse/platformAdmin/request/SaasPlanFilterRequest.java`
2. `src/main/java/com/company/GymVerse/platformAdmin/specifications/SaasPlanSpecification.java`
3. `src/main/java/com/company/GymVerse/platformAdmin/response/PagedResponse.java`
4. `src/main/java/com/company/GymVerse/platformAdmin/service/GetAllSaasPlansService.java`
5. `src/test/java/com/company/GymVerse/platformAdmin/GetAllSaasPlansIntegrationTest.java`

---

## 7. Error Handling & Security Considerations

1. **Property Path Injection Prevention**:
   - Arbitrary client sort strings are rejected before passing to Hibernate / Spring Data.
2. **Database Performance**:
   - Page sizes are strictly capped at 100 to prevent large memory allocations.

---

## 8. Test Requirements

1. **`GetAllSaasPlansIntegrationTest`**:
   - **Test 1**: Verify dynamic filters (active only, billing cycle, price range, partial name).
   - **Test 2**: Verify sorting by allowed fields (`price`, `name`, `id`) in `asc` and `desc`.
   - **Test 3**: Verify pagination boundaries (first page, middle page, last page with `last = true`).
   - **Test 4**: Verify invalid sort field returns HTTP 400.
   - **Test 5**: Verify negative page or excessive size (> 100) returns HTTP 400.
   - **Test 6**: Verify empty matching result returns valid empty `PagedResponse`.

---

## 9. Definition of Done

- `SaasSubscriptionPlanRepository` extends `JpaSpecificationExecutor`.
- Filtering, sorting, and pagination execute entirely in MySQL via JPA Specifications.
- Sort whitelist rejects unauthorized property names.
- Integration tests pass against live MySQL central database.
- `/docs/payloads.md` updated with complete parameter specification.
- Task log maintained in `antigravity/`.
