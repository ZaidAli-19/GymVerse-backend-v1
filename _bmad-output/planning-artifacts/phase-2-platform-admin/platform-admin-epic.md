---
id: "epic-pa"
title: "Platform Admin Module — SaaS Platform Governance & Plan Management (Amended)"
status: "planning"
phase: "Phase 2 / Central Platform Administration"
created_at: "2026-10-15"
amended_at: "2026-10-15"
author: "Antigravity AI Agent (Pair Programming with Winston, System Architect)"
documents_referenced:
  - "PROMPT.md"
  - "RULES.md"
  - "docs/architecture.md"
  - "_bmad-output/planning-artifacts/phase-1/phase-1-epics.md"
---

# Epic PA: Platform Admin Module — SaaS Platform Governance & Plan Management (Amended)

## 1. Executive Summary & Business Objective

With **Phase 1: Enterprise Multi-Tenant Persistence Foundation** fully verified, `GymVerse-Backend-v2` possesses a dual-database architecture:
- A central platform database (`GymVerse`) hosting central user accounts, tenant registry, and platform-level subscriptions.
- Dynamically provisioned, isolated physical tenant databases (`gym_db_<id>`) for individual gym facilities.

The **Platform Admin Module** establishes the operational control plane for the **GymVerse SaaS Platform Owner/Superadmin**. The Platform Admin is fundamentally distinct from a gym tenant administrator (`TENANT_ADMIN`):
- A **Tenant Administrator** governs a single gym tenant (local members, check-ins, local trainers, and local subscription payments).
- The **Platform Administrator** (`isPlatformAdmin = true`) governs the entire SaaS ecosystem: creating and maintaining SaaS subscription tiers, toggling plan availability for prospective gyms, inspecting all registered gym tenants across the platform, and bootstrapping initial administrative access.

### Plan Amendments Incorporated:
1. **Dedicated YAML Seed Configuration (`platformAdminSeed.yml`)**:
   - Initial Platform Admin account seed properties are declared in a dedicated configuration file: `src/main/resources/platformAdminSeed.yml`.
   - `src/main/resources/application.yaml` imports `platformAdminSeed.yml` via Spring Boot configuration import (`spring.config.import: classpath:platformAdminSeed.yml`).
   - The import is mandatory, while seeding execution can be explicitly disabled via `gymverse.platform-admin.seed.enabled: false`.
   - Sensitive values use environment-variable placeholders without default password fallbacks (e.g. `${PLATFORM_ADMIN_PASSWORD}`).
   - Bound via `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")`, strictly separating configuration from seeding logic.
   - Idempotency guarantees: repeated startups never reset existing administrator passwords.
   - Safe conflict policy: existing non-admin accounts are never silently promoted to platform admin.
2. **Standardized Fetch Services via JPA Specifications**:
   - Multi-record fetch operations (`GET /plans`, `GET /tenants`) utilize Spring Data JPA Specifications (`JpaSpecificationExecutor<T>`) for dynamic database-level query composition, whitelist-validated sorting, and standardized pagination (`PagedResponse<T>`).
   - Single-resource retrieval (`GET /plans/{id}`) retains dedicated direct lookup semantics without pagination.

---

## 2. Invariants & Scope Boundaries

