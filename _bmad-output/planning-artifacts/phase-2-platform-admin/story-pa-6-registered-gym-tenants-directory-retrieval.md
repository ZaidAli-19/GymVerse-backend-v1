---
id: "story-pa-6"
epic_id: "epic-pa"
title: "Platform Registered Gym Tenants Dynamic Directory Retrieval"
status: "planning"
type: "platform-admin-feature"
created_at: "2026-10-15"
amended_at: "2026-10-15"
dependencies: []
files_to_modify:
  - "src/main/java/com/company/GymVerse/repositories/central/GymTenantRepository.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidationsImpl.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/platformAdmin/request/GymTenantFilterRequest.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/specifications/GymTenantSpecification.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/response/GymTenantSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/mapper/GymTenantSummaryMapper.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/mapper/GymTenantSummaryMapperImpl.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/GetGymTenantsService.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/GetGymTenantsIntegrationTest.java"
---

# Story PA-6: Platform Registered Gym Tenants Dynamic Directory Retrieval

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.GymTenant` and `com.company.GymVerse.repositories.central.GymTenantRepository`.  
> 3. **JPA SPECIFICATIONS REQUIREMENT:** Dynamic queries must be executed against the central database (`gym_tenants`) using Spring Data JPA Specifications (`JpaSpecificationExecutor<GymTenant>`). Do NOT perform in-memory filtering.  
> 4. **SORT WHITELIST GUARD:** You must validate sort fields against an explicit whitelist of allowed entity properties to prevent property traversal attacks.  
> 5. **ZERO TENANT ROUTING:** This service runs exclusively on `centralTransactionManager` (read-only mode). It must never query individual tenant databases or touch `TenantContext`.  
> 6. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 7. You must document the API contract in `/docs/payloads.md` with all 13 mandatory fields and copy-pasteable cURL commands (`PROMPT.md` Section 10).  
> 8. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** GymVerse Platform Administrator,  
**I want to** dynamically filter, sort, and paginate through all registered gym tenants using Spring Data JPA Specifications,  
**So that** I have comprehensive, high-performance operational visibility into all facilities across the platform by status, city, gym code, and registration dates with standardized pagination.

---

## 2. Business & Technical Context

In the GymVerse SaaS platform ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 4.1 & 7):
- The central platform database maintains the authoritative directory of all gym tenants in table `gym_tenants`.
- Platform Administrators need to search, filter, and page through hundreds or thousands of registered gyms without loading entire tables into memory.
- Dynamic query criteria include:
  - Lifecycle status (`status = ACTIVE | SUSPENDED | INACTIVE`)
  - Geographic location (`city`)
  - Facility search (`gymName`, partial match)
  - Discovery code (`gymCode`, exact or prefix match)
- Paginated results must adhere strictly to `PROMPT.md` Section 4.5 using `PagedResponse<GymTenantSummaryResponse>`.
- Client sort fields must be strictly validated against an allowed whitelist.

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Central persistence foundation active (`GymTenant` entity).
  - Paged response envelope (`PagedResponse<T>`) and exception handlers available.
- **Dependencies**:
  - `GymTenantRepository` must be updated to extend `JpaSpecificationExecutor<GymTenant>`.

---

## 4. Scope & Boundaries

### In Scope
1. **Repository Extension (`GymTenantRepository`)**:
   - Update repository declaration:
     ```java
     public interface GymTenantRepository extends 
         JpaRepository<GymTenant, Long>, 
         JpaSpecificationExecutor<GymTenant> { ... }
     ```
2. **Filter & Query Model (`GymTenantFilterRequest`)**:
   - Filter criteria:
     - `GymStatus status` (optional: filter by ACTIVE, SUSPENDED, INACTIVE)
     - `String city` (optional: case-insensitive partial match)
     - `String gymName` (optional: case-insensitive partial match)
     - `String gymCode` (optional: case-insensitive prefix/exact match)
   - Pagination & Sorting parameters:
     - `Integer page` (default `0`)
     - `Integer size` (default `10`)
     - `String sortBy` (default `"registeredAt"`)
     - `String sortDirection` (default `"desc"`, accepts `"asc"` or `"desc"`)
3. **Specification Builder (`GymTenantSpecification`)**:
   - Implements `org.springframework.data.jpa.domain.Specification<GymTenant>`.
   - Composes predicates:
     - If `status != null` -> `cb.equal(root.get("status"), status)`
     - If `city != null && !city.isBlank()` -> `cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%")`
     - If `gymName != null && !gymName.isBlank()` -> `cb.like(cb.lower(root.get("gymName")), "%" + gymName.toLowerCase() + "%")`
     - If `gymCode != null && !gymCode.isBlank()` -> `cb.like(cb.lower(root.get("gymCode")), gymCode.toLowerCase() + "%")`
