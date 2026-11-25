---
id: "epic-phase-5-tenant-admin"
title: "Phase 5: In-Gym Tenant Administration Module (7-Page Operational Suite)"
status: "approved-for-story-breakdown"
phase: "Phase 5 / Tenant Administration & Core Gym Operations"
created_at: "2026-11-25"
author: "Antigravity AI Agent"
documents_referenced:
  - "PROMPT.md"
  - "RULES.md"
  - "antigravity/project-context.md"
  - "src/main/resources/db/migration/tenant/V1__init_tenant_schema.sql"
  - "src/main/resources/db/migration/central/V1__init_central_schema.sql"
  - "src/main/java/com/company/GymVerse/tenantAdmin/controller/TenantAdminController.java"
---

# Epic: Phase 5 — In-Gym Tenant Administration Module

## 1. Executive Summary & Business Objectives

With **Phase 4 (Tenant Onboarding, Dynamic Provisioning & Scoped RBAC)** completed, gym owners can register gyms (`REGISTER_GYM`), switch into tenant-scoped tokens (`SWITCH_TENANT`), and inspect their SaaS subscription (`FETCH_TENANT_SUBSCRIPTION`), while prospective members can discover gyms (`FETCH_GYM_BY_CODE`) and submit join applications (`SUBMIT_JOIN_REQUEST`).

**Phase 5** builds the **In-Gym Tenant Administration Module** under `/api/tenant-admin` (`com.company.GymVerse.tenantAdmin`) powering the 7-page Tenant Admin application:

| # | Navigation Page | Core Capabilities | Phase 5 Scope |
| :--- | :--- | :--- | :--- |
| **1** | **Dashboard** | KPI sneak peek (total members, live members, total revenue, active rate), active tier breakdown, membership count card by tier, revenue breakdown card (new memberships / renewals / others), expiring-soon & live expiring cards. | **In Scope** *(Live counts stubbed to `0` with commented attendance hooks)* |
| **2** | **Plans** | Plans mini-dashboard (active count + tier breakdown), paginated/filtered plans list (All, by Tier, Offers/Discounts), dynamic filter-options retrieval, Add Plan with default tiers (`ELITE` -> `GOLDEN`, `LEGENDARY` -> `PURPLE`) + Custom Tiers, custom facility management, and 20 canonical custom themes. | **In Scope** |
| **3** | **Members** | Members mini-dashboard (total, live, expired, trial), paginated/filtered member directory (All, Live, Expired, Trial), rich Member Details view (visits, streak, amount paid, days left, daily attendance card, membership details, contact details), and **Join Requests** tab (search/filter/review/mark `PAID` or `PARTIALLY_PAID`/approve/reject). | **In Scope** *(Live & attendance fields stubbed with commented hooks; Manual OTP Member Onboarding deferred)* |
| **4** | **Trainers / Staff** | Trainers mini-dashboard (total trainers, active live count, avg attendance rate) and searchable/paginated trainer address book. | **In Scope** *(Live & avg attendance stubbed with commented hooks; Add Trainer flow deferred)* |
| **5** | **Finance** | Period toggle (`WEEKLY` / `MONTHLY` / `YEARLY`), KPI summary (total revenue, total expense, net profit, budget used % of revenue), Revenue/Expense/Profit overview, Expense category breakdown, Collection mode breakdown (`CASH` vs `ONLINE`), Revenue source breakdown (`NEW_MEMBERSHIP` / `RENEWAL` / `OTHER`), paginated expense transactions, and Add Expense (with default categories + custom notes). | **In Scope** |
| **6** | **Live Roster** | Searchable/paginated address book of members currently inside the gym, live summary counts (total live, avg time spent, live expired count, live trial count). | **DTOs, Service & Commented Hooks In Scope** *(Returns `0` / empty until Attendance Module arrives)* |
| **7** | **Settings** | Gym configuration, geofence, hours, preferences. | **Deferred (Future Scope)** |

