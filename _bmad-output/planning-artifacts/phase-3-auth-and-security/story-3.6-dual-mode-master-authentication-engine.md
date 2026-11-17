---
id: "story-3.6"
epic_id: "epic-phase-3-security-auth"
title: "Dual-Mode Master Authentication Engine (LOGIN_PASSWORD, LOGIN_OTP_INITIATE, LOGIN_OTP_VERIFY, REFRESH_TOKEN)"
status: "planning"
type: "auth-login"
created_at: "2026-11-17"
dependencies: ["story-3.1", "story-3.2", "story-3.4", "story-3.5"]
files_to_modify:
  - "src/main/java/com/company/GymVerse/auth/request/enums/AuthRequestType.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/auth/request/LoginRequest.java"
  - "src/main/java/com/company/GymVerse/auth/service/LoginPasswordService.java"
  - "src/main/java/com/company/GymVerse/auth/service/LoginOtpInitiateService.java"
  - "src/main/java/com/company/GymVerse/auth/service/LoginOtpVerifyService.java"
  - "src/main/java/com/company/GymVerse/auth/service/RefreshTokenService.java"
  - "src/test/java/com/company/GymVerse/auth/LoginLifecycleIntegrationTest.java"
---

# Story 3.6: Dual-Mode Master Authentication Engine (`LOGIN_PASSWORD`, `LOGIN_OTP_INITIATE`, `LOGIN_OTP_VERIFY`, `REFRESH_TOKEN`)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Authenticate password logins using Spring Security's `AuthenticationManager`.
> 4. Guard against locked/suspended accounts: only accounts with `status == ACTIVE` are allowed to log in.
> 5. Refresh token exchange must strictly validate that incoming token has claim `type == "REFRESH"` and `tokenType == "MASTER_ACCOUNT"`.
> 6. Maintain task logging in `antigravity/story-3.6-dual-mode-master-authentication-engine.md`.

---

## 1. User Story

**As an** existing registered master user,  
**I want** to log in using either my email and password or a passwordless one-time verification code, and renew my session using a refresh token,  
**So that** I can access platform resources flexibly, securely, and seamlessly without constant re-authentications.

---

## 2. Technical Context & Requirements

1. **Login DTO (`com.company.GymVerse.auth.request.LoginRequest`)**:
   - `String email`
   - `String password`
   - `String otp`
   - `String refreshToken`

2. **Operation 1: Password Login (`LoginPasswordService`)**:
   - Implements `AuthOperation` (`AuthRequestType.LOGIN_PASSWORD`).
   - Invokes `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`.
   - If invalid credentials, Spring throws `BadCredentialsException` (translated to HTTP 401).
   - Retrieves `UserAccount` from `userAccountRepository.findByEmail(email)`.
   - Checks account status:
     - If `status != UserAccountStatus.ACTIVE`: throws `BadRequestException("Account is not active. Status: " + status)`.
   - Generates tokens:
     - `accessToken = jwtUtil.generateMasterAccessToken(account)`
     - `refreshToken = jwtUtil.generateMasterRefreshToken(account)`
   - Returns `AuthResponse` with tokens, user summary, and message: `"Login successful"`.

3. **Operation 2: OTP Login Initiate (`LoginOtpInitiateService`)**:
   - Implements `AuthOperation` (`AuthRequestType.LOGIN_OTP_INITIATE`).
   - Retrieves `UserAccount` from `userAccountRepository.findByEmail(email)`:
     - If missing: throws `ResourceNotFoundException("No account registered with email " + email)`.
   - Checks `status == UserAccountStatus.ACTIVE`.
   - Triggers OTP: `otpService.sendOtp(email, OtpPurpose.LOGIN, account.getFirstName() + " " + account.getLastName())`.
   - Returns `AuthResponse` with message: `"Login verification OTP sent to your email"`.

4. **Operation 3: OTP Login Verify (`LoginOtpVerifyService`)**:
   - Implements `AuthOperation` (`AuthRequestType.LOGIN_OTP_VERIFY`).
   - Verifies OTP via `otpService.verifyOtp(email, otp, OtpPurpose.LOGIN)`.
   - Consumes verified status via `otpService.consumeVerifiedOtp(email, OtpPurpose.LOGIN)`.
   - Retrieves `UserAccount` from database.
   - Generates and returns `MASTER_ACCOUNT` Access & Refresh tokens.

5. **Operation 4: Token Refresh Exchange (`RefreshTokenService`)**:
   - Implements `AuthOperation` (`AuthRequestType.REFRESH_TOKEN`).
   - Validates that `refreshToken` is provided and non-blank.
   - Parses token via `jwtUtil`:
     - Verifies token signature and expiration.
     - Confirms `jwtUtil.extractType(refreshToken).equals("REFRESH")`.
     - Confirms `jwtUtil.extractTokenType(refreshToken).equals("MASTER_ACCOUNT")`.
   - Extracts subject (`userId = Long.parseLong(jwtUtil.extractSubject(refreshToken))`).
   - Retrieves `UserAccount` by ID. Checks `status == UserAccountStatus.ACTIVE`.
   - Issues fresh `accessToken` (and optional rotated `refreshToken`).
   - Returns `AuthResponse` with new tokens.

---

## 3. Verification & Acceptance Criteria

1. **Integration Tests (`LoginLifecycleIntegrationTest.java`)**:
   - Verify `LOGIN_PASSWORD` succeeds with valid credentials and returns valid JWTs.
   - Verify `LOGIN_PASSWORD` fails with HTTP 401 on incorrect password.
   - Verify `LOGIN_PASSWORD` fails with HTTP 400 on suspended/inactive account.
   - Verify `LOGIN_OTP_INITIATE` and `LOGIN_OTP_VERIFY` complete passwordless login successfully.
   - Verify `REFRESH_TOKEN` successfully issues new access token from valid refresh token.
   - Verify `REFRESH_TOKEN` rejects an access token passed in as a refresh token with HTTP 401.
