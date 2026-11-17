# Task Log: Story 3.4 — Enterprise Redis OtpService with Rate Limiting & Cooldown Engine

- **Story ID:** `story-3.4`
- **Phase:** Phase 3 — Core Security & Master DB Authentication
- **Timestamp:** 2026-11-17T15:46:00+05:30
- **Author:** Antigravity AI Assistant

---

## 1. Objective & Scope

Implement a dedicated, enterprise-grade Redis-backed `OtpService` with configurable TTLs, 60-second resend cooldown rate limiting, and 5-attempt brute-force protection to standardize OTP generation, dispatch via Kafka, and verification across all platform flows (Signup, Login, Password Reset).

---

## 2. Technical Implementation Details

### 2.1 Configuration Properties (`src/main/java/com/company/GymVerse/otp/config/OtpConfigProperties.java`)
- Bound to prefix `gymverse.otp` via `@ConfigurationProperties`:
  - `length`: 6 digits
  - `ttl`: 10 minutes (`Duration.ofMinutes(10)`)
  - `resendCooldown`: 60 seconds (`Duration.ofSeconds(60)`)
  - `maxAttempts`: 5 attempts
  - `verifiedTtl`: 15 minutes (`Duration.ofMinutes(15)`)
  - Namespaced key prefixes:
    - `prefixCode`: `"gymverse:otp:code:"`
    - `prefixCooldown`: `"gymverse:otp:cooldown:"`
    - `prefixAttempts`: `"gymverse:otp:attempts:"`
    - `prefixVerified`: `"gymverse:otp:verified:"`

### 2.2 Domain Entities & Discriminators
- **Purpose Enum:** [`OtpPurpose.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/otp/util/OtpPurpose.java) (`SIGNUP`, `LOGIN`, `FORGOT_PASSWORD`).
- **Domain Exceptions (organized in `exceptions/otp/`):**
  - [`OtpRateLimitException.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/otp/OtpRateLimitException.java) (HTTP 429 Too Many Requests)
  - [`OtpInvalidException.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/otp/OtpInvalidException.java) (HTTP 400 Bad Request)
  - [`OtpExpiredException.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/otp/OtpExpiredException.java) (HTTP 400 Bad Request)
- **Central Exception Translation:**
  - Updated [`GlobalExceptionHandler.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/handler/GlobalExceptionHandler.java) to catch `OtpRateLimitException`, `OtpInvalidException`, and `OtpExpiredException`, mapping them to standardized `ErrorResponse` payloads.

### 2.3 Core Service Contract & Implementation
- **Interface:** [`OtpService.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/otp/service/OtpService.java)
  - `sendOtp(String email, OtpPurpose purpose, String recipientName)`
  - `resendOtp(String email, OtpPurpose purpose, String recipientName)`
  - `boolean verifyOtp(String email, String otp, OtpPurpose purpose)`
  - `boolean isOtpVerified(String email, OtpPurpose purpose)`
  - `void consumeVerifiedOtp(String email, OtpPurpose purpose)`
- **Implementation:** [`OtpServiceImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/otp/serviceImpl/OtpServiceImpl.java)
  - **Cooldown Rate Limiting:** Verifies if `cooldownKey` exists in Redis; if present, calculates remaining seconds and throws `OtpRateLimitException`.
  - **Secure Code Generation:** Generates 6-digit numeric codes using `SecureRandom`.
  - **Redis Storage:** Atomically sets code (10m TTL), cooldown (60s TTL), and resets attempt counter (10m TTL).
  - **Kafka Asynchronous Dispatch:** Publishes strongly typed `OtpEmailEvent` to `otp-email-topic`.
  - **Brute-Force Attempt Limiting:**
    - Checks stored code; throws `OtpExpiredException` if missing.
    - Increments attempt counter via Redis `increment`.
    - If attempts exceed 5, evicts the code from Redis and throws `OtpInvalidException`.
    - If code is invalid, computes remaining attempts and throws `OtpInvalidException`.
    - On success, evicts code and attempts keys, writes verified token state (`verifiedKey = "true"` with 15m TTL), and returns true.
  - **Verified State Lifecycle:** `isOtpVerified` checks active verified state; `consumeVerifiedOtp` evicts the token upon downstream operation completion.

### 2.4 Configuration Alignment
- **`application.yaml`**: Added `gymverse.otp.*` block.
- **`application-test.yaml`**: Added `gymverse.otp.*` block.

---

## 3. Verification & Test Evidence

### 3.1 Unit Testing: `OtpServiceImplTest.java` (12 Tests Passing)
- `sendOtp_WhenNoCooldown_SavesToRedisAndPublishesKafka`: Validates OTP creation, Redis persistence (code, cooldown, attempts), and Kafka event publication.
- `sendOtp_WhenCooldownActive_ThrowsOtpRateLimitException`: Validates cooldown rate limiting and remaining seconds message.
- `resendOtp_DelegatesToSendOtp`: Validates resend delegation.
- `verifyOtp_WhenNoStoredOtp_ThrowsOtpExpiredException`: Validates expired OTP handling.
- `verifyOtp_WhenOtpMatches_SetsVerifiedToken`: Validates successful verification, cleanup of code/attempts, and setting verified key with 15m TTL.
- `verifyOtp_WhenOtpMismatched_IncrementsAttemptsAndThrows`: Validates remaining attempt count reporting.
- `verifyOtp_When5thAttemptFails_EvictsOtpAndLocksOut`: Validates code lockout and eviction after 5 failed attempts.
- `verifyOtp_WhenAttemptsAlreadyExceeded_EvictsOtp`: Validates guard against post-lockout verification attempts.
- `isOtpVerified_WhenPresent_ReturnsTrue`: Validates verification status check.
- `isOtpVerified_WhenMissing_ReturnsFalse`: Validates negative check.
- `consumeVerifiedOtp_DeletesKey`: Validates single-use consumption of verified state.
- `methods_RejectNullOrBlankEmail`: Validates input validation.

### 3.2 Full Test Suite Status
- **Total Tests Run:** 108
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Status:** BUILD SUCCESS

---

## 4. Git Policy Compliance
- **Rule 1 Strictly Enforced:** Zero autonomous `git commit` or `git push` executed. All changes remain unstaged in local working tree.