### Explicitly Deferred Flows (Confirmed for Future Phases)
1. **Manual Walk-In Member Onboarding (Email OTP + Registration):** Deferred to future implementation.
2. **Add Trainer Flow:** Deferred to future implementation.
3. **Live Attendance & Geofencing Engine:** Arrives with the Tenant Member module; Phase 5 establishes the reusable `AttendanceQueryPort` boundary with `// TODO [ATTENDANCE-MODULE]` hooks returning `0` / empty cleanly.
4. **Settings Page:** Deferred to future implementation.

---

## 2. Core Architectural Invariants & Package Subfolder Standard

All Phase 5 implementation must strictly comply with:

1. **Unified Single-Endpoint Gateway (`RULES.md` Rule 4 & `PROMPT.md`)**:
   - All tenant admin operations dispatch exclusively through `@PostMapping("/api/tenant-admin")` in [`TenantAdminController`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/controller/TenantAdminController.java) using `TenantAdminRequestType`.
   - **Zero** ad-hoc `@GetMapping`, `@PutMapping`, `@DeleteMapping`, or sub-path routes.
2. **Strict Subfolder Classification inside `com.company.GymVerse.tenantAdmin`**:
   - Every page's services, request DTOs, response DTOs, and JPA Specifications must be organized inside page-specific subfolders:
     ```
     src/main/java/com/company/GymVerse/tenantAdmin/
     ├── controller/
     │   └── TenantAdminController.java
     ├── factory/
     │   ├── TenantAdminFactory.java
     │   └── TenantAdminOperation.java
     ├── helper/
     │   ├── TenantAdminContextHelper.java
     │   ├── MembershipCalculationHelper.java
     │   └── FinancePeriodHelper.java
     ├── mapper/
     │   ├── TenantAdminMapper.java
     │   └── TenantAdminMapperImpl.java
     ├── validations/
     │   ├── TenantAdminValidations.java
     │   └── TenantAdminValidationsImpl.java
     ├── specifications/
     │   ├── plans/MembershipPlanSpecification.java
     │   ├── members/{MemberSpecification.java, JoinRequestSpecification.java}
     │   ├── trainers/TrainerSpecification.java
     │   └── finance/ExpenseSpecification.java
     ├── request/
     │   ├── TenantAdminRequest.java
     │   ├── enums/
     │   │   ├── TenantAdminRequestType.java
     │   │   ├── PlanTheme.java
     │   │   ├── FinancePeriodType.java
     │   │   └── MemberFilterStatus.java
     │   ├── subscription/FetchTenantSubscriptionRequest.java
     │   ├── dashboard/FetchAdminDashboardRequest.java
     │   ├── plans/{FetchPlansRequest.java, CreatePlanRequest.java, UpdatePlanAvailabilityRequest.java}
     │   ├── members/{FetchMembersRequest.java, FetchMemberDetailsRequest.java, FetchJoinRequestsRequest.java, ApproveJoinRequest.java, RejectJoinRequest.java}
     │   ├── trainers/FetchTrainersRequest.java
     │   ├── finance/{FetchFinanceOverviewRequest.java, FetchExpenseTransactionsRequest.java, CreateExpenseRequest.java}
     │   └── liveRoster/FetchLiveRosterRequest.java
     ├── response/
     │   ├── TenantAdminResponse.java
     │   ├── subscription/...
     │   ├── dashboard/...
     │   ├── plans/...
     │   ├── members/...
     │   ├── trainers/...
     │   ├── finance/...
     │   └── liveRoster/...
     └── service/
         ├── subscription/FetchTenantSubscriptionService.java
         ├── dashboard/FetchAdminDashboardService.java
         ├── plans/{FetchPlansSummaryService.java, FetchPlanFilterOptionsService.java, FetchPlansService.java, CreatePlanService.java, UpdatePlanAvailabilityService.java}
         ├── members/{FetchMembersSummaryService.java, FetchMembersService.java, FetchMemberDetailsService.java, FetchJoinRequestsService.java, ApproveJoinRequestService.java, RejectJoinRequestService.java}
         ├── trainers/{FetchTrainersSummaryService.java, FetchTrainersService.java}
         ├── finance/{FetchFinanceOverviewService.java, FetchExpenseTransactionsService.java, CreateExpenseService.java}
         └── liveRoster/FetchLiveRosterService.java
     ```
