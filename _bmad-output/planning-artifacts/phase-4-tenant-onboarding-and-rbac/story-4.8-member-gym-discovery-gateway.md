---
id: "story-4.8"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Member Gym Discovery Gateway (FETCH_GYM_BY_CODE)"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-20"
dependencies:
  - "story-4.4r"
  - "story-4.5"
files_to_modify:
  - "src/main/java/com/company/GymVerse/user/request/enums/UserRequestType.java"
  - "src/main/java/com/company/GymVerse/user/request/UserRequest.java"
  - "src/main/java/com/company/GymVerse/user/response/UserResponse.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidations.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapper.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapperImpl.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/user/request/FetchGymByCodeRequest.java"
  - "src/main/java/com/company/GymVerse/user/response/FetchGymByCodeResponse.java"
  - "src/main/java/com/company/GymVerse/user/util/UserHelper.java"
  - "src/main/java/com/company/GymVerse/user/service/FetchGymByCodeService.java"
  - "src/test/java/com/company/GymVerse/user/service/FetchGymByCodeServiceTest.java"
  - "src/test/java/com/company/GymVerse/user/util/UserHelperTest.java"
---

# Story 4.8: Member Gym Discovery Gateway (`FETCH_GYM_BY_CODE`)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Single Controller Gateway Invariant**:
>    - Operation routes exclusively through `@PostMapping("/api/user")` via discriminator `UserRequestType.FETCH_GYM_BY_CODE`.
>    - NO new controllers, sub-path routes, or `@GetMapping` endpoints.
> 4. **Token Scope Invariant**:
>    - Strictly requires a valid **Master Account token (`tokenType: MASTER_ACCOUNT`)**.
>    - Enforced by `UserValidations.validateMasterContext()`. Tenant tokens or anonymous requests are rejected.
> 5. **Clean Architecture Standards**:
>    - All domain constraints and validations reside in `UserValidations` / `UserValidationsImpl`.
>    - Boilerplate security context resolution and repository lookup logic reside in `UserHelper`.
>    - Object mapping is 100% handwritten in `UserMapper` / `UserMapperImpl` with zero reflection.

---

## 1. User Story

**As a** registered central master user on the mobile app,  
**I want** to search for a gym using its unique `gymCode` (scanned from a QR code or entered manually),  
**So that** I can view the gym's verified identity, address, contact details, and active status before deciding to submit a join request.

---

## 2. Technical Context & Requirements

1. **Request Flow**:
   - Caller sends `POST /api/user` with:
     ```json
     {
       "requestType": "FETCH_GYM_BY_CODE",
       "fetchGymByCodeRequest": {
         "gymCode": "FIT-101"
       }
     }
     ```
2. **Security & Validation Layer**:
   - `UserValidations.validateMasterContext()` verifies caller is authenticated with a `MASTER_ACCOUNT` token and `TenantContext` is null.
   - `UserValidations.validateFetchGymByCode(request)` ensures `fetchGymByCodeRequest` and `gymCode` are not null or blank.
   - `UserHelper.normalizeGymCode(gymCode)` trims and standardizes the code to uppercase.
3. **Database Query & Status Verification**:
   - Queries central `GymTenantRepository.findByGymCode(gymCode)`.
   - If not found, throws `ResourceNotFoundException("Gym not found with code: " + gymCode)`.
   - Checks latest subscription status in `gym_subscriptions` via `UserHelper.getLatestSubscriptionStatus(gymTenant.getId())`.
   - `UserValidations.validateGymActiveForJoin(gymTenant, subStatus)` verifies:
     - Gym status is `ACTIVE` (rejects `SUSPENDED` or `INACTIVE` with `BadRequestException`).
     - Gym subscription status is `ACTIVE` or `TRIAL` (rejects `EXPIRED` or `CANCELLED` with `BadRequestException`).
4. **Response Structure**:
   - Returns 200 OK wrapped in unified `UserResponse`:
     ```json
     {
       "message": "Gym details retrieved successfully",
       "status": 200,
       "timestamp": "2026-11-20T15:00:00Z",
       "fetchGymByCodeResponse": {
         "tenantId": 1,
         "gymCode": "FIT-101",
         "gymName": "Iron Paradise Gym",
         "address": "123 Muscle Beach Rd",
         "city": "Venice",
         "phoneNumber": "+1-555-0199",
         "contactEmail": "info@ironparadise.com",
         "logoUrl": "https://cdn.gymverse.com/logos/iron-paradise.png",
         "status": "ACTIVE",
         "subscriptionStatus": "TRIAL"
       }
     }
     ```

---

## 3. Acceptance Criteria

- [ ] `UserRequestType.FETCH_GYM_BY_CODE` registered and dispatched via `UserFactory`.
- [ ] Enforces `MASTER_ACCOUNT` token authentication; rejects unauthenticated callers.
- [ ] Returns verified gym metadata when `gymCode` matches an active gym in good standing.
- [ ] Throws `ResourceNotFoundException` (HTTP 404) if `gymCode` does not exist.
- [ ] Throws `BadRequestException` (HTTP 400) if gym is suspended or subscription is expired.
- [ ] Validations completely decoupled into `UserValidations` / `UserValidationsImpl`.
- [ ] Boilerplate extraction centralized in `UserHelper`.
- [ ] Unit test coverage in `FetchGymByCodeServiceTest` and `UserHelperTest` with 100% assertions passing.
- [ ] Documentation updated in `docs/payloads.md` with complete contract and cURL example.
