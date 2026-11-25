# Story 5.4 Task Log: Members Directory, Mini-Dashboard & Member Details View

## 1. Context & Operational Goals
- **Objective:** Implement the Members Directory, Mini-Dashboard, and Member Details operations on `/api/tenant-admin` (`FETCH_MEMBERS_SUMMARY`, `FETCH_MEMBERS`, `FETCH_MEMBER_DETAILS`) organized under `service/members/`, `request/members/`, `response/members/`, `response/enums/`, and `specifications/members/`.
- **Architectural Invariants:**
  - Single unified `@PostMapping("/api/tenant-admin")` controller gateway via `TenantAdminRequestType`.
  - Dual trial classification supporting **both** `members.trial_end_date >= today` and active `is_trial = true` on `member_memberships` / `membership_plans`.
  - All live attendance counts, `LIVE` member IDs, lifetime visits, streaks, and daily check-in status delegate exclusively to `AttendanceQueryPort` with explicit `// TODO [ATTENDANCE-MODULE]` markers.
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched; zero autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Enums, Request & Response DTOs**:
   - `TenantAdminRequestType`: Added `FETCH_MEMBERS_SUMMARY`, `FETCH_MEMBERS`, `FETCH_MEMBER_DETAILS`.
   - `MemberMembershipStatus`: Added `ACTIVE`, `TRIAL`, `EXPIRED`.
   - `FetchMembersRequest`, `FetchMemberDetailsRequest` in `request/members/`; updated `TenantAdminRequest`.
   - `MembersSummaryResponse`, `MemberListItemResponse`, `MemberDetailsResponse` (with `AttendanceActivityCard`, `DailyAttendanceCard`, `QuickStatsCard`, `MembershipDetailsCard`, `ContactDetailsCard`) in `response/members/`; updated `TenantAdminResponse`.
2. **Repositories, Specifications, Helpers, Validations & Mapper**:
   - `MemberMembershipRepository`: Added `findAllWithPlanByMemberIdIn` (batch loading for paged directory rows without N+1 queries) and `findAllWithPlanAndFacilitiesByMemberId` (eagerly fetches `plan`, `planTier`, and `facilities` for member details).
   - `RevenueRepository`: Added `sumTotalPaidByMemberId` aggregating `revenue.amount` across all memberships of a member.
   - `MembershipCalculationHelper`: Added dual-trial `isTrialActive`, `resolveMemberStatus`, and `resolveMemberDaysRemaining` helpers.
   - `MemberSpecification`: Dynamic JPA Criteria builder supporting `ALL`, `LIVE` (safe empty disjunction when stubbed), `EXPIRED` (`trialEndDate < today` / null and no active non-expired membership), `TRIAL` (dual `trialEndDate >= today` OR active `member_memberships.trial` / `membership_plans.trial`), and case-insensitive `search` across `user.firstName`, `user.lastName`, `user.email`, `user.phoneNumber`, and `gymId`.
   - `TenantAdminValidations` / `TenantAdminValidationsImpl`: Added `validateFetchMembers` (whitelisted sort fields: `id`, `joinDate`, `gymId`, `user.firstName`) and `validateFetchMemberDetails`.
   - `TenantAdminMapper` / `TenantAdminMapperImpl`: Handwritten mappings for summary, member directory items, and 4-card member details.
3. **Services (`service/members/`)**:
   - `FetchMembersSummaryService` (`FETCH_MEMBERS_SUMMARY`)
   - `FetchMembersService` (`FETCH_MEMBERS`)
   - `FetchMemberDetailsService` (`FETCH_MEMBER_DETAILS`)
4. **Documentation & Tests**:
   - Updated `docs/payloads.md` with 13-field API contracts and cURL commands for Operations `4.2`, `4.3`, and `4.4`.
   - Added `MembersServiceTest` (4 unit tests) and `MembersSpecificationIntegrationTest` (MySQL 9.1 integration test verifying `MemberSpecification`, dual-trial/expired counts, sorting by `user.firstName`, and `MemberDetailsResponse` cards).

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 40/40 tests passed (0 failures, 0 errors, 0 skipped) across:
  - `MembersServiceTest` (4)
  - `MembersSpecificationIntegrationTest` (1)
  - `JoinRequestAdminServiceTest` (5)
  - `JoinRequestSpecificationIntegrationTest` (1)
  - `PlansServiceTest` (3)
  - `PlansSpecificationIntegrationTest` (1)
  - `FetchTenantSubscriptionServiceTest` (2)
  - `TenantSubscriptionIntegrationTest` (10)
  - `TenantAdminValidationsImplTest` (4)
  - `MembershipCalculationHelperTest` (4)
  - `TenantAdminContextHelperTest` (4)
  - `FinancePeriodHelperTest` (1)
- **Protected Core Invariant:** `git status --short src\main\java\com\company\GymVerse\multitanancy` returned empty (100% untouched).
- **Git Safety:** Zero commits or pushes executed.
