---
id: "story-5.7"
epic_id: "epic-phase-5-tenant-admin"
title: "Admin Dashboard, Live Roster Gateway & API Payloads Documentation"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
  - "story-5.2"
  - "story-5.3"
  - "story-5.4"
  - "story-5.6"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/dashboard/FetchAdminDashboardRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/liveRoster/FetchLiveRosterRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/dashboard/AdminDashboardResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/dashboard/DashboardTierBreakdownItem.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/dashboard/DashboardRevenueCardResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/dashboard/DashboardExpiryCardResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/liveRoster/LiveRosterResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/liveRoster/LiveRosterMemberResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/dashboard/FetchAdminDashboardService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/liveRoster/FetchLiveRosterService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/dashboard/DashboardAndLiveRosterServiceTest.java"
---

# Story 5.7: Admin Dashboard, Live Roster Gateway & API Payloads Documentation

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Reusable Attendance Boundary**: Both `FetchAdminDashboardService` and `FetchLiveRosterService` MUST query `AttendanceQueryPort` with explicit `// TODO [ATTENDANCE-MODULE]` comments, cleanly returning `0` / empty lists until the live attendance module is implemented.
> 4. **Subfolder Classification**: Place classes under `dashboard/` and `liveRoster/` subfolders in `service/`, `request/`, and `response/`.
> 5. **API Contracts (`docs/payloads.md`)**: Document every new Phase 5 operation with all 13 mandatory contract fields and copy-pasteable cURL examples.

---

## 1. User Story

**As a** Tenant Administrator opening the main Dashboard or the Live Roster page,  
**I want** a unified Dashboard snapshot (member totals, active rate, tier breakdowns, revenue card, expiring-soon card, and live placeholders) and a Live Roster search/summary gateway,  
**So that** I get an instant executive overview of my gym while remaining seamlessly ready for live geofence attendance drop-in.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_ADMIN_DASHBOARD`
- Uses `TenantOperationsProperties.getExpiringSoonDays()` (default `7`) and `AttendanceQueryPort.getLiveAttendanceSummary(expiringCutoff)`.
- Returns `AdminDashboardResponse`:
  - **Top KPIs**:
    - `totalMembers` (`long`)
    - `liveMembers` (`0L` via `AttendanceQueryPort` + `// TODO [ATTENDANCE-MODULE]`)
    - `totalRevenue` (`BigDecimal`)
    - `activeRate` (`BigDecimal` percentage: `(activeMembers / totalMembers) * 100`, `0.00` when `totalMembers == 0`)
  - **Tier-Based Active Breakdown** (`activeByTier`): list of `DashboardTierBreakdownItem { tierName, themeKey, count }` (members currently active per tier).
  - **Membership Card** (`membershipCard`): `{ totalMembers, tierBreakdown: List<DashboardTierBreakdownItem> }`.
  - **Revenue Card** (`revenueCard`): `{ totalRevenue, newMembershipRevenue, renewalRevenue, otherRevenue }`.
  - **Membership Expiry & Live Card** (`membershipExpiryCard`): `{ expiringSoonCount, expiringSoonWindowDays, liveExpiringSoonCount (0L via AttendanceQueryPort + // TODO) }`.

### 2.2 `FETCH_LIVE_ROSTER`
- Request (`FetchLiveRosterRequest { search (optional), membershipFilter (ALL | EXPIRED | TRIAL, default ALL), page, size, sortBy, sortDirection }`).
- Validates pagination & whitelisted `sortBy` fields (`checkInTime`, `memberName`, `elapsedMinutes`).
- Delegates to `AttendanceQueryPort.getLiveAttendanceSummary(...)` and `AttendanceQueryPort.getLiveRoster(search, membershipFilter, pageable)` (`// TODO [ATTENDANCE-MODULE]`).
- Returns `LiveRosterResponse`:
  - `totalLiveCount` (`0L`)
  - `avgTimeSpentMinutes` (`0.0`)
  - `liveExpiredCount` (`0L`)
  - `liveTrialCount` (`0L`)
  - `roster` (`PagedResponse<LiveRosterMemberResponse>`)

### 2.3 API Documentation (`docs/payloads.md`)
- Add complete section covering all Phase 5 `/api/tenant-admin` operations with 13 mandatory fields and cURL examples.

---

## 3. Acceptance Criteria

- [ ] `FETCH_ADMIN_DASHBOARD` accurately computes active rate, tier breakdowns, revenue card (`NEW_MEMBERSHIP`, `RENEWAL`, `OTHER`), and configurable 7-day expiring-soon counts while returning `0` for live metrics via `AttendanceQueryPort`.
- [ ] `FETCH_LIVE_ROSTER` validates pagination/sorting and returns `0` counts + empty `PagedResponse` cleanly via `AttendanceQueryPort` with clear `// TODO` hooks.
- [ ] `docs/payloads.md` updated for all Phase 5 operations.
- [ ] Full test suite runs cleanly with 0 failures/errors and all 8 protected multitenancy classes remain untouched.
