# Platform Admin Module: Open Decisions & Architectural Ambiguities (Amended)

This document tracks specific technical decisions, trade-offs, and design options within the **Platform Admin Module** of `GymVerse-Backend-v2`, updated to reflect the plan amendments for `platformAdminSeed.yml` configuration and JPA Specifications fetch services.

---

## Decision 1: Controller Routing Paradigm (Unified Factory vs. Resource REST vs. Hybrid)

### Context:
- [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) Section 3 describes **Paradigm A: Unified Factory / Dispatcher Pattern** (a single endpoint receiving a request object with a discriminator enum) and **Paradigm B: Resource-Oriented RESTful Controller Pattern**.
- [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) Rule 4 commands: *"Respect the unified factory dispatching pattern. Do not add ad-hoc controller endpoints where factories/operations exist."*
- Dynamic JPA Specifications queries with query parameters (`?page=0&size=10&active=true&sortBy=price`) map naturally to HTTP GET routes.

### Proposed Options:
- **Option 1A (Pure Unified Factory Dispatcher)**:
  - Single controller endpoint: `POST /api/platform-admin`.
  - Body carries `PlatformAdminCommandRequest` with `requestType`.
  - *Pros:* 100% adherence to pure Factory pattern.
  - *Cons:* Awkward for simple GET queries (requires POSTing a JSON body for reads; not browser/cURL friendly for query caching).
- **Option 1B (Resource-Oriented RESTful Endpoints)**:
  - `POST /api/platform-admin/plans` (Create plan)
  - `GET /api/platform-admin/plans` (List plans with Specifications & pagination)
  - `GET /api/platform-admin/plans/{id}` (Get plan by ID)
  - `PATCH /api/platform-admin/plans/{id}/availability` (Update availability)
  - `GET /api/platform-admin/tenants` (List gym tenants with Specifications & pagination)
  - *Pros:* Highly standard, intuitive HTTP semantics, easily testable via browser/cURL.
  - *Cons:* Does not use the Factory router at the HTTP entry point.
- **Option 1C (Concrete Operation-Specific Factories & Dedicated REST Endpoints — APPROVED REFERENCE PATTERN)**:
  - Expose RESTful endpoints at the controller layer (`POST /plans`, `PATCH /plans/{id}/availability`, `GET /plans`, etc.).
  - Mutation endpoints delegate to dedicated concrete operation-specific factories (e.g. `CreateSaasPlanFactory`, `UpdatePlanAvailabilityFactory`) with strongly-typed request and response DTOs (`CreateSaasPlanRequest`, `CreateSaasPlanResponse`).
  - Read queries with JPA Specifications delegate directly to single-responsibility fetch services.
  - Aligns strictly with ConnectSouq reference module conventions and `PROMPT.md` Section 3.1.

**Decision Resolution:** Option 1C selected and codified in `PROMPT.md` Section 3.1.

---

## Decision 2: Resolved by User Directive — Dedicated `platformAdminSeed.yml` Configuration File

### Resolution:
In accordance with user requirements, the initial Platform Admin seed configuration is separated into a dedicated YAML file:
- **Location**: `src/main/resources/platformAdminSeed.yml`.
- **Import**: `src/main/resources/application.yaml` imports it via:
  ```yaml
  spring:
    config:
      import:
        - classpath:platformAdminSeed.yml
  ```
- **Mandatory Import Policy**: The import is mandatory. If `platformAdminSeed.yml` is missing, application context initialization fails fast.
- **Execution Disable Switch**: Environments that do not provision the initial admin explicitly disable seeding via `gymverse.platform-admin.seed.enabled: false`.
- **Secret Handling**: Sensitive values (password) use environment-variable placeholders `${PLATFORM_ADMIN_PASSWORD}` possessing **NO default fallback**. Plaintext credentials must never be committed to Git.
- **Binding Model**: Bound through `PlatformAdminSeedProperties` (`@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")`).
- **Seeding Hook**: Provisioning is executed automatically on startup via `PlatformAdminBootstrapConfig` (`ApplicationRunner`) delegating to `PlatformAdminProvisioningService`.

---

## Decision 3: Resolved by User Directive — Safe Conflict Guard & Idempotency Policy

### Resolution:
In accordance with user requirements:
- **Idempotent Restart**: If an account with the configured seed email already exists and has `isPlatformAdmin = true`, the seeder logs an info milestone and **DOES NOT** overwrite the password or alter timestamps.
- **Safe Conflict Guard (No Silent Promotion)**: If an account with the configured seed email exists and has `isPlatformAdmin = false`, the seeder **DOES NOT** silently promote that account or reset its password. Instead, it logs a critical error and throws `IllegalStateException`, aborting the startup to prevent unauthorized privilege escalation.
- **Privilege Isolation**: Ordinary user registrations strictly default `isPlatformAdmin` to `false` and never grant platform admin privileges.

---

## Decision 4: Resolved by User Directive — Standardized Fetch via JPA Specifications

