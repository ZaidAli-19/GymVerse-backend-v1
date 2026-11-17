# Task Log: Story 3.6 — Dual-Mode Master Authentication Engine (`LOGIN_PASSWORD`, `LOGIN_OTP_INITIATE`, `LOGIN_OTP_VERIFY`, `REFRESH_TOKEN`)

- **Story ID:** `story-3.6`
- **Phase:** Phase 3 — Core Security & Master DB Authentication
- **Timestamp:** 2026-11-17T16:17:00+05:30
- **Author:** Antigravity AI Assistant

---

## 1. Objective & Scope

Implement dual-mode authentication (password credentials or passwordless OTP) and refresh token session renewal for Master DB user accounts, enforcing strict active account status checks and `MASTER_ACCOUNT` token typing.

---

## 2. Technical Implementation Details

### 2.1 Request & Validation Enhancements
- **Request DTO:** [`LoginRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/request/LoginRequest.java) (`email`, `password`, `otp`, `refreshToken`).
- **Unified Request Envelope:** Updated [`AuthRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/request/AuthRequest.java) to include `loginRequest`.
- **Validation Engine:** Extended [`AuthValidations.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/validations/AuthValidations.java) & [`AuthValidationsImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/auth/validations/AuthValidationsImpl.java):
  - `validateLoginPassword`: verifies email format and non-empty password.
  - `validateLoginOtpInitiate`: verifies email format.
  - `validateLoginOtpVerify`: verifies email format and 6-digit numeric OTP.
  - `validateRefreshToken`: verifies non-empty refresh token.

### 2.2 Concrete Operations
1. **`LoginPasswordService` (`LOGIN_PASSWORD`):**
   - Authenticates credentials via Spring Security's `AuthenticationManager.authenticate(...)`.
   - Loads `UserAccount` from central database.
   - Guard check: verifies `status == UserAccountStatus.ACTIVE` (rejects `SUSPENDED` or `DEACTIVATED` accounts with HTTP 400).
   - Generates and returns `MASTER_ACCOUNT` access and refresh tokens.
2. **`LoginOtpInitiateService` (`LOGIN_OTP_INITIATE`):**
   - Verifies account existence and `ACTIVE` status in central database.
   - Triggers `otpService.sendOtp(normalizedEmail, OtpPurpose.LOGIN, recipientName)` with 60s cooldown enforcement.
   - Returns confirmation message.
3. **`LoginOtpVerifyService` (`LOGIN_OTP_VERIFY`):**
   - Verifies submitted OTP against Redis via `otpService.verifyOtp(...)`.
   - Consumes verified token state via `otpService.consumeVerifiedOtp(...)`.
   - Verifies account is active, then issues `MASTER_ACCOUNT` access and refresh tokens.
4. **`RefreshTokenService` (`REFRESH_TOKEN`):**
   - Verifies token claims: `type == "REFRESH"` and `tokenType == "MASTER_ACCOUNT"`.
   - Verifies token signature and expiration via `JwtUtil`.
   - Guards against access tokens being passed in as refresh tokens (`BadCredentialsException` -> HTTP 401).
   - Verifies account is `ACTIVE`.
   - Rotates and issues fresh `MASTER_ACCOUNT` token pair.

### 2.3 Exception Handling Alignment
- Updated [`GlobalExceptionHandler.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/handler/GlobalExceptionHandler.java) to catch `AuthenticationException` and `JwtException`, mapping them directly to structured HTTP 401 Unauthorized `ErrorResponse` payloads.
- Harmonized bean definitions: retained [`SecurityCryptoConfig.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/config/SecurityCryptoConfig.java) as the single source for `PasswordEncoder`.

---

## 3. Verification & Test Evidence

### 3.1 Integration Testing: `LoginLifecycleIntegrationTest.java` (10 Tests Passing)
- `authFactory_ResolvesOperations`: Confirms `AuthFactory` resolves `LOGIN_PASSWORD`, `LOGIN_OTP_INITIATE`, `LOGIN_OTP_VERIFY`, `REFRESH_TOKEN`.
- `loginPassword_Success`: Confirms authentication manager delegation, user status check, and token generation.
- `loginPassword_WhenBadCredentials_ThrowsBadCredentialsException`: Confirms invalid passwords trigger `BadCredentialsException`.
- `loginPassword_WhenSuspended_ThrowsBadRequestException`: Confirms suspended accounts are blocked.
- `loginOtpInitiate_Success`: Confirms OTP dispatch for valid active accounts.
- `loginOtpInitiate_WhenNotFound_ThrowsResourceNotFoundException`: Confirms non-existent emails return 404.
- `loginOtpVerify_Success`: Confirms valid OTP issuance of tokens and verified state cleanup.
- `refreshToken_Success`: Confirms valid refresh token issues fresh access/refresh token pair.
- `refreshToken_WhenAccessTokenProvided_ThrowsBadCredentialsException`: Confirms access tokens cannot be used to refresh.
- `refreshToken_WhenExpired_ThrowsBadCredentialsException`: Confirms expired refresh tokens are rejected.

### 3.2 Full Test Suite Status
- **Total Tests Run:** 131
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Status:** BUILD SUCCESS

---

## 4. Git Policy Compliance
- **Rule 1 Strictly Enforced:** Zero autonomous `git commit` or `git push` executed. All changes remain unstaged in local working tree.
