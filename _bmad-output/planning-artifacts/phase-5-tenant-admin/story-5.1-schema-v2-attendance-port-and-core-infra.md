---
id: "story-5.1"
epic_id: "epic-phase-5-tenant-admin"
title: "Phase 5 Schema Extensions (V2), Reusable Attendance Port & Subfolder Core Infrastructure"
status: "ready-for-review"
type: "foundation"
created_at: "2026-11-25"
dependencies: []
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/main/java/com/company/GymVerse/models/tenant/MembershipPlan.java"
  - "src/main/java/com/company/GymVerse/models/tenant/MemberMembership.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Revenue.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MembershipPlanRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberMembershipRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/JoinRequestRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/ExpenseRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/RevenueRepository.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
files_to_create:
  - "src/main/resources/db/migration/tenant/V2__phase5_tenant_admin_extensions.sql"
  - "src/main/java/com/company/GymVerse/models/tenant/PlanTier.java"
  - "src/main/java/com/company/GymVerse/models/tenant/GymFacility.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Trainer.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/PlanTierRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/GymFacilityRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/TrainerRepository.java"
  - "src/main/java/com/company/GymVerse/attendance/service/AttendanceQueryPort.java"
  - "src/main/java/com/company/GymVerse/attendance/service/AttendanceQueryStubImpl.java"
  - "src/main/java/com/company/GymVerse/attendance/dto/LiveAttendanceSummary.java"
  - "src/main/java/com/company/GymVerse/attendance/dto/MemberAttendanceSnapshot.java"
  - "src/main/java/com/company/GymVerse/attendance/dto/TrainerAttendanceSummary.java"
  - "src/main/java/com/company/GymVerse/attendance/dto/LiveRosterEntry.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/config/TenantOperationsProperties.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/PlanTheme.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/PaymentStatus.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/PaymentMode.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/RevenueType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/ExpenseCategory.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/FinancePeriodType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/MemberFilterStatus.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/helper/TenantAdminContextHelper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/helper/MembershipCalculationHelper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/helper/FinancePeriodHelper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/subscription/FetchTenantSubscriptionService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/subscription/FetchTenantSubscriptionRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/subscription/TenantSubscriptionDetailsResponse.java"
---

# Story 5.1: Phase 5 Schema Extensions (`V2`), Reusable Attendance Port & Subfolder Core Infrastructure

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Protected Multitenancy Invariant**: All classes in `com.company.GymVerse.multitanancy` remain **100% byte-for-byte untouched**.
> 4. **Reusable Attendance Separation**: Attendance live-query contracts MUST live in `com.company.GymVerse.attendance` (NOT inside `tenantAdmin`) so future Member/Trainer/Staff modules reuse them directly.
> 5. **Subfolder Organization**: Move existing `FetchTenantSubscription*` classes cleanly into `subscription/` subfolders under `service/`, `request/`, and `response/` while keeping all existing tests green.

---

## 1. User Story

**As a** backend architect building the 7-page Tenant Admin suite,  
**I want** the `V2` tenant database schema, domain entities/repositories with JPA Specifications, reusable attendance stub port, theme catalog, and subfoldered core infrastructure in place,  
**So that** all subsequent feature stories (`5.2`–`5.7`) build cleanly with zero code duplication, clean separation of concerns, and future-ready live-attendance hooks.

---

## 2. Technical Context & Implementation Strategy

1. **Tenant Flyway Migration (`V2__phase5_tenant_admin_extensions.sql`)**:
   - Create `plan_tiers` table and seed `ELITE` (`theme_key = 'GOLDEN'`, `is_default = TRUE`) and `LEGENDARY` (`theme_key = 'PURPLE'`, `is_default = TRUE`).
   - Create `gym_facilities` table (`id`, `name` unique, `created_at`).
   - Alter `membership_plans`: drop `tier` and `facility`, add `plan_tier_id BIGINT NULL` FK (`fk_mp_plan_tier` -> `plan_tiers(id)`), and add `is_trial BOOLEAN NOT NULL DEFAULT FALSE`.
   - Create `membership_plan_facilities` join table (`plan_id` FK -> `membership_plans(id)`, `facility_id` FK -> `gym_facilities(id)`).
   - Alter `member_memberships`: add `paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00`, `payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID'`, `is_renewal BOOLEAN NOT NULL DEFAULT FALSE`, `is_trial BOOLEAN NOT NULL DEFAULT FALSE`.
   - Alter `revenue`: drop foreign key & unique index `uq_rev_membership` on `member_membership_id`, modify `member_membership_id BIGINT NULL`, re-add foreign key `fk_rev_membership`, add `revenue_type VARCHAR(30) NOT NULL DEFAULT 'NEW_MEMBERSHIP'`, add `notes TEXT NULL`.
   - Create `trainers` table (`id`, `user_id` unique FK -> `users(id)`, `specialization VARCHAR(150)`, `experience_years INT`, `bio TEXT`, `joined_date DATE NOT NULL`, `active BOOLEAN NOT NULL DEFAULT TRUE`).
   - Add index `idx_exp_category_date (category, expense_date)` on `expenses`.
   - *(Note: Ensure SQL is compatible with both MySQL 8 and H2 MySQL-mode used in tests, or adjust H2/test profile if needed.)*

2. **Entities & Repositories (`JpaSpecificationExecutor`)**:
   - Add `PlanTier`, `GymFacility`, `Trainer` JPA entities in `com.company.GymVerse.models.tenant`.
   - Update `MembershipPlan`, `MemberMembership`, `Revenue` to map all new columns cleanly.
   - Extend `MembershipPlanRepository`, `MemberRepository`, `JoinRequestRepository`, `ExpenseRepository`, `TrainerRepository` with `JpaSpecificationExecutor<T>`.
   - Add `PlanTierRepository`, `GymFacilityRepository`, `TrainerRepository`, plus targeted aggregation query methods on `RevenueRepository` / `ExpenseRepository` / `MemberMembershipRepository` where helpful (keeping business logic in Helpers/Services).

