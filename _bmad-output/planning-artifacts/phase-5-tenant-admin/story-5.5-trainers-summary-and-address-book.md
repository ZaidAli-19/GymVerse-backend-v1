---
id: "story-5.5"
epic_id: "epic-phase-5-tenant-admin"
title: "Trainers & Staff Summary & Searchable Address Book"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/specifications/trainers/TrainerSpecification.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/trainers/FetchTrainersRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/trainers/TrainersSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/trainers/TrainerItemResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/trainers/FetchTrainersSummaryService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/trainers/FetchTrainersService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/trainers/TrainersServiceTest.java"
---

# Story 5.5: Trainers & Staff Summary & Searchable Address Book

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Scope Note**: Per user confirmation, the **Add Trainer** onboarding flow is deferred to a future phase. This story delivers the Trainers dashboard summary and searchable/paginated trainer address book.
> 4. **Reusable Attendance Boundary**: `activeLiveCount` and `avgAttendanceRate` MUST delegate to `AttendanceQueryPort` with `// TODO [ATTENDANCE-MODULE]` comments.
> 5. **Subfolder Classification**: Services in `service/trainers/`, Requests in `request/trainers/`, Responses in `response/trainers/`, Specifications in `specifications/trainers/`.

---

## 1. User Story

**As a** Tenant Administrator viewing the Trainers / Staff page,  
**I want** a summary header showing total trainers, active live count, and average attendance rate alongside a searchable, paginated address book of gym trainers,  
**So that** I can look up trainer profiles, specializations, and contact information easily.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_TRAINERS_SUMMARY`
- Queries `TrainerRepository.countByActiveTrue()` (and total count) + `attendanceQueryPort.getTrainerAttendanceSummary()`.
- Returns `TrainersSummaryResponse`:
  - `totalTrainersCount` (`long`)
  - `activeLiveCount` (`0L` via `AttendanceQueryPort` + `// TODO [ATTENDANCE-MODULE]`)
  - `avgAttendanceRate` (`0.0` via `AttendanceQueryPort` + `// TODO [ATTENDANCE-MODULE]`)

### 2.2 `FETCH_TRAINERS`
- Request (`FetchTrainersRequest { active (optional Boolean), specialization (optional String), search (optional String), page, size, sortBy, sortDirection }`).
- Specification (`TrainerSpecification.withFilters(active, specialization, search)`):
  - `active`: optional equality on `trainer.active`.
  - `specialization`: optional case-insensitive `LIKE`.
  - `search`: case-insensitive `LIKE` across `user.firstName`, `user.lastName`, `user.email`, `user.phoneNumber`, `specialization`.
- Returns `PagedResponse<TrainerItemResponse>` (`trainerId`, `userId`, `firstName`, `lastName`, `email`, `phoneNumber`, `specialization`, `experienceYears`, `bio`, `joinedDate`, `active`, `assignedMembersCount`).

---

## 3. Acceptance Criteria

- [ ] `FETCH_TRAINERS_SUMMARY` returns total trainers count and delegates live/attendance metrics cleanly to `AttendanceQueryPort` (`0` / `0.0`).
- [ ] `FETCH_TRAINERS` uses `TrainerSpecification`, pagination, and whitelisted sorting (`id`, `joinedDate`, `specialization`, `user.firstName`).
- [ ] Unit tests in `TrainersServiceTest` verify summary counts, search filtering, active filter, and pagination mapping.
