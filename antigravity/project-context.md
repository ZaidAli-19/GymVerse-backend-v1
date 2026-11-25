# GymVerse Backend v2 — Project Context & Execution Memory

> **Living Context File:** This document maintains the current status, architectural standards, progress tracking, and next actionable steps for the `GymVerse-Backend-v2` repository. It is maintained and referenced by AI engineering assistants to ensure seamless continuity across sessions.

---

## 1. Quick Status Card

| Metric | Current State |
| :--- | :--- |
| **Project Name** | `GymVerse-Backend-v2` |
| **Repository Root** | `D:\projects\GymVerse-Backend-v2` |
| **Current Git Branch** | `GymVerse/dev` |
| **Current Project Phase** | **Phase 5: In-Gym Tenant Administration Module (7-Page Operational Suite)** |
| **Current Phase Stage** | **Phase 5 Active Implementation (Story 5.4 Completed)** |
| **Last Completed Story** | **Story 5.4: Members Directory, Mini-Dashboard & Member Details View** |
| **Immediate Next Phase / Story** | **Story 5.5: Trainers & Staff Summary & Searchable Address Book** |
| **Test Suite Health** | **Phase 5 `tenantAdmin` Unit & MySQL Integration Verification: 40/40 Passing (0 Failures, 0 Errors)** |
| **Protected Code Status** | All 8 classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched |

---

## 2. Executive Summary & System Architecture

### 2.1 Project Purpose
`GymVerse-Backend-v2` is a multi-tenant SaaS backend platform for fitness clubs and gym management. The platform supports:
1. **Platform Administration (Super Admin)**: Plan catalog management, tenant directory & oversight, platform-level configurations, and centralized routing.
2. **Multi-Tenant Operations**: Isolated tenant databases per gym franchise/center for data privacy, compliance, and scalable tenant isolation.
3. **Tenant Administration & Member Operations**: Gym-specific operations (staff management, member subscriptions, class scheduling, attendance, billing).

### 2.2 Core Architectural Invariants (Non-Negotiable)

1. **Strict Git Commit & Push Policy (`RULES.md` Rule 1)**:
   - **NEVER** run `git commit` or `git push` autonomously.
   - Git commits and pushes must **ONLY** be executed when the user explicitly commands it in chat.
   - All changes must remain unstaged or staged locally for review.

2. **Unified Single-Endpoint Controller Gateway (`RULES.md` Rule 4 & `PROMPT.md`)**:
   - Every feature module controller must expose **strictly ONE unified endpoint**: `@PostMapping("/api/<module-route>")` (e.g., `@PostMapping("/api/platform-admin")`).
   - **All operations without exception**—both write mutations (`CREATE`, `UPDATE`, `DELETE`) and read queries (`FETCH_ALL`, `FETCH_BY_ID`, `SEARCH`)—must route through this single `POST` endpoint using the `<Module>RequestType` discriminator enum.
   - **NEVER** add ad-hoc controller endpoints, HTTP GET endpoints, or sub-path routes (e.g., no `@GetMapping`, `@PutMapping`, `@DeleteMapping`, or `@PostMapping("/sub-path")`).

3. **Module-Level Factory Pattern**:
   - One factory per module (`PlatformAdminFactory`) dispatching to concrete operation beans implementing `PlatformAdminFactoryOperation`.
   - Shared `<Module>Request` and `<Module>Response` with nested DTOs for operation-specific payloads.
   - Strictly no operation-specific factories, no generic `<T, R>` registries, and no unchecked casts.

4. **Multi-Tenancy Isolation & Protected Classes**:
   - Central database (`GymVerse`) manages platform-level records: `gym_tenants`, `saas_subscription_plans`, `platform_admin`.
   - Tenant databases (`gymverse_tenant_<gym_code>`) manage tenant-scoped data.
   - All 8 protected classes in `com.company.GymVerse.multitanancy` must remain 100% untouched:
     - `TenantContext`, `TenantDataSourceConfig`, `TenantRoutingDataSource`, `TenantConnectionProvider`, `CurrentTenantIdentifierResolverImpl`, `MultiTenantConstants`, `TenantDatabaseMigrationService`, `TenantResolutionFilter`.

5. **Transaction Management & Routing Invariant**:
   - Platform Admin operations run exclusively on `centralTransactionManager` (read-only where applicable).
   - Platform Admin services must **never** query tenant databases or touch `TenantContext`.

