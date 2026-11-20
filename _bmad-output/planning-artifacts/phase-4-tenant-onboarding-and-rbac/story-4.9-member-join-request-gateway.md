---
id: "story-4.9"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Member Join Request Gateway (SUBMIT_JOIN_REQUEST)"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-20"
dependencies:
  - "story-4.8"
files_to_modify:
  - "src/main/java/com/company/GymVerse/user/request/enums/UserRequestType.java"
  - "src/main/java/com/company/GymVerse/user/request/UserRequest.java"
  - "src/main/java/com/company/GymVerse/user/response/UserResponse.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidations.java"
  - "src/main/java/com/company/GymVerse/user/validations/UserValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapper.java"
  - "src/main/java/com/company/GymVerse/user/mapper/UserMapperImpl.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/JoinRequestRepository.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/user/request/SubmitJoinRequest.java"
  - "src/main/java/com/company/GymVerse/user/response/SubmitJoinRequestResponse.java"
  - "src/main/java/com/company/GymVerse/user/service/SubmitJoinRequestService.java"
  - "src/test/java/com/company/GymVerse/user/service/SubmitJoinRequestServiceTest.java"
---

# Story 4.9: Member Join Request Gateway (`SUBMIT_JOIN_REQUEST`)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Single Controller Gateway Invariant**:
>    - Operation routes exclusively through `@PostMapping("/api/user")` via discriminator `UserRequestType.SUBMIT_JOIN_REQUEST`.
>    - NO new controllers or sub-path endpoints.
> 4. **Token Scope Invariant**:
>    - Strictly requires a valid **Master Account token (`tokenType: MASTER_ACCOUNT`)**.
>    - Enforced by `UserValidations.validateMasterContext()`.
> 5. **Multitenancy & Database Isolation Invariants**:
>    - The caller does NOT yet hold an active role in this gym; central validation checks `user_tenant_roles`.
>    - Insertion into tenant database table `join_requests` MUST be wrapped inside `TenantRoutingBridge.callInTenantTx(gymTenant.getDbName(), ...)`.
>    - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
> 6. **Clean Architecture Standards**:
>    - All validations (payload checks, duplicate association checks, pending request checks) live in `UserValidations` / `UserValidationsImpl`.
>    - Mappers are 100% handwritten in `UserMapper` / `UserMapperImpl`.

---

## 1. User Story

**As a** registered central master user on the mobile app who has discovered a gym,  
**I want** to submit a membership join request with optional personal notes,  
**So that** the gym's tenant administrators can review my application and approve my enrollment into their gym.

---

## 2. Technical Context & Requirements

1. **Request Flow**:
   - Caller sends `POST /api/user` with:
     ```json
     {
       "requestType": "SUBMIT_JOIN_REQUEST",
       "submitJoinRequest": {
         "gymCode": "FIT-101",
         "requestNotes": "Looking forward to morning weightlifting classes!"
       }
     }
     ```
2. **Security & Central Validation Layer**:
   - `UserValidations.validateMasterContext()` verifies caller possesses a valid `MASTER_ACCOUNT` token.
   - `UserValidations.validateSubmitJoinRequest(request)` checks `gymCode` is not blank and `requestNotes` does not exceed 1000 characters.
   - `UserHelper.getAuthenticatedUserAccount()` retrieves caller identity (`UserAccount`) from `SecurityContextHolder`.
   - `GymTenantRepository.findByGymCode(gymCode)` resolves target gym.
   - `UserValidations.validateGymActiveForJoin(gymTenant, subStatus)` ensures gym is `ACTIVE` and subscription is in good standing.
   - `UserValidations.validateUserNotAlreadyAssociated(userAccount.getId(), gymTenant.getId())` queries central `user_tenant_roles`:
     - If user is already an `ACTIVE` role holder, throws `BadRequestException("User is already an active member/associated with this gym")`.
     - If user has a `PENDING` role, throws `BadRequestException("User membership is already pending approval for this gym")`.
3. **Tenant Database Demarcation & Persistence**:
   - Uses `TenantRoutingBridge.callInTenantTx(gymTenant.getDbName(), status -> ...)` to safely execute inside tenant database transaction.
   - `UserValidations.validateNoPendingJoinRequest(userAccount.getId(), joinRequestRepository)` checks tenant table `join_requests` for open `'PENDING'` entries for this user:
     - Throws `BadRequestException("A pending join request already exists for this user in this gym")` if found.
   - Builds and persists `JoinRequest`:
     - `centralUserId`: caller's central account ID
     - `email`: caller's central email
     - `firstName`: caller's central first name
     - `lastName`: caller's central last name
     - `phoneNumber`: caller's central phone number
     - `status`: `"PENDING"`
     - `requestNotes`: from payload
     - `createdAt`: `Instant.now()`
4. **Response Structure**:
   - Returns 201 Created wrapped in unified `UserResponse`:
     ```json
     {
       "message": "Join request submitted successfully. Awaiting gym administrator review.",
       "status": 201,
       "timestamp": "2026-11-20T15:00:00Z",
       "submitJoinRequestResponse": {
         "requestId": 1,
         "gymTenantId": 10,
         "gymCode": "FIT-101",
         "gymName": "Iron Paradise Gym",
         "status": "PENDING",
         "requestNotes": "Looking forward to morning weightlifting classes!",
         "createdAt": "2026-11-20T15:00:00Z"
       }
     }
     ```

---

## 3. Acceptance Criteria

- [ ] `UserRequestType.SUBMIT_JOIN_REQUEST` registered and dispatched via `UserFactory`.
- [ ] Enforces `MASTER_ACCOUNT` token authentication; rejects unauthenticated callers.
- [ ] Rejects submission if user is already an active member in `user_tenant_roles`.
- [ ] Rejects submission if an open `PENDING` join request already exists in the target gym's `join_requests` table.
- [ ] Rejects submission if target gym is not `ACTIVE` or has an expired SaaS subscription.
- [ ] Safely routes to tenant database via `TenantRoutingBridge.callInTenantTx` with guaranteed `TenantContext` cleanup.
- [ ] Correctly snapshots user's contact information (`centralUserId`, `email`, `firstName`, `lastName`, `phoneNumber`) into tenant `join_requests`.
- [ ] Validations completely decoupled into `UserValidations` / `UserValidationsImpl`.
- [ ] Unit test coverage in `SubmitJoinRequestServiceTest` with 100% assertions passing.
- [ ] Documentation updated in `docs/payloads.md` with complete contract and cURL example.
