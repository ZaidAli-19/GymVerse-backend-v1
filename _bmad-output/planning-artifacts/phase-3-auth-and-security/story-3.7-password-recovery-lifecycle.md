---
id: "story-3.7"
epic_id: "epic-phase-3-security-auth"
title: "Master User Password Recovery Lifecycle (FORGOT_PASSWORD_INITIATE, FORGOT_PASSWORD_VERIFY, FORGOT_PASSWORD_RESET)"
status: "implemented"
type: "password-recovery"
created_at: "2026-11-17"
dependencies: ["story-3.1", "story-3.4", "story-3.5"]
files_to_modify:
  - "src/main/java/com/company/GymVerse/auth/request/enums/AuthRequestType.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/auth/request/ForgotPasswordRequest.java"
  - "src/main/java/com/company/GymVerse/auth/service/ForgotPasswordInitiateService.java"
  - "src/main/java/com/company/GymVerse/auth/service/ForgotPasswordVerifyService.java"
  - "src/main/java/com/company/GymVerse/auth/service/ForgotPasswordResetService.java"
  - "src/test/java/com/company/GymVerse/auth/ForgotPasswordLifecycleIntegrationTest.java"
---

# Story 3.7: Master User Password Recovery Lifecycle (`FORGOT_PASSWORD_INITIATE`, `FORGOT_PASSWORD_VERIFY`, `FORGOT_PASSWORD_RESET`)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Password reset must require successful OTP verification before accepting a new password.
> 4. Ensure password complexity validations are enforced on the new password.
> 5. Update central `user_accounts` table under `centralTransactionManager`.
> 6. Maintain task logging in `antigravity/story-3.7-password-recovery-lifecycle.md`.

---

## 1. User Story

**As a** master platform user who has forgotten their password,  
**I want** to request a password reset via email OTP, verify the OTP, and set a new secure password,  
**So that** I can recover access to my account without administrative intervention.

---

## 2. Technical Context & Requirements

1. **Recovery Request DTO (`com.company.GymVerse.auth.request.ForgotPasswordRequest`)**:
   - `String email`
   - `String otp`
   - `String newPassword`

2. **Step 1: FORGOT_PASSWORD_INITIATE (`ForgotPasswordInitiateService`)**:
   - Implements `AuthOperation` (`AuthRequestType.FORGOT_PASSWORD_INITIATE`).
   - Retrieves `UserAccount` from `userAccountRepository.findByEmail(email)`.
   - If user does not exist, throw `ResourceNotFoundException("No account registered with email " + email)`.
   - Checks `account.getStatus() == UserAccountStatus.ACTIVE`.
   - Triggers OTP dispatch: `otpService.sendOtp(email, OtpPurpose.FORGOT_PASSWORD, account.getFirstName() + " " + account.getLastName())`.
   - Returns `AuthResponse` with message: `"Password reset OTP has been sent to your email"`.

3. **Step 2: FORGOT_PASSWORD_VERIFY (`ForgotPasswordVerifyService`)**:
   - Implements `AuthOperation` (`AuthRequestType.FORGOT_PASSWORD_VERIFY`).
   - Verifies OTP via `otpService.verifyOtp(email, otp, OtpPurpose.FORGOT_PASSWORD)`.
   - Returns `AuthResponse` with message: `"OTP verified successfully. Please submit your new password."`.

4. **Step 3: FORGOT_PASSWORD_RESET (`ForgotPasswordResetService`)**:
   - Implements `AuthOperation` (`AuthRequestType.FORGOT_PASSWORD_RESET`).
   - Validates that `otpService.isOtpVerified(email, OtpPurpose.FORGOT_PASSWORD)` is `true`.
     - If false, throws `BadRequestException("Password reset authorization missing or expired. Please verify OTP first.")`.
   - Validates new password strength via `authValidations.validatePasswordStrength(newPassword)`.
   - Retrieves `UserAccount` from `userAccountRepository.findByEmail(email)`.
   - Encodes password via `passwordEncoder.encode(newPassword)`.
   - Updates `account.setPassword(encodedPassword)` and persists via `userAccountRepository.save(account)` under `centralTransactionManager`.
   - Consumes verified status via `otpService.consumeVerifiedOtp(email, OtpPurpose.FORGOT_PASSWORD)`.
   - Returns `AuthResponse` with message: `"Password reset successfully. Please login with your new credentials."`.

---

## 3. Verification & Acceptance Criteria

1. **Integration Test (`ForgotPasswordLifecycleIntegrationTest.java`)**:
   - Verify `FORGOT_PASSWORD_INITIATE` rejects unregistered emails with HTTP 404.
   - Verify `FORGOT_PASSWORD_INITIATE` generates OTP and sets cooldown for registered user.
   - Verify `FORGOT_PASSWORD_VERIFY` rejects invalid OTP.
   - Verify `FORGOT_PASSWORD_RESET` fails if OTP was never verified.
   - Verify `FORGOT_PASSWORD_RESET` rejects weak new passwords.
   - Verify `FORGOT_PASSWORD_RESET` successfully updates the password in `user_accounts` and subsequent `LOGIN_PASSWORD` succeeds with the new password.