6. **Handwritten Mappers & Dedicated Validations**:
   - All mappers use handwritten implementations (`XxxMapper` interface + `XxxMapperImpl`). No MapStruct or reflection libraries.
   - Domain validations and authorization checks reside in dedicated validation components (`XxxValidations` + `XxxValidationsImpl`).

7. **Documentation & Task Logging Standards**:
   - Dedicated task log in `antigravity/<task-descriptive-name>.md` for every task completed.
   - Maintain API payload specifications in `docs/payloads.md` with 13 mandatory contract fields and cURL examples.

---

## 3. Phased Roadmap & Progress Tracking

```
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: DATABASE SCHEMA & MULTI-TENANCY BASELINE [✅ 100% COMPLETED]  │
│ - Story 1.1: DB Configuration & Test Environment Profiles              │
│ - Story 1.2: Central Database Flyway Migration (V1 init schema)        │
│ - Story 1.3: Central JPA Entities & Repositories                       │
│ - Story 1.4: Tenant Database Flyway Migration (V1 init tenant schema)  │
│ - Story 1.5: Tenant JPA Entities & Multi-tenant Routing Engine         │
│ - Story 1.6: Multitenancy Bridge Integration & Tenant Routing Checks   │
│ - Story 1.7: Comprehensive Integration Test Suite                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 2: PLATFORM ADMIN MODULE [✅ 100% COMPLETED]                     │
│ - Story PA-1: Platform Administrator Provisioning (platformAdminSeed)  │ [✅ DONE]
│ - Story PA-2: SaaS Subscription Plan Creation (CREATE_SAAS_PLAN)       │ [✅ DONE]
│ - Story PA-3: SaaS Subscription Plans Catalog (FETCH_ALL_SAAS_PLANS)   │ [✅ DONE]
│ - Story PA-4: Single SaaS Plan Retrieval (FETCH_SAAS_PLAN_BY_ID)       │ [✅ DONE]
│ - Story PA-5: Plan Availability Management (UPDATE_PLAN_AVAILABILITY)  │ [✅ DONE]
│ - Story PA-6: Gym Tenants Directory (FETCH_ALL_GYM_TENANTS)            │ [✅ DONE]
│ - Story PA-7: Request DTO Consolidation & Pagination Standardization   │ [✅ DONE]
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 3: CORE SECURITY & MASTER DB AUTHENTICATION [✅ 100% COMPLETED]   │
│ - Security Baseline: JWT Util, Filter Chain, Security Dependencies     │ [✅ DONE]
│ - Top-Layer Master User Lifecycle (Central DB):                        │
│   • Signup: Full Name, Email, Phone -> Email OTP -> Password Creation   │ [✅ DONE]
│   • Login: Dual mode (Email + Password OR Email OTP)                   │ [✅ DONE]
│   • Password Recovery: Forgot Password via Email OTP                   │ [✅ DONE]
│ - Unified Single-Endpoint Controller Gateway (/api/auth) & API Docs    │ [✅ DONE]
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 4: TENANT ONBOARDING, PROVISIONING & RBAC [✅ 100% COMPLETED]    │
│ - Gym Tenant Database Auto-Provisioning & Dynamic Flyway Migration     │ [✅ DONE]
│ - Tenant Association & Directory (Fetch My Tenants, Switch Tenant)     │ [✅ DONE]
│ - Scoped RBAC across Platform Admin, Central User & Tenant Admin Gateways│ [✅ DONE]
│ - Subscription Insights & Entitlements Management (/api/tenant-admin)   │ [✅ DONE]
│ - Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine          │ [✅ DONE]
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Phase Story Breakdown & Implementation Roadmap

### 4.1 Phase 3 Story Status (Completed)
| Story ID | Description | Core Artifacts | Status |
| :--- | :--- | :--- | :--- |
| **Story 3.1** | Security Baseline Configuration, Password Hashing & CustomUserDetailsService | `SecurityConfig.java`, `CustomUserDetailsService.java` | ✅ Completed |
| **Story 3.2** | Modernized JJWT Token Engine (JwtUtil & JwtFilter with MASTER_ACCOUNT Token Typing) | `JwtUtil.java`, `JwtFilter.java` | ✅ Completed |
| **Story 3.3** | Kafka Asynchronous Email Pipeline & JavaMailSender Integration | `OtpEmailEvent.java`, `OtpEmailListener.java`, `EmailServiceImpl.java` | ✅ Completed |
| **Story 3.4** | Enterprise Redis OtpService with Rate Limiting & Cooldown Engine | `OtpConfigProperties.java`, `OtpService.java`, `OtpServiceImpl.java` | ✅ Completed |
| **Story 3.5** | Master User 3-Step Signup Lifecycle (SIGNUP_INITIATE, SIGNUP_VERIFY, SIGNUP_COMPLETE) | `SignupInitiateService`, `SignupVerifyService`, `SignupCompleteService`, `AuthValidations` | ✅ Completed |
| **Story 3.6** | Dual-Mode Master Authentication Engine (LOGIN_PASSWORD, LOGIN_OTP_INITIATE, LOGIN_OTP_VERIFY, REFRESH_TOKEN) | `LoginPasswordService`, `LoginOtpInitiateService`, `LoginOtpVerifyService`, `RefreshTokenService` | ✅ Completed |
| **Story 3.7** | Master User Password Recovery Lifecycle (FORGOT_PASSWORD_INITIATE, FORGOT_PASSWORD_VERIFY, FORGOT_PASSWORD_RESET) | `ForgotPasswordInitiateService`, `ForgotPasswordVerifyService`, `ForgotPasswordResetService` | ✅ Completed |
| **Story 3.8** | Unified Controller Gateway Integration (/api/auth), Exception Translation & API Payload Docs | `AuthController.java`, `AuthMapper.java`, `AuthMapperImpl.java`, `AuthUnifiedGatewayIntegrationTest.java`, `docs/payloads.md` | ✅ Completed |

### 4.2 Phase 4 Story Status (Extended)
| Story ID | Description | Core Artifacts | Status |
| :--- | :--- | :--- | :--- |
| **Story 4.1** | SaaS Trial Engine Configuration & Properties | `TenantConfigProperties.java`, `SubscriptionDateUtil.java`, `application.yaml` | ✅ Completed |
| **Story 4.2** | Tenant-Scoped JWT Engine & Security Context Resolution | `JwtUtil.java`, `JwtFilter.java`, `TenantTokenClaims.java` | ✅ Completed |
| **Story 4.3** | Dynamic Tenant Provisioning & Schema Migration Service | `TenantProvisioningService.java`, `TenantProvisioningCommand.java`, `TenantProvisioningResult.java`, compensation rollback | ✅ Completed |
| **Story 4.4** | Gym Registration Gateway (REGISTER_GYM) | `RegisterGymService.java`, `TenantProvisioningService.java`, platform guard | ✅ Superseded by 4.4R |
| **Story 4.4R** | Central User Module Gateway Architecture & Onboarding Refactoring | `UserController.java` (`/api/user`), `UserFactory.java`, `UserValidations.java`, `RegisterGymService.java` | ✅ Completed |
| **Story 4.5** | User Tenants Directory & Token Exchange Gateway | `FetchMyTenantsService.java` (`/api/user`), `SwitchTenantService.java` (`/api/auth`), `AuthMapper`, `UserMapper` | ✅ Completed |
| **Story 4.6** | Subscription Insights & Entitlements Management | `TenantAdminController.java` (`/api/tenant-admin`), `FetchTenantSubscriptionService.java`, `TenantAdminMapper` | ✅ Completed |
| **Story 4.7** | Default SaaS Trial Plan Seeding & Self-Bootstrapping Engine | `DefaultSaasPlanSeeder.java`, `TenantConfigProperties.java`, `TenantProvisioningService.java` | ✅ Completed |
| **Story 4.8** | Member Gym Discovery Gateway (FETCH_GYM_BY_CODE) | `FetchGymByCodeService.java` (`/api/user`), `UserHelper`, `UserValidations`, `docs/payloads.md` | ✅ Completed |
| **Story 4.9** | Member Join Request Gateway (SUBMIT_JOIN_REQUEST) | `SubmitJoinRequestService.java` (`/api/user`), `TenantRoutingBridge`, `join_requests`, `docs/payloads.md` | ✅ Completed |

### 4.3 Phase 5 Story Status (Tenant Admin Module)
| Story ID | Description | Core Artifacts | Status |
| :--- | :--- | :--- | :--- |
| **Story 5.1** | Phase 5 Schema (`V2`), Reusable Attendance Port & Subfolder Core Infrastructure | `V2__phase5_tenant_admin_extensions.sql`, `PlanTier`, `GymFacility`, `Trainer`, `AttendanceQueryPort`, `PlanTheme`, `helper/`, `subscription/` subfolder refactor | ✅ Completed |
| **Story 5.2** | Plans Management Suite | `service/plans/`, `MembershipPlanSpecification`, `PlanHelper`, `CREATE_PLAN`, `FETCH_PLANS`, `FETCH_PLANS_SUMMARY`, `FETCH_PLAN_FILTER_OPTIONS`, `UPDATE_PLAN_AVAILABILITY` | ✅ Completed |
| **Story 5.3** | Join Requests Review & Approval Workflow | `service/members/`, `JoinRequestSpecification`, `JoinRequestHelper`, `FETCH_JOIN_REQUESTS`, `APPROVE_JOIN_REQUEST`, `REJECT_JOIN_REQUEST` | ✅ Completed |
| **Story 5.4** | Members Directory, Mini-Dashboard & Member Details View | `service/members/`, `MemberSpecification`, `FETCH_MEMBERS_SUMMARY`, `FETCH_MEMBERS`, `FETCH_MEMBER_DETAILS`, `docs/payloads.md` | ✅ Completed |
| **Story 5.5** | Trainers & Staff Summary & Address Book | `service/trainers/`, `TrainerSpecification`, `FETCH_TRAINERS_SUMMARY`, `FETCH_TRAINERS` | ⏳ Next |
| **Story 5.6** | Finance Analytics Suite & Expense Ledger | `service/finance/`, `ExpenseSpecification`, `FETCH_FINANCE_OVERVIEW`, `FETCH_EXPENSE_TRANSACTIONS`, `CREATE_EXPENSE` | ⏳ Planned |
| **Story 5.7** | Admin Dashboard, Live Roster Gateway & API Docs | `service/dashboard/`, `service/liveRoster/`, `FETCH_ADMIN_DASHBOARD`, `FETCH_LIVE_ROSTER`, `docs/payloads.md` | ⏳ Planned |

---

## 5. Scope & Target Artifacts for Immediate Next Work: Story 5.5 (Trainers & Staff Summary & Searchable Address Book)

- **Active Stage:** `Phase 5 Implementation — Story 5.5`
- **Next Story:** [`story-5.5-trainers-summary-and-address-book.md`](file:///D:/projects/GymVerse-Backend-v2/_bmad-output/planning-artifacts/phase-5-tenant-admin/story-5.5-trainers-summary-and-address-book.md)
- **Target Operations (`/api/tenant-admin`):**
  1. `FETCH_TRAINERS_SUMMARY` (`totalTrainersCount` via `TrainerRepository`, `activeLiveCount` & `avgAttendanceRate` via `AttendanceQueryPort` with `// TODO [ATTENDANCE-MODULE]`).
  2. `FETCH_TRAINERS` (`TrainerSpecification` + pagination + whitelisted sorting (`id`, `joinedDate`, `specialization`, `user.firstName`) + `active` / `specialization` / `search` filters).