### Resolution:
In accordance with user requirements:
- Multi-record fetch services (`GET /plans`, `GET /tenants`) use Spring Data JPA Specifications (`JpaSpecificationExecutor<T>`), building dynamic `CriteriaBuilder` queries in MySQL.
- In-memory filtering is strictly prohibited.
- Single-resource retrieval by ID (`GET /plans/{id}`) retains direct lookup semantics without pagination.
- Sort fields are validated against strict entity whitelists (`PlatformAdminValidations`).
- Pagination parameters are validated (`page >= 0`, `1 <= size <= 100`).
- Responses match standardized `PagedResponse<T>` conforming to `PROMPT.md` Section 4.5.

---

## Decision 5: Plan Lifecycle Management — Deactivation vs. Deletion

### Context:
In the central database schema (`V1__init_central_schema.sql`), `gym_subscriptions` references `saas_subscription_plans(id)` via `ON DELETE RESTRICT`. A SQL `DELETE` query against a plan with historical subscriptions will fail.

### Proposed Invariant:
- **Permanent Soft-Retirement**: SaaS plans **cannot be hard-deleted** via the Platform Admin API.
- Retiring a plan is accomplished strictly by setting `active = false` (Story PA-5).
- Inactive plans remain queryable by Platform Admins via `SaasPlanFilterRequest(active = false)`, but are hidden from tenant registration catalogs.

**Decision Required:** Please confirm that hard deletion of SaaS plans is prohibited and that soft-retirement via `active = false` is the approved lifecycle policy.

---

## Decision 6: Exposure of Internal Database Metadata (`db_name`) to Platform Admin

### Context:
- In `docs/architecture.md` ADR-4 ("Zero-Trust Server-Side Tenant Resolution"), `db_name` is strictly concealed from tenant users and tenant tokens.
- For the Platform Admin directory retrieval (`GET /api/platform-admin/tenants`), the central `gym_tenants` table contains `db_name` (e.g., `gym_db_001`).

### Proposed Options:
- **Option 6A (Include `db_name` in Response — RECOMMENDED)**:
  - Include `dbName` in `GymTenantSummaryResponse`.
  - *Rationale:* Platform Admins are infrastructure superadmins. Knowing the physical schema name is essential for diagnosing tenant database connectivity, running database maintenance, and auditing storage.
- **Option 6B (Conceal `db_name` from Response)**:
  - Omit `dbName` from the API response DTO entirely; expose only `gymCode`, `gymName`, status, and contact info.
  - *Rationale:* Maximizes internal database topology concealment even across administrative APIs.

**Decision Required:** Please specify whether `dbName` should be included in `GymTenantSummaryResponse` (Option 6A or Option 6B).

---

## Decision 7: Platform Admin Request DTO Consolidation & Operation Payload Organization (Story PA-7)

### Context:
Across Stories PA-2 through PA-6, `PlatformAdminRequest` accrued top-level pagination/sorting fields (`page`, `size`, `sortBy`, `sortDirection`) alongside duplicate fields in nested filter DTOs (`SaasPlanFilterRequest`, `GymTenantFilterRequest`), flat filter fields (`minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, `gymCode`), and dual availability fields (`planId`, `active` AND `planAvailability`).

### Proposed Consolidation Structure:
- **Centralized Pagination & Sorting**: Root `PlatformAdminRequest` holds `page`, `size`, `sortBy`, `sortDirection`. Removed from all nested filter DTOs.
- **Dedicated Filter DTOs**: `SaasPlanFilterRequest planFilter` and `GymTenantFilterRequest tenantFilter` hold pure filter parameters without duplicate pagination.
- **Removal of Spurious Flat Filters**: Flat filter fields (`minPrice`, `maxPrice`, `gymStatus`, `city`, `gymName`, `gymCode`) are eliminated from `PlatformAdminRequest`.

### Open Choices for Review:
1. **Sub-Issue 7.1: SaaS Plan Creation Payload Organization**:
   - **Option 7.1A (Flat on Root — ConnectSouq Event Pattern — RECOMMENDED)**:
     - Keep the 9 plan creation fields (`name`, `description`, `price`, `billingCycle`, `maxMembers`, `maxTrainers`, `analyticsEnabled`, `multiTrainerEnabled`, `customBrandingEnabled`) at the root of `PlatformAdminRequest`.
     - *Rationale:* Mirrors ConnectSouq's `FranchiseOperatorRequest` where event creation attributes sit directly on the module request. Preserves existing client contracts for `CREATE_SAAS_PLAN`.
   - **Option 7.1B (Encapsulate in Nested `CreateSaasPlanRequest createPlan`)**:
     - Group the 9 fields into a dedicated nested object `createPlan`.
     - *Rationale:* Isolates plan creation fields, making the root request leaner. Requires updating `CREATE_SAAS_PLAN` payload contracts.

2. **Sub-Issue 7.2: Plan Availability Payload Organization**:
   - **Option 7.2A (Flat on Root — RECOMMENDED)**:
     - Use root `Long planId;` and `Boolean active;`.
     - *Rationale:* `planId` is already a root scalar identifier shared with `FETCH_SAAS_PLAN_BY_ID`. Simplifies payload to `{"platformAdminRequestType": "UPDATE_PLAN_AVAILABILITY", "planId": 3, "active": false}`.
   - **Option 7.2B (Nested `UpdatePlanAvailabilityRequest planAvailability`)**:
     - Retain `planAvailability` nested object and eliminate root `active`.

**Decision Required:** Please review and confirm your preference on Sub-Issues 7.1 and 7.2.

