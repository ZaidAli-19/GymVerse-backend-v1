# Story 4.9 Task Log: Member Join Request Gateway (`SUBMIT_JOIN_REQUEST`)

## 1. Context & Operational Goals
- **Objective:** Enable central master users on mobile/web to submit a membership application to an active gym tenant after discovering the gym via its `gymCode`.
- **Architectural Invariants:**
  - Exclusively routed through `@PostMapping("/api/user")` via `UserRequestType.SUBMIT_JOIN_REQUEST`.
  - Requires `MASTER_ACCOUNT` token (enforced by `UserValidations.validateMasterContext()`).
  - Caller does NOT hold a tenant role yet; central relationship is validated in `user_tenant_roles`.
  - Tenant database transaction demarcated safely via `TenantRoutingBridge.callInTenantTx(gymTenant.getDbName(), ...)`.
  - Protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
  - Zero reflection, handwritten `UserMapperImpl`.
  - All domain validations decoupled into `UserValidationsImpl`.

## 2. Changes Implemented
1. **DTOs & Enums**:
   - `SubmitJoinRequest.java`: Request carrying `gymCode` and optional `requestNotes`.
   - `SubmitJoinRequestResponse.java`: Response payload confirming submission (`requestId`, `gymTenantId`, `gymCode`, `gymName`, `status`, `requestNotes`, `createdAt`).
   - `UserRequestType.java`: Added `SUBMIT_JOIN_REQUEST`.
   - `UserRequest.java`: Added `SubmitJoinRequest submitJoinRequest;`.
   - `UserResponse.java`: Added `SubmitJoinRequestResponse submitJoinRequestResponse;`.
2. **Repository Layer**:
   - `JoinRequestRepository.java`: Added `boolean existsByCentralUserIdAndStatus(Long centralUserId, String status);`.
3. **Validation Layer (`UserValidations` & `UserValidationsImpl`)**:
   - `validateSubmitJoinRequest(request)`: Checks `gymCode` non-blank and `requestNotes` length $\le$ 1,000 characters.
   - `validateUserNotAlreadyAssociated(userAccountId, gymTenantId)`: Queries central `user_tenant_roles` and rejects if user has an `ACTIVE` role or `PENDING` assignment.
   - `validateNoPendingJoinRequest(userAccountId, joinRequestRepository)`: Checks tenant `join_requests` table for open `'PENDING'` entries for this caller.
4. **Mapper Layer (`UserMapper` & `UserMapperImpl`)**:
   - `toSubmitJoinRequestResponse(joinRequest, gymCode, gymName)`: Handwritten conversion.
   - `toSubmitJoinRequestUserResponse(message, response)`: Wraps into unified `UserResponse` with HTTP 201 status.
5. **Service Layer (`SubmitJoinRequestService.java`)**:
   - Implements `UserOperation` for `SUBMIT_JOIN_REQUEST`.
   - Uses `UserHelper` to resolve authenticated `UserAccount`, normalize `gymCode`, and check subscription health.
   - Uses `TenantRoutingBridge.callInTenantTx(gymTenant.getDbName(), status -> ...)` to safely cross into the tenant database, check for open pending requests, snapshot caller profile, and persist into `join_requests`.
6. **Tests**:
   - `SubmitJoinRequestServiceTest.java`: 5 unit tests covering happy path (201 Created), already member (400), pending request already exists (400), unknown gym (404), and getRequestType.
   - `UserValidationsTest.java`: Updated with 10 unit tests covering all validation methods.

## 3. Verification & Compliance
- **Unit Test Execution:** All 24 unit tests across `FetchGymByCodeServiceTest`, `SubmitJoinRequestServiceTest`, `UserHelperTest`, and `UserValidationsTest` passed in 0.35 seconds (`BUILD SUCCESS`).
- **Protected Core Invariant:** All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
- **Git Safety:** Zero commits or pushes executed.
- **API Payloads:** Documented in `docs/payloads.md` section 3.4 with 13 mandatory fields and copy-pasteable cURL example.