3. **Orchestrator + Helper + Validation + Handwritten Mapper Discipline**:
   - **Services** act strictly as clean, linear orchestrators (shallow nesting, no multi-level nested loops or messy `if/else` chains).
   - **Validations** (`TenantAdminValidations` / `TenantAdminValidationsImpl`) own RBAC, request validation, range checks, and state invariant guards.
   - **Helpers** (`tenantAdmin/helper/`) encapsulate reusable computations (date-range calculation, tier/theme resolution, membership pricing/expiry/trial status math, central context extraction).
   - **Handwritten Mappers** (`TenantAdminMapper` / `TenantAdminMapperImpl`) — zero MapStruct or reflection.
4. **Pagination, Sorting & JPA Specifications Everywhere**:
   - All fetch/list APIs (`FETCH_PLANS`, `FETCH_MEMBERS`, `FETCH_JOIN_REQUESTS`, `FETCH_TRAINERS`, `FETCH_EXPENSE_TRANSACTIONS`, `FETCH_LIVE_ROSTER`) must support pagination (`page`, `size`), sorting (`sortBy`, `sortDirection`), and dynamic filtering via **Spring Data JPA `Specification<T>`** (`JpaSpecificationExecutor<T>`), returning `PagedResponse<T>`.
5. **Reusable Attendance Module Boundary**:
   - Live attendance tracking is visible across **all roles** (not solely Admin).
   - Define `com.company.GymVerse.attendance.service.AttendanceQueryPort` + `AttendanceQueryStubImpl` + DTOs (`LiveAttendanceSummary`, `MemberAttendanceSnapshot`, `LiveRosterEntry`, `TrainerAttendanceSummary`) outside `tenantAdmin`, with explicit `// TODO [ATTENDANCE-MODULE]` comments at every call site, cleanly returning `0` / empty / `null` until the live attendance feature arrives.
6. **Protected Multitenancy Core & Git Policy**:
   - All classes in `com.company.GymVerse.multitanancy` remain **100% byte-for-byte untouched**.
   - **NEVER** execute `git commit` or `git push` autonomously (`RULES.md` Rule 1).
   - Every `/api/tenant-admin` operation requires a `TENANT_ACCOUNT` Bearer token with `ROLE_TENANT_ADMIN`.

---

## 3. Tenant Database Schema Evolution (`V2__phase5_tenant_admin_extensions.sql`)

| Table / Area | `V2` Migration Changes | Purpose |
| :--- | :--- | :--- |
| **`plan_tiers`** *(New)* | `id`, `name` (`VARCHAR(50)` unique), `theme_key` (`VARCHAR(50)`), `is_default` (`BOOLEAN`), `created_at` (`DATETIME(6)`). Seed `ELITE` (`GOLDEN`, `is_default=TRUE`) & `LEGENDARY` (`PURPLE`, `is_default=TRUE`). | Persists default and admin-created custom tiers with deterministic theme keys. |
| **`gym_facilities`** *(New)* | `id`, `name` (`VARCHAR(100)` unique), `created_at` (`DATETIME(6)`). | Persists admin-added reusable facilities for plans & filters. |
| **`membership_plans`** *(Alter)* | Drop `tier` and `facility`, add `plan_tier_id BIGINT NULL` FK -> `plan_tiers(id)` and `is_trial BOOLEAN NOT NULL DEFAULT FALSE`. | Enforces relational tier/theme integrity and supports trial-flagged plans. |
| **`membership_plan_facilities`** *(New)* | Composite PK `(plan_id, facility_id)` with FKs -> `membership_plans(id)` and `gym_facilities(id)`. | Links each membership plan to one or more reusable facilities. |
| **`member_memberships`** *(Alter)* | Add `paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00`, `payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID'`, `is_renewal BOOLEAN NOT NULL DEFAULT FALSE`, `is_trial BOOLEAN NOT NULL DEFAULT FALSE`. | Supports `PAID` / `PARTIALLY_PAID` tracking, renewal classification, and flag-based trial classification alongside `members.trial_end_date`. |
| **`revenue`** *(Alter)* | Make `member_membership_id` nullable (`BIGINT NULL`), drop `UNIQUE` constraint `uq_rev_membership` (re-create standard FK index), add `revenue_type VARCHAR(30) NOT NULL DEFAULT 'NEW_MEMBERSHIP'`, `notes TEXT NULL`. | Supports `NEW_MEMBERSHIP`, `RENEWAL`, and `OTHER` revenue breakdowns plus partial/multi-payment records. |
| **`trainers`** *(New)* | `id`, `user_id` (`BIGINT` unique FK -> `users(id)`), `specialization` (`VARCHAR(150)`), `experience_years` (`INT`), `bio` (`TEXT`), `joined_date` (`DATE`), `active` (`BOOLEAN`). | Clean 1:1 trainer profile for the searchable Trainers address book & summary. |
| **`expenses`** *(Index)* | Add composite index `idx_exp_category_date (category, expense_date)`. | Accelerates Finance weekly/monthly/yearly category aggregations. |

