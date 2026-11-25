# Story 5.5 Task Log: Trainers & Staff Summary & Searchable Address Book

## 1. Context & Operational Goals
- **Objective:** Implement the Trainers / Staff summary header and searchable, paginated address book operations on `/api/tenant-admin` (`FETCH_TRAINERS_SUMMARY`, `FETCH_TRAINERS`) organized under `service/trainers/`, `request/trainers/`, `response/trainers/`, and `specifications/trainers/`.
- **Architectural Invariants:**
  - Single unified `@PostMapping("/api/tenant-admin")` controller gateway via `TenantAdminRequestType`.
  - Add Trainer onboarding remains deferred to a future phase.
  - `activeLiveCount` and `avgAttendanceRate` delegate exclusively to `AttendanceQueryPort` with explicit `// TODO [ATTENDANCE-MODULE]` hooks.
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched; zero autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Enums, Request & Response DTOs**:
   - `TenantAdminRequestType`: Added `FETCH_TRAINERS_SUMMARY`, `FETCH_TRAINERS`.
   - `FetchTrainersRequest` in `request/trainers/`; updated `TenantAdminRequest`.
   - `TrainersSummaryResponse` (`totalTrainersCount`, `activeTrainersCount`, `activeLiveCount`, `avgAttendanceRate`) and `TrainerItemResponse` (`trainerId`, `userId`, `firstName`, `lastName`, `email`, `phoneNumber`, `specialization`, `experienceYears`, `bio`, `joinedDate`, `active`, `assignedMembersCount`) in `response/trainers/`; updated `TenantAdminResponse`.
2. **Repository, Specification, Validations & Mapper**:
   - `MemberRepository`: Added `countAssignedMembersByTrainerUserIds` (`GROUP BY m.trainer.id`) to resolve assigned member counts in a single batch query without N+1 selects.
   - `TrainerSpecification`: Dynamic JPA Criteria filtering for `active`, case-insensitive partial `specialization`, and `search` across `user.firstName`, `user.lastName`, `user.email`, `user.phoneNumber`, and `specialization`.
   - `TenantAdminValidations` / `TenantAdminValidationsImpl`: Added `validateFetchTrainers` with whitelisted sort fields (`id`, `joinedDate`, `specialization`, `user.firstName`).
   - `TenantAdminMapper` / `TenantAdminMapperImpl`: Handwritten conversions for trainer summary and trainer address book rows.
3. **Services (`service/trainers/`)**:
   - `FetchTrainersSummaryService` (`FETCH_TRAINERS_SUMMARY`)
   - `FetchTrainersService` (`FETCH_TRAINERS`)
4. **Documentation & Tests**:
   - Updated `docs/payloads.md` with 13-field API contracts and cURL commands for Operations `4.5` and `4.6`.
   - Added `TrainersServiceTest` (2 unit tests) and `TrainersSpecificationIntegrationTest` (MySQL 9.1 integration test verifying `TrainerSpecification` filters, sorting by `user.firstName`, and grouped `assignedMembersCount`).

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 43/43 tests passed (0 failures, 0 errors, 0 skipped) across all 14 `tenantAdmin` unit and MySQL integration test classes (including `TrainersServiceTest` 2/2 and `TrainersSpecificationIntegrationTest` 1/1).
- **Protected Core Invariant:** `git status --short src\main\java\com\company\GymVerse\multitanancy` returned empty (100% untouched).
- **Git Safety:** Zero commits or pushes executed.
