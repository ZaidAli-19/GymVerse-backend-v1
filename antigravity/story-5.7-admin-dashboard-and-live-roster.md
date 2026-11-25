# Story 5.7 Task Log: Admin Dashboard, Live Roster Gateway & API Payloads Documentation

## 1. Context & Operational Goals
- **Objective:** Complete the final story of Phase 5 by implementing `FETCH_ADMIN_DASHBOARD` and `FETCH_LIVE_ROSTER` on `/api/tenant-admin` under `dashboard/` and `liveRoster/` subfolders, and ensuring `docs/payloads.md` documents every Phase 5 operation with all 13 mandatory fields and cURL examples.
- **Architectural Invariants:**
  - Single unified `@PostMapping("/api/tenant-admin")` controller gateway via `TenantAdminRequestType`.
  - All live occupancy, live expiring-soon, and checked-in roster queries delegate strictly to `AttendanceQueryPort` with explicit `// TODO [ATTENDANCE-MODULE]` hooks.
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched; zero autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Enums, Request & Response DTOs**:
   - `TenantAdminRequestType`: Added `FETCH_ADMIN_DASHBOARD`, `FETCH_LIVE_ROSTER`.
   - `FetchAdminDashboardRequest` (`request/dashboard/`) and `FetchLiveRosterRequest` (`request/liveRoster/`); updated `TenantAdminRequest`.
   - `AdminDashboardResponse` (with `DashboardMembershipCard`), `DashboardTierBreakdownItem`, `DashboardRevenueCardResponse`, `DashboardExpiryCardResponse` (`response/dashboard/`) and `LiveRosterResponse`, `LiveRosterMemberResponse` (`response/liveRoster/`); updated `TenantAdminResponse`.
2. **Repositories, Validations & Mapper**:
   - `MemberMembershipRepository`: Added `countActiveMembersGroupedByTier` and `countExpiringSoonMembers`.
   - `RevenueRepository`: Added `sumAmountGroupedByRevenueType` for executive revenue breakdown (`NEW_MEMBERSHIP`, `RENEWAL`, `OTHER`).
   - `TenantAdminValidations` / `TenantAdminValidationsImpl`: Added `validateFetchLiveRoster` (whitelisted sorts: `checkInTime`, `memberName`, `elapsedMinutes`) and `parseLiveRosterMembershipFilter` (`ALL`, `EXPIRED`, `TRIAL`).
   - `TenantAdminMapper` / `TenantAdminMapperImpl`: Handwritten conversions for dashboard and live roster payloads.
3. **Services (`service/dashboard/`, `service/liveRoster/`)**:
   - `FetchAdminDashboardService` (`FETCH_ADMIN_DASHBOARD`): Computes `totalMembers`, `activeRate` (with zero-member guard), `activeByTier`, `membershipCard`, `revenueCard`, and `membershipExpiryCard` using `TenantOperationsProperties.getExpiringSoonDays()`, while delegating `liveMembers` and `liveExpiringSoonCount` to `AttendanceQueryPort`.
   - `FetchLiveRosterService` (`FETCH_LIVE_ROSTER`): Delegates live occupancy metrics and paginated checked-in roster queries to `AttendanceQueryPort`.
4. **Documentation & Tests**:
   - Completed `docs/payloads.md` Section 4 covering all 19 `/api/tenant-admin` operations (`4.1`–`4.19`: Subscription, Members, Trainers, Finance, Plans, Join Requests, Admin Dashboard, and Live Roster) with 13-field contracts and cURL commands.
   - Added `DashboardAndLiveRosterServiceTest` (2 unit tests) and `DashboardIntegrationTest` (MySQL 9.1 integration test verifying tier breakdown, 7-day expiry counts, `activeRate`, revenue card, and live roster).

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 50/50 tests passed (0 failures, 0 errors, 0 skipped) across all 18 `tenantAdmin` unit and MySQL integration test classes (including `DashboardAndLiveRosterServiceTest` 2/2 and `DashboardIntegrationTest` 1/1).
- **Protected Core Invariant:** `git status --short src\main\java\com\company\GymVerse\multitanancy` returned empty (100% untouched).
- **Git Safety:** Zero commits or pushes executed.