4. **Validation Guard (`PlatformAdminValidations`)**:
   - Whitelist of allowed sort fields: `Set.of("id", "gymName", "gymCode", "city", "status", "registeredAt")`.
   - If `sortBy` is not in whitelist -> throw `BadRequestException("Invalid sort field: '" + sortBy + "'. Allowed fields: [id, gymName, gymCode, city, status, registeredAt]")`.
   - If `page < 0` -> throw `BadRequestException("Page index must not be negative")`.
   - If `size < 1 || size > 100` -> throw `BadRequestException("Page size must be between 1 and 100")`.
5. **Response DTOs & Mapper**:
   - `GymTenantSummaryResponse`: contains `id`, `gymName`, `contactEmail`, `phoneNumber`, `address`, `city`, `logoUrl`, `gymCode`, `dbName`, `status`, `registeredAt`, `suspendedAt`, `suspensionReason`.
   - `GymTenantSummaryMapper` interface + `GymTenantSummaryMapperImpl` handwritten class per `RULES.md` Rule 4.
6. **Service Operation (`GetGymTenantsService`)**:
   - Executes under `@Transactional(transactionManager = "centralTransactionManager", readOnly = true)`.
   - Calls `gymTenantRepository.findAll(spec, pageable)` and maps entities to `PagedResponse<GymTenantSummaryResponse>`.
7. **Controller Endpoint (`PlatformAdminController`)**:
   - `GET /api/platform-admin/tenants` accepting filter and pagination query parameters.
8. **Documentation**:
   - Update `docs/payloads.md` with all query parameters, defaults, and paginated response schema.

### Out of Scope
- Modifying gym tenant records (tenant lifecycle mutations belong to subsequent administrative stories).
- Accessing individual tenant databases (`gym_db_<id>`).
- Client authentication or JWT parsing.

---

## 5. Detailed Acceptance Criteria

### AC-PA6.1: Dynamic Filtering by Status and City
- **Given** three gym tenants exist in MySQL `gym_tenants`:
  - Gym A: `gymName = "Iron Fitness"`, `city = "Austin"`, `status = ACTIVE`
  - Gym B: `gymName = "Metro Health"`, `city = "Dallas"`, `status = ACTIVE`
  - Gym C: `gymName = "Austin Fit"`, `city = "Austin"`, `status = SUSPENDED`
- **When** calling `GET /api/platform-admin/tenants?status=ACTIVE&city=austin`,
- **Then** the JPA Specification queries MySQL with `WHERE status = 'ACTIVE' AND LOWER(city) LIKE '%austin%'`,
- **And** returns HTTP `200 OK` with `PagedResponse` containing only Gym A (`totalElements = 1`, `totalPages = 1`).

### AC-PA6.2: Search by Gym Code and Partial Name
- **Given** registered tenants,
- **When** calling `GET /api/platform-admin/tenants?gymCode=IRON&gymName=fit`,
- **Then** the service queries with `LOWER(gym_code) LIKE 'iron%' AND LOWER(gym_name) LIKE '%fit%'`,
- **And** returns HTTP `200 OK` with matching results.

