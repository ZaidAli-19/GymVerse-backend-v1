# Story 5.1 Task Log: Phase 5 Schema Extensions (`V2`), Reusable Attendance Port & Subfolder Core Infrastructure

## 1. Context & Operational Goals
- **Objective:** Establish the database schema, domain entities/repositories, reusable cross-role attendance query port, theme catalog, shared helpers, and page-based subfolder structure for Phase 5 (In-Gym Tenant Administration).
- **Architectural Invariants:**
  - Single unified `@PostMapping("/api/tenant-admin")` controller gateway preserved.
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
  - Live attendance contracts isolated in `com.company.GymVerse.attendance` (outside `tenantAdmin`) so Member, Trainer, Staff, and Admin modules share the live-traffic USP uniformly.
  - Strictly no autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Tenant Flyway Migration (`V2__phase5_tenant_admin_extensions.sql`)**:
   - Created `plan_tiers` and seeded default tiers (`ELITE` -> `GOLDEN`, `LEGENDARY` -> `PURPLE`).
   - Created `gym_facilities` for reusable admin-managed facilities.
   - Altered `membership_plans`: dropped legacy `tier` and `facility` string columns, added nullable `plan_tier_id` FK (`fk_mp_plan_tier` -> `plan_tiers(id)`) and `is_trial` boolean flag.
   - Created `membership_plan_facilities` many-to-many join table (`plan_id`, `facility_id`).
   - Altered `member_memberships`: added `paid_amount`, `payment_status`, `is_renewal`, and `is_trial`.
   - Altered `revenue`: dropped `uq_rev_membership`, made `member_membership_id` nullable with `idx_rev_membership` FK index, added `revenue_type` (`NEW_MEMBERSHIP` default) and `notes`.
   - Created `trainers` table (1:1 with `users`) for trainer operational profiles.
   - Added `idx_exp_category_date` on `expenses`.
2. **Tenant Entities & Repositories**:
   - Added `PlanTier`, `GymFacility`, `Trainer` entities and `PlanTierRepository`, `GymFacilityRepository`, `TrainerRepository`.
   - Updated `MembershipPlan`, `MemberMembership`, and `Revenue`.
   - Added `JpaSpecificationExecutor<T>` to `MembershipPlanRepository`, `MemberRepository`, `JoinRequestRepository`, `ExpenseRepository`, and `TrainerRepository`.
3. **Reusable Attendance Boundary (`com.company.GymVerse.attendance`)**:
   - Created `AttendanceQueryPort` + `AttendanceQueryStubImpl` returning safe zero/empty values with `// TODO [ATTENDANCE-MODULE]` markers, plus DTOs `LiveAttendanceSummary`, `MemberAttendanceSnapshot`, `TrainerAttendanceSummary`, `LiveRosterEntry`.
4. **Domain Enums, Theme Catalog & Helpers**:
   - Standardized ALL tenant status, type, category, theme, discount, and payment fields to `@Enumerated(EnumType.STRING)` enums in `com.company.GymVerse.models.tenant.enums`: `PlanTheme`, `DiscountType`, `PaymentStatus`, `PaymentMode`, `RevenueType`, `ExpenseCategory`, `JoinRequestStatus`, `MemberType`, `MembershipRequestStatus`, `InvitationStatus`, and `AttendanceStatus` (keeping all free-form names as `String`).
   - Kept query/filter-only UI enums (`TenantAdminRequestType`, `FinancePeriodType`, `MemberFilterStatus`) in `tenantAdmin/request/enums/`.
   - Added `TenantOperationsProperties` (`gymverse.tenant.membership.expiring-soon-days: 7`).
   - Added `TenantAdminContextHelper`, `MembershipCalculationHelper`, and `FinancePeriodHelper`.
   - Added `PagedResponse<T>` and `TenantAdminValidations.validateAndBuildPageable(...)`.
5. **Subfolder Reorganization**:
   - Moved Story 4.6 subscription classes into `service/subscription/`, `request/subscription/`, and `response/subscription/`.

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 42 tests passed (0 failures, 0 errors) across `AttendanceQueryStubAndThemeTest` (2), `FinancePeriodHelperTest` (1), `MembershipCalculationHelperTest` (4), `TenantAdminContextHelperTest` (4), `FetchTenantSubscriptionServiceTest` (2), `TenantAdminValidationsImplTest` (2), `TenantSubscriptionIntegrationTest` (10), `TenantPersistenceIntegrationTest` (1), `TenantIsolationIntegrationTest` (1), `SubmitJoinRequestServiceTest` (5), and `UserValidationsTest` (10) against MySQL 9.1 (`V1` + `V2` Flyway verified).
- **Protected Core Invariant:** `git status -s src/main/java/com/company/GymVerse/multitanancy` returned empty (100% untouched).
- **Git Safety:** Zero commits or pushes executed.
