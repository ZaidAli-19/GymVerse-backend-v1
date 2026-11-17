# Task Log: Story 3.5 — Master User 3-Step Signup Lifecycle (`SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`)

- **Story ID:** `story-3.5`
- **Phase:** Phase 3 — Core Security & Master DB Authentication
- **Timestamp:** 2026-11-17T15:54:45+05:30
- **Author:** Antigravity AI Assistant

---

## 1. Objective & Scope

Implement the secure 3-step registration lifecycle for central Master DB user accounts (`SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`) using single-responsibility operation beans (`AuthOperation`), dedicated defensive validation components (`AuthValidations`), Redis OTP verification tokens, password hashing with `BCryptPasswordEncoder`, and issuing initial `MASTER_ACCOUNT` JWT tokens.

---

## 2. Technical Implementation Details

### 2.1 Unified Request & Response Envelopes
- **Discriminator Enum:** [`AuthRequestType.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/request/enums/AuthRequestType.java) (`SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`, etc.).
- **Sub-Request:** [`SignupRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/request/SignupRequest.java) (`firstName`, `lastName`, `email`, `phoneNumber`, `otp`, `password`).
- **Request Envelope:** [`AuthRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/request/AuthRequest.java) with `authRequestType` and `signupRequest`.
- **Response Envelopes:**
  - [`UserSummaryResponse.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/response/UserSummaryResponse.java) (`id`, `email`, `firstName`, `lastName`, `phoneNumber`, `status`).
  - [`AuthResponse.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/response/AuthResponse.java) (`message`, `accessToken`, `refreshToken`, `user`).

### 2.2 Factory & Handler Contracts
- **Contract:** [`AuthOperation.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/factory/AuthOperation.java) (`getRequestType()`, `execute(AuthRequest)`).
- **Factory:** [`AuthFactory.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/factory/AuthFactory.java) with constructor-injected operations and Map lookup.

### 2.3 Defensive Validations
- **Interface & Implementation:** [`AuthValidations.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/validations/AuthValidations.java) & [`AuthValidationsImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/validations/AuthValidationsImpl.java).
- Enforces:
  - RFC 5322 email regex.
  - Name length (2–50 characters).
  - Phone format (optional, E.164 / digits).
  - OTP format (strict 6 digits).
  - Password complexity: min 8 chars, 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.

### 2.4 Concrete Operation Services
1. **`SignupInitiateService` (`SIGNUP_INITIATE`):**
   - Validates input profile (`firstName`, `lastName`, `email`, `phoneNumber`).
   - Checks `userAccountRepository.existsByEmail(normalizedEmail)`. Throws `ConflictException` if email exists.
   - Triggers `otpService.sendOtp(normalizedEmail, OtpPurpose.SIGNUP, recipientName)`.
   - Caches pending registration metadata in Redis with 15m TTL (`gymverse:signup:pending:{email}`).
   - Returns confirmation `AuthResponse`.
2. **`SignupVerifyService` (`SIGNUP_VERIFY`):**
   - Validates email and 6-digit OTP.
   - Delegates to `otpService.verifyOtp(normalizedEmail, otp, OtpPurpose.SIGNUP)`.
   - Returns verification confirmation `AuthResponse`.
3. **`SignupCompleteService` (`SIGNUP_COMPLETE`):**
   - Validates password strength and verified OTP status via `otpService.isOtpVerified(email, OtpPurpose.SIGNUP)`.
   - Re-verifies email uniqueness.
   - Resolves profile (from request or cached pending profile).
   - Encodes password via `PasswordEncoder.encode(password)`.
   - Sets security invariants: `emailVerified = true`, `isPlatformAdmin = false` (strict privilege escalation guard), `status = ACTIVE`.
   - Persists `UserAccount` under `centralTransactionManager`.
   - Consumes verified OTP token via `otpService.consumeVerifiedOtp(...)` and evicts pending cache.
   - Issues `MASTER_ACCOUNT` access and refresh tokens via `JwtUtil`.
   - Returns tokens, user summary, and success message.

---

## 3. Verification & Test Evidence

### 3.1 Unit Testing: `AuthValidationsTest.java` (5 Tests Passing)
- `validateEmail_ValidAndInvalid`: Confirms valid RFC patterns and rejects malformed emails.
- `validateName_EnforcesLength`: Confirms length bounds (2-50 chars).
- `validatePasswordStrength_ValidAndInvalid`: Confirms uppercase, lowercase, digit, and special character requirements.
- `validateOtp_ValidAndInvalid`: Confirms exact 6-digit numeric constraint.
- `validateSignupInitiate_Success`: Confirms complete request envelope validation.

### 3.2 Integration Testing: `SignupLifecycleIntegrationTest.java` (8 Tests Passing)
- `authFactory_RegistersOperations`: Confirms `AuthFactory` resolves `SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`.
- `signupInitiate_WhenEmailExists_ThrowsConflictException`: Confirms duplicate email detection.
- `signupInitiate_WhenEmailAvailable_SendsOtpAndCachesProfile`: Confirms OTP dispatch and profile caching.
- `signupVerify_WhenOtpInvalid_PropagatesException`: Confirms invalid OTP rejection.
- `signupVerify_WhenOtpValid_ReturnsSuccess`: Confirms valid OTP success response.
- `signupComplete_WhenNotVerified_ThrowsBadRequestException`: Confirms guard against unverified email completion.
- `signupComplete_WhenPasswordWeak_ThrowsBadRequestException`: Confirms weak password rejection.
- `signupComplete_WhenVerifiedAndPasswordValid_ProvisionsUserAndIssuesTokens`: Confirms complete account provisioning, hashed password, `isPlatformAdmin = false`, token issuance, and OTP cleanup.

### 3.3 Full Test Suite Status
- **Total Tests Run:** 121
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Status:** BUILD SUCCESS

---

## 4. Git Policy Compliance
- **Rule 1 Strictly Enforced:** Zero autonomous `git commit` or `git push` executed. All changes remain unstaged in local working tree.