### 2.1 In Scope
1. **Initial Platform Admin Provisioning (`platformAdminSeed.yml`)**: A safe, repeatable, idempotent mechanism for provisioning the initial platform administrator using a dedicated YAML resource file imported by `application.yaml` and executed via `ApplicationRunner`.
2. **SaaS Subscription Plan Creation**: An administrative operation to define new SaaS plans with strict monetary precision (`BigDecimal`), resource limits, feature flags, and billing cycles via `CreateSaasPlanFactory`.
3. **Dynamic SaaS Subscription Plans Querying & Catalog Retrieval**: A dynamic query service utilizing Spring Data JPA Specifications, validated pagination, and whitelisted sorting to filter active/inactive plans, billing cycles, price ranges, and names.
4. **Single SaaS Subscription Plan Retrieval by Identifier**: Direct lookup of a specific SaaS plan by ID returning `SaasPlanResponse` or throwing `ResourceNotFoundException` (HTTP 404).
5. **Isolated Plan Availability Management**: A dedicated, surgical operation to toggle a plan's availability (`active` flag) without risking unintentional mutations to prices, billing cycles, or member/trainer limits.
6. **Dynamic Registered Gym Tenants Directory Retrieval**: A dynamic query service utilizing JPA Specifications, pagination, and whitelisted sorting to search and filter gym tenants across the platform.
7. **Modular Feature Organization**: Encapsulation of all module components strictly inside `com.company.GymVerse.platformAdmin` following `PROMPT.md` Section 2.
8. **Uniform Error Handling & Validation**: Custom domain exceptions (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`) and validation components mapped via a centralized `@RestControllerAdvice`.

### 2.2 Explicitly Out of Scope
1. **Authentication System Implementation**: No login, logout, registration, email OTP verification, JWT generation/parsing, refresh token rotation, or password recovery flows are implemented in this epic. (Authentication is planned as a separate, subsequent epic).
2. **Gym Tenant Self-Service Registration / Provisioning**: The workflow where gym owners register a gym and provision a physical tenant database (`REGISTER_GYM_ENDPOINT` / `TenantDatabaseService`) belongs to Phase 3 (Gym Registration & Tenant Provisioning).
3. **Tenant-Level Operations**: Modifying gym tenant operational data, members, attendance, or local databases is strictly prohibited.
4. **Modifying Protected Multitenancy Classes**: All eight classes in `com.company.GymVerse.multitanancy` remain 100% bit-for-bit untouched.
5. **Duplicating Phase 1 Work**: Central schemas and JPA entities created in Phase 1 are leveraged directly without duplicate definitions or schema alters.

---

## 3. Architecture & Design Pattern Alignment

### 3.1 Directory Layout & Package Structure
Per `PROMPT.md` Section 2.1, the Platform Admin module is organized as a self-contained feature module:

```
src/main/java/com/company/GymVerse/
├── platformAdmin/
│   ├── controller/
│   │   └── PlatformAdminController.java          # REST API ingress / Unified Dispatcher
│   ├── factory/
│   │   ├── CreateSaasPlanFactory.java            # Operation-specific factory for plan creation
│   │   └── UpdatePlanAvailabilityFactory.java    # Operation-specific factory for availability toggle
│   ├── service/
│   │   ├── CreateSaasPlanService.java            # Single-responsibility plan creation
│   │   ├── GetAllSaasPlansService.java           # Dynamic JPA Specification plan fetch
│   │   ├── GetSaasPlanByIdService.java           # Direct single plan retrieval by ID
│   │   ├── UpdatePlanAvailabilityService.java    # Isolated availability toggle
│   │   ├── GetGymTenantsService.java             # Dynamic JPA Specification tenant fetch
│   │   └── PlatformAdminProvisioningService.java # Idempotent YAML-driven admin seeder
│   ├── specifications/
│   │   ├── SaasPlanSpecification.java            # Composable Predicate builder for plans
│   │   └── GymTenantSpecification.java           # Composable Predicate builder for tenants
│   ├── request/
│   │   ├── CreateSaasPlanRequest.java            # Plan creation input DTO
│   │   ├── UpdatePlanAvailabilityRequest.java    # Availability toggle input DTO
│   │   ├── SaasPlanFilterRequest.java            # Dynamic filter & pagination DTO
│   │   └── GymTenantFilterRequest.java           # Dynamic tenant filter & pagination DTO
│   ├── response/
│   │   ├── CreateSaasPlanResponse.java           # Plan creation output DTO
│   │   ├── UpdatePlanAvailabilityResponse.java   # Availability toggle output DTO
│   │   ├── SaasPlanResponse.java                 # Structured plan retrieval DTO
│   │   ├── PagedResponse.java                    # Standardized paginated envelope (PROMPT.md Sec 4.5)
│   │   └── GymTenantSummaryResponse.java         # Tenant directory item DTO
│   ├── validations/
│   │   ├── PlatformAdminValidations.java         # Validation interface (sort whitelist & limits)
│   │   └── PlatformAdminValidationsImpl.java     # Precondition & invariant guards
│   └── mapper/
│       ├── SaasPlanMapper.java                   # Plan entity <-> DTO mapper interface
│       ├── SaasPlanMapperImpl.java               # Handwritten plan mapper implementation
│       ├── GymTenantSummaryMapper.java           # Tenant entity <-> DTO mapper interface
│       └── GymTenantSummaryMapperImpl.java       # Handwritten tenant mapper implementation
├── exceptions/                                   # Centralized exception handling
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   ├── ConflictException.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
├── config/
│   ├── SecurityCryptoConfig.java                 # BCryptPasswordEncoder bean
│   ├── PlatformAdminSeedProperties.java          # Configuration properties model (platformAdminSeed.yml)
│   └── PlatformAdminBootstrapConfig.java         # ApplicationRunner seeder hook

src/main/resources/
├── application.yaml                              # Imports platformAdminSeed.yml
└── platformAdminSeed.yml                         # Dedicated seed configuration file
```

### 3.2 Design Patterns Applied
1. **Dedicated Configuration Import & Property Binding**:
   - Seed configuration values reside in `src/main/resources/platformAdminSeed.yml` and are imported into `application.yaml`.
   - Bound cleanly via `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")` in `PlatformAdminSeedProperties`, strictly separating external configuration from database provisioning logic.
2. **Spring Data JPA Specifications (`JpaSpecificationExecutor<T>`)**:
   - Dynamic database queries for plans and tenants are composed using `Specification<T>` criteria, eliminating in-memory filtering and executing queries directly in MySQL with validated `Pageable`.
3. **Sort Whitelist Guard Pattern**:
   - Strict whitelisting in `PlatformAdminValidations` validates requested sort fields against approved entity properties, preventing property path traversal and SQL syntax exceptions.
4. **Concrete Operation-Specific Factories (ConnectSouq Reference Standard)**:
   - Each operation/mutation defines a dedicated, concrete factory (e.g. `CreateSaasPlanFactory`, `UpdatePlanAvailabilityFactory`) with strongly-typed request and response DTOs (`CreateSaasPlanRequest`, `CreateSaasPlanResponse`).
   - Rejects generic type parameters `<T, R>`, runtime discriminator mappings, and unchecked casts per `PROMPT.md` Section 3.1.
5. **Separated Validation Components (`PlatformAdminValidations`)**:
   - Domain invariants, uniqueness checks, and pagination bounds reside in dedicated validation components.
6. **Handwritten Mappers (`XxxMapper` + `XxxMapperImpl`)**:
   - Entity-to-DTO and DTO-to-entity mappings are handwritten classes implementing clear interfaces, adhering strictly to `RULES.md` Rule 4.
7. **Strict Constructor Injection**:
   - Every component utilizes constructor injection via Lombok `@RequiredArgsConstructor` or explicit constructors with `private final` fields.

---

## 4. Multi-Tenancy & Database Routing Alignment

The Platform Admin module operates **exclusively in the Central Platform Domain**:
1. All queries and mutations target `user_accounts`, `saas_subscription_plans`, and `gym_tenants` in the central MySQL database (`GymVerse`).
2. Persistence transactions execute strictly under `@Transactional("centralTransactionManager")`.
3. `TenantContext` is not modified by Platform Admin operations, allowing `GymVerseCurrentTenantIdentifierResolver` to automatically resolve `"central"`.
4. Tenant database connections and `TenantDataSourceCache` are never invoked, guaranteeing zero leakage into physical tenant databases.

---

## 5. Security & Ingress Dependencies

While authentication implementation is explicitly excluded from this epic:
1. **Target Authorization Role**: The Platform Admin endpoints will ultimately be protected by Spring Security filters verifying:
   - A valid Central Access Token (`tokenType = CENTRAL_USER`).
   - The verified claim `isPlatformAdmin = true` (or mapped authority `ROLE_PLATFORM_ADMIN`).
2. **Current Security Posture**:
   - The application has `spring-boot-starter-security` on the classpath.
   - For this epic, MockMvc unit and integration tests must run against controller layers using mock security contexts (`@WithMockUser` or test security configurations) without implementing the live JWT filter or login endpoint.
   - Live endpoints are designed so that adding security filters in the subsequent Auth Epic seamlessly guards them without modifying controller signatures.

---

## 6. Story Breakdown

| Story ID | Story Title | Primary Capability | Key Classes / Impact |
| :--- | :--- | :--- | :--- |
| **Story PA-1** | Initial Platform Administrator Provisioning Engine (YAML-Configured via `platformAdminSeed.yml`) | Dedicated YAML-bound idempotent bootstrap of initial platform admin | `platformAdminSeed.yml`, `PlatformAdminSeedProperties`, `PlatformAdminProvisioningService`, `SecurityCryptoConfig` |
| **Story PA-2** | SaaS Subscription Plan Creation | Administrative creation of platform subscription plans | `CreateSaasPlanService`, `SaasPlanMapper`, `PlatformAdminValidations` |
| **Story PA-3** | SaaS Subscription Plans Dynamic Query & Catalog Retrieval | Dynamic query, filtering, sorting, and pagination of plans via JPA Specifications | `SaasPlanSpecification`, `GetAllSaasPlansService`, `PagedResponse`, `SaasSubscriptionPlanRepository` |
| **Story PA-4** | Single SaaS Subscription Plan Retrieval by Identifier | Direct single-resource retrieval of plan details by ID | `GetSaasPlanByIdService`, `ResourceNotFoundException`, `SaasPlanMapper` |
| **Story PA-5** | SaaS Subscription Plan Availability Management | Isolated toggle of plan availability (`active`), guaranteeing price immutability | `UpdatePlanAvailabilityService`, `PlatformAdminValidations`, `SaasPlanMapper` |
| **Story PA-6** | Platform Registered Gym Tenants Dynamic Directory Retrieval | Dynamic query, filtering, sorting, and pagination of gym tenants via JPA Specifications | `GymTenantSpecification`, `GetGymTenantsService`, `PagedResponse`, `GymTenantRepository` |

---

## 7. Epic-Level Acceptance Criteria (Amended)

1. **AC-PA.1 (`platformAdminSeed.yml` Configuration & Idempotent Bootstrapping)**:
   - `src/main/resources/platformAdminSeed.yml` is imported into `application.yaml` via `spring.config.import`.
   - `PlatformAdminSeedProperties` binds all properties under prefix `gymverse.platform-admin.seed`.
   - The password placeholder `${PLATFORM_ADMIN_PASSWORD}` has no literal default fallback in version control.
   - Provisions the initial platform admin with verified BCrypt hash if none exists.
   - Repeated startups do not duplicate accounts or reset an existing administrator's password.
   - If the seed email belongs to a pre-existing non-admin account, seeding aborts without silent promotion.
   - Ordinary registrations never inherit platform-admin privileges.
2. **AC-PA.2 (Plan Lifecycle & Immutability Guard)**:
   - Platform admins can create new SaaS plans and toggle plan availability (`active = true/false`).
   - Toggling plan availability cannot alter prices, member/trainer limits, or billing cycles under any circumstance.
3. **AC-PA.3 (Standardized JPA Specification Fetching & Pagination)**:
   - Multi-record queries for SaaS plans and gym tenants utilize Spring Data JPA Specifications (`JpaSpecificationExecutor<T>`) for dynamic database query execution.
   - Filtering, sorting, and pagination operate together correctly at the database level.
   - Sort fields are strictly validated against entity-specific whitelists; invalid fields return HTTP `400 Bad Request`.
   - All fetch responses return standardized `PagedResponse<T>` conforming to `PROMPT.md` Section 4.5.
4. **AC-PA.4 (Dedicated Single-Resource Retrieval)**:
   - Single plan lookup by ID retains direct resource retrieval semantics returning `SaasPlanResponse` on success or standardized `404 Not Found` when absent.
5. **AC-PA.5 (Architectural & Verification Standard)**:
   - All newly created components reside strictly in `com.company.GymVerse.platformAdmin` and `com.company.GymVerse.exceptions`.
   - The 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% bit-for-bit unchanged.
   - Comprehensive automated unit and integration tests verify every operation against live MySQL central persistence.