- **Invariants:**
  - Add Trainer onboarding remains deferred to a future phase.
  - Subfolder classification under `service/trainers/`, `request/trainers/`, `response/trainers/`, `specifications/trainers/`.
  - Live attendance calls strictly via `AttendanceQueryPort` with `// TODO [ATTENDANCE-MODULE]` hooks.
  - Multi-tenancy core untouched (8 protected classes); no autonomous git commit/push (`RULES.md` Rule 1).

---

## 6. Context Maintenance Protocol for AI Assistants

Whenever completing or transitioning tasks, the AI assistant must:
1. **Update Section 1 (Quick Status Card)**:
   - Advance the current phase/stage, last completed story, and immediate next story.
   - Update test pass count and git commit hash if changes were committed upon user request.
2. **Update Section 4 (Phase Story Breakdown)**:
   - Mark completed stories as `✅ Completed` with brief summary of verified artifacts.
3. **Log Completed Work in `antigravity/`**:
   - Write a dedicated task log file `antigravity/<story-name>.md` detailing changes, architectural decisions, and verification proofs.
4. **Update `docs/payloads.md`**:
   - Ensure all affected endpoints have updated request/response contracts and valid cURL snippets.
5. **Report to User**:
   - Summarize changes concisely, cite verification output, and present clear next steps without executing unauthorized git commits or pushes.
