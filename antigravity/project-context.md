# GymVerse Backend v2 — Project Context & Execution Memory

> **Living Context File:** This document maintains the current status, architectural standards, progress tracking, and next actionable steps for the `GymVerse-Backend-v2` repository. It is maintained and referenced by AI engineering assistants to ensure seamless continuity across sessions.

---

## 1. Quick Status Card

| Metric | Current State |
| :--- | :--- |
| **Project Name** | `GymVerse-Backend-v2` |
| **Repository Root** | `D:\projects\GymVerse-Backend-v2` |
| **Current Git Branch** | `GymVerse/dev` |
| **Current Project Phase** | **Phase 3: Core Security & Master DB Authentication (Active)** |
| **Current Phase Stage** | **Story 3.3 Implemented & Verified; Ready for Story 3.4** |
| **Last Completed Story** | **Story 3.3** (Kafka Asynchronous Email Pipeline & JavaMailSender Integration) |
| **Immediate Next Phase / Story** | **Story 3.4: Enterprise Redis OtpService with Rate Limiting & Cooldown Engine** |
| **Test Suite Health** | **96 Tests Passing (0 Failures, 0 Errors, 0 Skipped)** |
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
│ PHASE 3: CORE SECURITY & MASTER DB AUTHENTICATION [⏳ PLANNING]        │
│ - Security Baseline: JWT Util, Filter Chain, Security Dependencies     │
│ - Top-Layer Master User Lifecycle (Central DB):                        │
│   • Signup: Full Name, Email, Phone -> Email OTP -> Password Creation   │
│   • Login: Dual mode (Email + Password OR Email OTP)                   │
│   • Password Recovery: Forgot Password via Email OTP                   │
│ - Reference project alignment & structural integration                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 4: TENANT ONBOARDING, PROVISIONING & RBAC [PLANNED]              │
│ - Gym Tenant Database Auto-Provisioning & Dynamic Flyway Migration     │
│ - Tenant Association (Tenant Owner / Admin / Member mapping)           │
│ - RBAC across Platform Admin & Tenant Endpoints                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Phase 3 Story Breakdown & Implementation Roadmap

| Story ID | Description | Core Artifacts | Status |
| :--- | :--- | :--- | :--- |
| **Story 3.1** | Security Baseline Configuration, Password Hashing & CustomUserDetailsService | `SecurityConfig.java`, `CustomUserDetailsService.java` | ✅ Completed |
| **Story 3.2** | Modernized JJWT Token Engine (JwtUtil & JwtFilter with MASTER_ACCOUNT Token Typing) | `JwtUtil.java`, `JwtFilter.java` | ✅ Completed |
| **Story 3.3** | Kafka Asynchronous Email Pipeline & JavaMailSender Integration | `OtpEmailEvent.java`, `OtpEmailListener.java`, `EmailServiceImpl.java` | ✅ Completed |
| **Story 3.4** | Enterprise Redis OtpService with Rate Limiting & Cooldown Engine | `OtpConfigProperties.java`, `OtpService.java`, `OtpServiceImpl.java` | ⏳ Ready for Implementation |
| **Story 3.5** | Master User 3-Step Signup Lifecycle (SIGNUP_INITIATE, SIGNUP_VERIFY, SIGNUP_COMPLETE) | `SignupInitiateService`, `SignupVerifyService`, `SignupCompleteService`, `AuthValidations` | ⏳ Planned |
| **Story 3.6** | Dual-Mode Master Authentication Engine (LOGIN_PASSWORD, LOGIN_OTP_INITIATE, LOGIN_OTP_VERIFY, REFRESH_TOKEN) | `LoginPasswordService`, `LoginOtpInitiateService`, `LoginOtpVerifyService`, `RefreshTokenService` | ⏳ Planned |
| **Story 3.7** | Master User Password Recovery Lifecycle (FORGOT_PASSWORD_INITIATE, FORGOT_PASSWORD_VERIFY, FORGOT_PASSWORD_RESET) | `ForgotPasswordInitiateService`, `ForgotPasswordVerifyService`, `ForgotPasswordResetService` | ⏳ Planned |
| **Story 3.8** | Unified Controller Gateway Integration (/api/auth), Exception Translation & API Payload Docs | `AuthController.java`, `AuthFactory.java`, `GlobalExceptionHandler.java`, `docs/payloads.md` | ⏳ Planned |

---

## 5. Scope & Target Artifacts for Immediate Next Work: Story 3.4

- **Story Focus:** `Story 3.4: Enterprise Redis OtpService with Rate Limiting & Cooldown Engine`
- **Target Files to Modify / Create:**
  - `src/main/java/com/company/GymVerse/config/OtpConfigProperties.java` (@ConfigurationProperties for TTL, cooldown, and max attempts)
  - `src/main/java/com/company/GymVerse/exceptions/otp/OtpRateLimitException.java`
  - `src/main/java/com/company/GymVerse/exceptions/otp/InvalidOtpException.java`
  - `src/main/java/com/company/GymVerse/exceptions/otp/OtpExpiredException.java`
  - `src/main/java/com/company/GymVerse/exceptions/otp/OtpMaxAttemptsExceededException.java`
  - `src/main/java/com/company/GymVerse/security/otp/service/OtpService.java`
  - `src/main/java/com/company/GymVerse/security/otp/serviceImpl/OtpServiceImpl.java`
  - Unit and integration tests for Redis OTP generation, cooldown throttling, attempt limiting, and verification token generation.

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
