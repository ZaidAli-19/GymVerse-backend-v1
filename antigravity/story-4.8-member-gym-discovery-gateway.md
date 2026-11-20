# Story 4.8 Task Log: Member Gym Discovery Gateway (`FETCH_GYM_BY_CODE`)

## 1. Context & Operational Goals
- **Objective:** Enable central master users on mobile/web to discover and inspect gym metadata by providing a unique `gymCode` (QR code scanner or manual entry) before submitting a membership join request.
- **Architectural Constraints:**
  - Exclusively routed through `@PostMapping("/api/user")` via `UserRequestType.FETCH_GYM_BY_CODE`.
  - Strictly requires a valid `MASTER_ACCOUNT` token (enforced by `UserValidations.validateMasterContext()`).
  - Validates gym existence, `ACTIVE` status, and SaaS subscription health (`ACTIVE` or `TRIAL`).
  - Zero reflection, handwritten `UserMapperImpl`.
  - Clean separation: domain validations in `UserValidations`, reusable boilerplate in `UserHelper`.

## 2. Changes Implemented
1. **DTOs & Enums**:
   - `FetchGymByCodeRequest.java`: Request carrying `gymCode`.
   - `FetchGymByCodeResponse.java`: Response payload carrying gym metadata (`tenantId`, `gymCode`, `gymName`, `address`, `city`, `phoneNumber`, `contactEmail`, `logoUrl`, `status`, `subscriptionStatus`).
   - `UserRequestType.java`: Added `FETCH_GYM_BY_CODE`.
   - `UserRequest.java`: Added `FetchGymByCodeRequest fetchGymByCodeRequest;`.
   - `UserResponse.java`: Added `FetchGymByCodeResponse fetchGymByCodeResponse;`.
2. **Helper Layer (`UserHelper.java`)**:
   - `getAuthenticatedUserAccount()`: Resolves authenticated `UserAccount` from `SecurityContextHolder`.
   - `normalizeGymCode(gymCode)`: Standardizes gym code (trimmed and upper-cased).
   - `getLatestSubscriptionStatus(gymTenantId)`: Queries latest SaaS subscription status from Central DB.
3. **Validation Layer (`UserValidations` & `UserValidationsImpl`)**:
   - `validateFetchGymByCode(request)`: Ensures request envelope, payload, and `gymCode` are non-null and non-blank.
   - `validateGymActiveForJoin(gymTenant, subscriptionStatus)`: Rejects suspended gyms or gyms with expired/cancelled SaaS subscriptions.
4. **Mapper Layer (`UserMapper` & `UserMapperImpl`)**:
   - `toFetchGymByCodeResponse(gymTenant, subscriptionStatus)`: Handwritten conversion.
   - `toFetchGymByCodeUserResponse(message, response)`: Wraps into unified `UserResponse`.
5. **Service Layer (`FetchGymByCodeService.java`)**:
   - Implements `UserOperation` for `FETCH_GYM_BY_CODE`.
   - `@Transactional(readOnly = true, transactionManager = "centralTransactionManager")`.
   - Coordinates `UserValidations`, `GymTenantRepository`, `UserHelper`, and `UserMapper`.
6. **Tests**:
   - `FetchGymByCodeServiceTest.java`: 4 unit tests covering happy path, missing gym (404), and suspended gym (400).
   - `UserHelperTest.java`: 5 unit tests covering user account extraction, gym code normalization, and subscription resolution.
   - `UserValidationsTest.java`: 7 unit tests covering input validations, code constraints, and gym status checks.

## 3. Verification & Compliance
- **Unit Test Execution:** All 16 unit tests across `FetchGymByCodeServiceTest`, `UserHelperTest`, and `UserValidationsTest` passed in 0.29 seconds (`BUILD SUCCESS`).
- **Protected Core Invariant:** All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
- **Git Policy:** No autonomous commits or pushes executed.
- **API Payloads:** Documented in `docs/payloads.md` section 3.3 with 13 mandatory fields and copy-pasteable cURL example.
