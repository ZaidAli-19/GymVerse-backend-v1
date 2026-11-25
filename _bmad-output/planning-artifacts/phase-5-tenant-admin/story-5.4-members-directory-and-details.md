---
id: "story-5.4"
epic_id: "epic-phase-5-tenant-admin"
title: "Members Directory, Mini-Dashboard & Member Details View"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
  - "story-5.2"
  - "story-5.3"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberMembershipRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/RevenueRepository.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/specifications/members/MemberSpecification.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/members/FetchMembersRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/members/FetchMemberDetailsRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/members/MembersSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/members/MemberListItemResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/members/MemberDetailsResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/FetchMembersSummaryService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/FetchMembersService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/FetchMemberDetailsService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/members/MembersServiceTest.java"
---

# Story 5.4: Members Directory, Mini-Dashboard & Member Details View

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Reusable Attendance Boundary**: All live counts, `LIVE` filter, total visits, streak, and daily attendance check-in/elapsed status MUST delegate to `AttendanceQueryPort` with clear `// TODO [ATTENDANCE-MODULE]` comments.
> 4. **Dual Trial Support**: Trial classification MUST support **both** `members.trial_end_date >= today` AND `is_trial = true` on `member_memberships` / `membership_plans`.
> 5. **Subfolder Classification**: Services in `service/members/`, Requests in `request/members/`, Responses in `response/members/`, Specifications in `specifications/members/`.

---

## 1. User Story

**As a** Tenant Administrator managing my gym's Member Journal,  
**I want** a members mini-dashboard (total, live, expired, trial counts), a paginated/filtered member directory (`ALL`, `LIVE`, `EXPIRED`, `TRIAL`), and a comprehensive Member Details view (attendance card, membership card, payment & expiry days, contact details),  
**So that** I can track every member's status, payments, and engagement from one place.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_MEMBERS_SUMMARY`
- Returns `MembersSummaryResponse`:
  - `totalMembersCount` (`long`)
  - `liveCount` (`0L` via `attendanceQueryPort.getLiveAttendanceSummary(...)` + `// TODO [ATTENDANCE-MODULE]`)
  - `expiredCount` (`long`: members whose latest/active membership `endDate < today` or who have no active non-expired membership and no active trial)
  - `trialCount` (`long`: members with `trialEndDate >= today` OR active `isTrial == true` membership with `endDate >= today`)

### 2.2 `FETCH_MEMBERS`
- Request (`FetchMembersRequest { statusFilter (ALL | LIVE | EXPIRED | TRIAL), search, page, size, sortBy, sortDirection }`).
- Specification (`MemberSpecification.withFilters(statusFilter, search, today, liveMemberIds)`):
  - `ALL`: no status restriction.
  - `LIVE`: delegates to `AttendanceQueryPort` for live member IDs (`// TODO [ATTENDANCE-MODULE]: when live module arrives, supply active checked-in member IDs; currently returns empty page safely`).
  - `EXPIRED`: JPA subquery/predicate matching members without an active non-expired `MemberMembership` (`endDate >= today`) and without an active `trialEndDate >= today`.
  - `TRIAL`: JPA predicate matching `trialEndDate >= today` OR active `MemberMembership` (`isTrial = true` AND `endDate >= today`).
  - `search`: case-insensitive `LIKE` across `user.firstName`, `user.lastName`, `user.email`, `user.phoneNumber`, `gymId`.
- Returns `PagedResponse<MemberListItemResponse>` (`memberId`, `gymId`, `fullName`, `email`, `phoneNumber`, `planName`, `tier`, `themeKey`, `membershipStatus` [`ACTIVE`/`TRIAL`/`EXPIRED`], `daysRemaining`, `paidAmount`, `finalPrice`, `paymentStatus`, `checkedInToday` [`false` via `AttendanceQueryPort`]).

### 2.3 `FETCH_MEMBER_DETAILS`
- Request (`FetchMemberDetailsRequest { memberId }`).
- Loads `Member` (with `TenantUser`), latest/active `MemberMembership` (with `MembershipPlan` + `PlanTier` theme), total revenue paid by this member, and `MemberAttendanceSnapshot` from `AttendanceQueryPort`.
- Returns `MemberDetailsResponse` structured cleanly around the UI cards:
  - **Attendance & Activity**: `totalVisitCount` (`0L`), `currentStreak` (`0`), `dailyAttendance`: `{ checkedIn (false), checkInTime (null), elapsedMinutes (null) }` *(// TODO [ATTENDANCE-MODULE])*.
  - **Quick Stats**: `totalAmountPaid` (`BigDecimal`), `daysLeftUntilExpiry` (`long`, `0` if expired).
  - **Membership Details Card**: `planName`, `tier`, `themeKey`, `facility`, `joinDate`, `startDate`, `expiryDate`, `remainingDays`, `finalPrice`, `paidAmount`, `paymentStatus`, `isTrial`, `membershipStatus`.
  - **Contact Details Card**: `memberId`, `gymId`, `firstName`, `lastName`, `email`, `phoneNumber`.

---

## 3. Acceptance Criteria

- [ ] `FETCH_MEMBERS_SUMMARY` computes total, expired, and trial (both date & flag) accurately while stubbing `liveCount = 0` via `AttendanceQueryPort`.
- [ ] `FETCH_MEMBERS` filters via `MemberSpecification` + pagination + whitelisted sorting (`id`, `joinDate`, `gymId`, `user.firstName`).
- [ ] `FETCH_MEMBER_DETAILS` returns all 4 UI card groups with accurate payment totals and expiry countdowns, throwing `ResourceNotFoundException` for unknown `memberId`.
- [ ] Unit tests in `MembersServiceTest` verify summary counts, dual trial logic, expired filtering, search matching, and detail card calculations.