3. **Reusable Attendance Port (`com.company.GymVerse.attendance`)**:
   - Interface `AttendanceQueryPort`:
     - `LiveAttendanceSummary getLiveAttendanceSummary(LocalDate expiringSoonCutoffDate)` -> returns `liveMembersCount=0`, `liveExpiringSoonCount=0`, `liveExpiredCount=0`, `liveTrialCount=0`, `avgTimeSpentMinutes=0.0`.
     - `MemberAttendanceSnapshot getMemberAttendanceSnapshot(Long memberId)` -> returns `totalVisits=0L`, `currentStreak=0`, `checkedInToday=false`, `checkInTime=null`, `elapsedMinutes=null`.
     - `TrainerAttendanceSummary getTrainerAttendanceSummary()` -> returns `activeLiveTrainersCount=0L`, `avgAttendanceRate=0.0`.
     - `Page<LiveRosterEntry> getLiveRoster(String search, String membershipStatus, Pageable pageable)` -> returns `Page.empty(pageable)`.
   - `AttendanceQueryStubImpl` (`@Service`): implements `AttendanceQueryPort` returning safe zeros/empty pages, annotated with clear `// TODO [ATTENDANCE-MODULE]: Replace stub with live geofencing attendance repository queries when the Tenant Attendance module is implemented.`

4. **Theme Catalog (`PlanTheme`) & Domain Enums**:
   - `PlanTheme` enum containing:
     - Default themes: `GOLDEN`, `PURPLE`
     - 20 canonical custom UI-aligned themes: `EMERALD_GREEN`, `CRIMSON_RED`, `OCEAN_BLUE`, `MIDNIGHT_BLACK`, `SUNSET_ORANGE`, `CYAN_WAVE`, `ROSE_PINK`, `AMBER_GOLD`, `INDIGO_NIGHT`, `TEAL_MINT`, `SLATE_SILVER`, `VIOLET_SPARK`, `CORAL_BLUSH`, `LIME_PULSE`, `COPPER_BRONZE`, `ARCTIC_ICE`, `FOREST_PINE`, `MAGENTA_GLOW`, `CHARCOAL_STEEL`, `ROYAL_COBALT`.
     - Provide helper method `isCustomTheme()` (all 20 except `GOLDEN` and `PURPLE`) and `getDefaultTierTheme(String tierName)`.
   - Enums: `PaymentStatus` (`PAID`, `PARTIALLY_PAID`), `PaymentMode` (`CASH`, `ONLINE`), `RevenueType` (`NEW_MEMBERSHIP`, `RENEWAL`, `OTHER`), `ExpenseCategory` (`ELECTRICITY`, `STAFF_SALARY`, `RENT`, `MACHINE_REPAIR`, `WATER_BILL`, `MAINTENANCE`, `OTHER`), `FinancePeriodType` (`WEEKLY`, `MONTHLY`, `YEARLY`), `MemberFilterStatus` (`ALL`, `LIVE`, `EXPIRED`, `TRIAL`).

5. **Helpers & Configuration**:
   - `TenantOperationsProperties`: `@ConfigurationProperties(prefix = "gymverse.tenant.membership")` with `int expiringSoonDays = 7;` bound in `application.yaml`.
   - `TenantAdminContextHelper`: extracts `tenantId`, `centralUserId`, `TenantUser` (current admin) cleanly from `JwtUtil` + `HttpServletRequest` + `TenantUserRepository`, eliminating duplication across services.
   - `MembershipCalculationHelper`: computes `finalPrice` (from `basePrice`, `discountType`, `discountValue`), `daysRemaining`, `isTrialMember(Member, List<MemberMembership>, LocalDate today)` (supporting **both** `trialEndDate >= today` AND `isTrial == true` on active membership/plan), and `isExpiredMember(...)`.
   - `FinancePeriodHelper`: converts `FinancePeriodType` (`WEEKLY`, `MONTHLY`, `YEARLY`) + optional `LocalDate referenceDate` into `[startDate, endDate]` and `[startInstant, endInstant]`, plus safe percentage calculation (`BigDecimal`).

6. **Subfolder Reorganization of Existing Subscription Files**:
   - Move `FetchTenantSubscriptionService`, `FetchTenantSubscriptionRequest`, `TenantSubscriptionDetailsResponse` into `subscription/` subpackages and update imports/tests (`FetchTenantSubscriptionServiceTest`).
   - Add pagination validation helper `validatePaginationAndSorting(Integer page, Integer size, String sortBy, String sortDirection, Set<String> allowedSortFields)` to `TenantAdminValidations`.

---

## 3. Acceptance Criteria

- [ ] `V2__phase5_tenant_admin_extensions.sql` executes cleanly in Flyway (verified against H2/MySQL migration test).
- [ ] `PlanTier`, `GymFacility`, `Trainer` entities & repositories exist; `MembershipPlan`, `MemberMembership`, `Revenue` updated cleanly.
- [ ] `AttendanceQueryPort` and `AttendanceQueryStubImpl` exist in `com.company.GymVerse.attendance` with explicit `// TODO` comments.
- [ ] `PlanTheme` defines `GOLDEN`, `PURPLE`, and 20 canonical custom theme names.
- [ ] `TenantAdminContextHelper`, `MembershipCalculationHelper`, `FinancePeriodHelper`, and `TenantOperationsProperties` are unit-tested.
- [ ] Existing `FetchTenantSubscriptionService` resides in `service/subscription/` and all existing tests pass.
- [ ] Protected multitenancy core remains 100% untouched.