### AC-PA6.3: Whitelist-Guarded Sorting
- **Given** a request with `sortBy=registeredAt&sortDirection=desc`,
- **When** the query executes,
- **Then** the results are ordered by registration timestamp descending.
- **Given** a request with an unapproved sort property `sortBy=contactEmail` or `sortBy=passwordHash`,
- **When** validation executes,
- **Then** `PlatformAdminValidations` throws `BadRequestException("Invalid sort field: ...")`,
- **And** returns HTTP `400 Bad Request`.

### AC-PA6.4: Validated Pagination Boundaries
- **Given** registered tenants exist,
- **When** calling `GET /api/platform-admin/tenants?page=0&size=5`,
- **Then** the response returns the first 5 records with `page = 0`, `size = 5`, and correct `totalElements` and `totalPages`.
- **Given** invalid page parameters `page = -1` or `size = 0` or `size = 150`,
- **When** validation executes,
- **Then** it throws `BadRequestException` returning HTTP `400 Bad Request`.

### AC-PA6.5: Zero Tenant Database Routing Invariant
- **Given** the execution of `GetGymTenantsService`,
- **When** the database query is executed,
- **Then** `TenantContext.getCurrentTenant()` remains null/unaccessed,
- **And** no connections are acquired from `TenantDataSourceCache`.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/java/com/company/GymVerse/repositories/central/GymTenantRepository.java`:
   - Extend `JpaSpecificationExecutor<GymTenant>`.
2. `src/main/java/com/company/GymVerse/platformAdmin/controller/PlatformAdminController.java`:
   - Add `GET /api/platform-admin/tenants` endpoint.
3. `src/main/java/com/company/GymVerse/platformAdmin/validations/PlatformAdminValidations.java` & `Impl`:
   - Add tenant sort and pagination validation methods.
4. `docs/payloads.md`:
   - Update endpoint documentation with all query params and paginated schema.

### Files to Create:
1. `src/main/java/com/company/GymVerse/platformAdmin/request/GymTenantFilterRequest.java`
2. `src/main/java/com/company/GymVerse/platformAdmin/specifications/GymTenantSpecification.java`
3. `src/main/java/com/company/GymVerse/platformAdmin/response/GymTenantSummaryResponse.java`
4. `src/main/java/com/company/GymVerse/platformAdmin/mapper/GymTenantSummaryMapper.java`
5. `src/main/java/com/company/GymVerse/platformAdmin/mapper/GymTenantSummaryMapperImpl.java`
6. `src/main/java/com/company/GymVerse/platformAdmin/service/GetGymTenantsService.java`
7. `src/test/java/com/company/GymVerse/platformAdmin/GetGymTenantsIntegrationTest.java`

---

## 7. Error Handling & Security Considerations

1. **Security Context**:
   - In production, protected by Platform Admin role check (`ROLE_PLATFORM_ADMIN`).
2. **Defensive Validation**:
   - Invalid enum constants in `status` parameter are caught and mapped to HTTP `400 Bad Request`.

---

## 8. Test Requirements

1. **`GetGymTenantsIntegrationTest`**:
   - Seed test gym tenants in MySQL central database.
   - **Test 1**: Verify dynamic filters (status, city partial match, gymCode prefix).
   - **Test 2**: Verify sorting by allowed fields (`registeredAt`, `gymName`, `id`) in `asc` and `desc`.
   - **Test 3**: Verify pagination boundaries (page size, page navigation, `last = true`).
   - **Test 4**: Verify invalid sort field returns HTTP 400.
   - **Test 5**: Verify invalid page index or excessive size (> 100) returns HTTP 400.
   - **Test 6**: Verify zero tenant database connections are acquired.

---

## 9. Definition of Done

- `GymTenantRepository` extends `JpaSpecificationExecutor`.
- Filtering, sorting, and pagination execute entirely in MySQL via JPA Specifications.
- Sort whitelist rejects unauthorized property names.
- Integration tests pass against live MySQL central database.
- `/docs/payloads.md` updated with complete parameter specification.
- Task log maintained in `antigravity/`.
