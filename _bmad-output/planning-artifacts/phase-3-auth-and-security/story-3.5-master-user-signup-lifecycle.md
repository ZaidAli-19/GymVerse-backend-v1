---
id: "story-3.5"
epic_id: "epic-phase-3-security-auth"
title: "Master User 3-Step Signup Lifecycle (SIGNUP_INITIATE, SIGNUP_VERIFY, SIGNUP_COMPLETE)"
status: "planning"
type: "auth-lifecycle"
created_at: "2026-11-17"
dependencies: ["story-3.1", "story-3.2", "story-3.4"]
files_to_modify:
  - "src/main/java/com/company/GymVerse/auth/request/enums/AuthRequestType.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/auth/request/SignupRequest.java"
  - "src/main/java/com/company/GymVerse/auth/validations/AuthValidations.java"
  - "src/main/java/com/company/GymVerse/auth/validations/AuthValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/auth/service/SignupInitiateService.java"
  - "src/main/java/com/company/GymVerse/auth/service/SignupVerifyService.java"
  - "src/main/java/com/company/GymVerse/auth/service/SignupCompleteService.java"
  - "src/test/java/com/company/GymVerse/auth/SignupLifecycleIntegrationTest.java"
---

# Story 3.5: Master User 3-Step Signup Lifecycle (`SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Implement single-responsibility operation beans implementing `AuthOperation`.
> 4. Separate validation logic into `AuthValidationsImpl`; use handwritten mappers only.
> 5. Store active accounts in central `user_accounts` table under `centralTransactionManager`.
> 6. Maintain task logging in `antigravity/story-3.5-master-user-signup-lifecycle.md`.

---

## 1. User Story

**As a** prospective gym owner, trainer, or platform member,  
**I want** a secure 3-step registration flow that takes my name, email, and phone, verifies my email address via OTP, and lets me set a secure password,  
**So that** my master platform account is securely verified and provisioned in the central database before issuing my `MASTER_ACCOUNT` access and refresh tokens.

---

## 2. Technical Context & Requirements

1. **Signup DTO & Validations (`SignupRequest.java` & `AuthValidationsImpl.java`)**:
   - `SignupRequest` properties:
     - `String firstName`, `String lastName`, `String email`, `String phoneNumber`, `String otp`, `String password`.
   - `AuthValidations`:
     - Email format validation (standard RFC 5322 regex).
     - Phone format validation (E.164 international format or standard digit string).
     - Name constraints (2–50 chars, no illegal characters).
     - Password strength validation: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special symbol (`@$!%*?&#`).

2. **Step 1: SIGNUP_INITIATE (`SignupInitiateService`)**:
   - Implements `AuthOperation` with `getAuthRequestType() == AuthRequestType.SIGNUP_INITIATE`.
   - Validates input profile (`firstName`, `lastName`, `email`, `phoneNumber`).
   - Checks `userAccountRepository.existsByEmail(email)`:
     - If exists, throws `ConflictException("An account with email " + email + " already exists")`.
   - Calls `otpService.sendOtp(email, OtpPurpose.SIGNUP, firstName + " " + lastName)`.
   - Caches pending registration metadata in Redis with 15m TTL (`gymverse:signup:pending:{email}`) or relies on client submission in Step 3.
   - Returns `AuthResponse` with `message: "Verification OTP has been sent to your email"`.

3. **Step 2: SIGNUP_VERIFY (`SignupVerifyService`)**:
   - Implements `AuthOperation` with `getAuthRequestType() == AuthRequestType.SIGNUP_VERIFY`.
   - Validates `email` and `otp`.
   - Calls `otpService.verifyOtp(email, otp, OtpPurpose.SIGNUP)`.
   - Returns `AuthResponse` with `message: "Email successfully verified. Please submit your password to complete registration."`.

4. **Step 3: SIGNUP_COMPLETE (`SignupCompleteService`)**:
   - Implements `AuthOperation` with `getAuthRequestType() == AuthRequestType.SIGNUP_COMPLETE`.
   - Validates `email` and `password`.
   - Checks `otpService.isOtpVerified(email, OtpPurpose.SIGNUP)`:
     - If false, throws `BadRequestException("Email has not been verified. Please verify your email first.")`.
   - Validates password strength via `authValidations.validatePasswordStrength(password)`.
   - Hashes password using `passwordEncoder.encode(password)`.
   - Builds `UserAccount`:
     - `email`: verified email
     - `firstName`, `lastName`, `phoneNumber`: extracted from request or cached profile
     - `password`: encoded password
     - `emailVerified`: `true`
     - `isPlatformAdmin`: `false` (strictly prevented from admin privilege escalation)
     - `status`: `UserAccountStatus.ACTIVE`
   - Persists via `userAccountRepository.save(account)` under `centralTransactionManager`.
   - Consumes verified token via `otpService.consumeVerifiedOtp(email, OtpPurpose.SIGNUP)`.
   - Generates tokens:
     - `accessToken = jwtUtil.generateMasterAccessToken(savedAccount)`
     - `refreshToken = jwtUtil.generateMasterRefreshToken(savedAccount)`
   - Returns `AuthResponse` with tokens, user summary, and message: `"Account registration completed successfully"`.

---

## 3. Verification & Acceptance Criteria

1. **Lifecycle Integration Test (`SignupLifecycleIntegrationTest.java`)**:
   - Verify `SIGNUP_INITIATE` throws `ConflictException` if email exists.
   - Verify `SIGNUP_INITIATE` triggers OTP sending when email is new.
   - Verify `SIGNUP_VERIFY` rejects wrong or expired OTP with HTTP 400.
   - Verify `SIGNUP_VERIFY` creates verified state when OTP matches.
   - Verify `SIGNUP_COMPLETE` fails if email was not verified.
   - Verify `SIGNUP_COMPLETE` rejects weak passwords (e.g. "password123").
   - Verify `SIGNUP_COMPLETE` successfully saves active `UserAccount` with hashed password and returns `MASTER_ACCOUNT` access + refresh tokens.