---

## 4. Approved Story Breakdown (Stories 5.1 – 5.7)

| Story ID | Story Title | Scope & Core Deliverables |
| :--- | :--- | :--- |
| **Story 5.1** | **Phase 5 Schema (`V2`), Reusable Attendance Port & Subfolder Core Infrastructure** | `V2__phase5_tenant_admin_extensions.sql`, new/updated tenant entities & repositories (`JpaSpecificationExecutor`), `PlanTheme` (2 defaults + 20 custom), domain enums, `AttendanceQueryPort` + stub, subfolder reorganization of existing `tenantAdmin` (`service/subscription`, `request/subscription`, `response/subscription`), `helper/` utilities, `expiring-soon-days: 7` config. |
| **Story 5.2** | **Plans Management Suite** | `FETCH_PLANS_SUMMARY`, `FETCH_PLAN_FILTER_OPTIONS`, `FETCH_PLANS` (JPA Spec + pagination), `CREATE_PLAN` (default/custom tiers + 20 custom themes + persisted facilities), `UPDATE_PLAN_AVAILABILITY`. |
| **Story 5.3** | **Join Requests Review & Approval Workflow** | `FETCH_JOIN_REQUESTS` (JPA Spec + pagination + search/status filter), `APPROVE_JOIN_REQUEST` (`PAID`/`PARTIALLY_PAID` + `paidAmount` -> Tenant `TenantUser`, `Member`, `MemberMembership`, `Revenue` + Central `UserTenantRole` + `max_members` quota check), `REJECT_JOIN_REQUEST`. |
| **Story 5.4** | **Members Directory, Mini-Dashboard & Member Details View** | `FETCH_MEMBERS_SUMMARY`, `FETCH_MEMBERS` (JPA Spec + `ALL`/`LIVE`/`EXPIRED`/`TRIAL` supporting both `trialEndDate` & `isTrial` flag + pagination), `FETCH_MEMBER_DETAILS` (commented attendance card + financial/expiry/membership/contact cards). |
| **Story 5.5** | **Trainers & Staff Summary & Address Book** | `FETCH_TRAINERS_SUMMARY` (`totalTrainersCount`, commented `activeLiveCount` & `avgAttendanceRate`), `FETCH_TRAINERS` (JPA Spec searchable/paginated address book). |
| **Story 5.6** | **Finance Analytics Suite & Expense Ledger** | `FETCH_FINANCE_OVERVIEW` (`WEEKLY`/`MONTHLY`/`YEARLY` toggle, KPIs, budget used %, expense/collection/revenue breakdowns), `FETCH_EXPENSE_TRANSACTIONS` (JPA Spec + pagination), `CREATE_EXPENSE`. |
| **Story 5.7** | **Admin Dashboard, Live Roster Gateway & API Docs** | `FETCH_ADMIN_DASHBOARD` (KPIs, tier breakdowns, revenue card, expiring-soon card + commented live hooks), `FETCH_LIVE_ROSTER` (DTOs, JPA Spec contract, `AttendanceQueryPort` stub returning `0` & empty page), `docs/payloads.md` updates. |

---

*Phase 5 Epic updated and approved for Story Breakdown (`story-5.1` to `story-5.7`).*
