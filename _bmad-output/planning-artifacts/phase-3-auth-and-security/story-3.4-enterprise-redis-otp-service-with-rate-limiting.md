---
id: "story-3.4"
epic_id: "epic-phase-3-security-auth"
title: "Enterprise Redis OtpService with Rate Limiting & Cooldown Engine"
status: "planning"
type: "otp-domain-service"
created_at: "2026-11-17"
dependencies: ["story-3.3"]
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
files_to_create:
  - "src/main/java/com/company/GymVerse/otp/config/OtpConfigProperties.java"
  - "src/main/java/com/company/GymVerse/otp/util/OtpPurpose.java"
  - "src/main/java/com/company/GymVerse/otp/service/OtpService.java"
  - "src/main/java/com/company/GymVerse/otp/serviceImpl/OtpServiceImpl.java"
  - "src/main/java/com/company/GymVerse/exceptions/otp/OtpRateLimitException.java"
  - "src/main/java/com/company/GymVerse/exceptions/otp/OtpInvalidException.java"
  - "src/main/java/com/company/GymVerse/exceptions/otp/OtpExpiredException.java"
  - "src/test/java/com/company/GymVerse/otp/OtpServiceImplTest.java"
---

# Story 3.4: Enterprise Redis OtpService with Rate Limiting & Cooldown Engine

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Do NOT generate or store OTPs directly in controller or operation services. Encapsulate all OTP logic within `OtpService`.
> 4. Use Redis for storage with dedicated namespacing and externalized `@ConfigurationProperties`.
> 5. Implement strict resend cooldown rate limiting (default 60s) and max attempt throttling (default 5 attempts).
> 6. Maintain task logging in `antigravity/story-3.4-enterprise-redis-otp-service-with-rate-limiting.md`.

---

## 1. User Story

**As a** platform security engineer,  
**I want** a dedicated, enterprise-grade Redis-backed `OtpService` with configurable TTLs, resend cooldown rate limiting, and brute-force attempt throttling,  
**So that** OTP generation and verification across all platform flows are standardized, secure against SMS/email bombing, and resilient against brute-force attacks.

---

## 2. Technical Context & Requirements

1. **Configuration Properties (`com.company.GymVerse.otp.config.OtpConfigProperties`)**:
   - Annotated with `@ConfigurationProperties(prefix = "gymverse.otp")`.
   - Properties:
     - `int length = 6;`
     - `Duration ttl = Duration.ofMinutes(10);`
     - `Duration resendCooldown = Duration.ofSeconds(60);`
     - `int maxAttempts = 5;`
     - `Duration verifiedTtl = Duration.ofMinutes(15);`
     - Nested prefix keys:
       - `String prefixCode = "gymverse:otp:code:";`
       - `String prefixCooldown = "gymverse:otp:cooldown:";`
       - `String prefixAttempts = "gymverse:otp:attempts:";`
       - `String prefixVerified = "gymverse:otp:verified:";`

2. **Domain Contract (`com.company.GymVerse.otp.service.OtpService`)**:
   ```java
   public interface OtpService {
       void sendOtp(String email, OtpPurpose purpose, String recipientName);
       void resendOtp(String email, OtpPurpose purpose, String recipientName);
       boolean verifyOtp(String email, String otp, OtpPurpose purpose);
       boolean isOtpVerified(String email, OtpPurpose purpose);
       void consumeVerifiedOtp(String email, OtpPurpose purpose);
   }
   ```

3. **Core Implementation (`com.company.GymVerse.otp.serviceImpl.OtpServiceImpl`)**:
   - Injects `RedisTemplate<String, String>`, `KafkaTemplate<String, Object>`, `OtpConfigProperties`.
   - **`sendOtp(email, purpose, recipientName)`**:
     - Check cooldown key `prefixCooldown + purpose + ":" + email`. If exists:
       - Calculate remaining seconds.
       - Throw `OtpRateLimitException("Please wait " + remainingSeconds + " seconds before requesting a new OTP")`.
     - Generate secure 6-digit numeric OTP using `SecureRandom`.
     - Set OTP key `prefixCode + purpose + ":" + email` with value `otp` and duration `ttl` (10m).
     - Set cooldown key `prefixCooldown + purpose + ":" + email` with value `"1"` and duration `resendCooldown` (60s).
     - Reset attempts key `prefixAttempts + purpose + ":" + email` with value `"0"`.
     - Publish `OtpEmailEvent` to Kafka topic (`otp-email-topic`).
   - **`resendOtp(email, purpose, recipientName)`**:
     - Delegates directly to `sendOtp(email, purpose, recipientName)`.
   - **`verifyOtp(email, otp, purpose)`**:
     - Retrieve stored OTP. If null, throw `OtpExpiredException("OTP has expired or was not requested")`.
     - Increment attempts count. If attempts > `maxAttempts`:
       - Evict OTP key from Redis.
       - Throw `OtpInvalidException("Maximum verification attempts exceeded. Please request a new OTP")`.
     - If `!storedOtp.equals(otp.trim())`:
       - Throw `OtpInvalidException("Invalid OTP code. " + (maxAttempts - attempts) + " attempts remaining")`.
     - If matches:
       - Evict OTP key and attempts key.
       - Set verified state key `prefixVerified + purpose + ":" + email` with value `"true"` and duration `verifiedTtl` (15m).
       - Return `true`.
   - **`isOtpVerified(email, purpose)`**:
     - Check if key `prefixVerified + purpose + ":" + email` exists and equals `"true"`.
   - **`consumeVerifiedOtp(email, purpose)`**:
     - Delete key `prefixVerified + purpose + ":" + email`.

---

## 3. Verification & Acceptance Criteria

1. **Unit & Redis Integration Tests (`OtpServiceImplTest.java`)**:
   - Verify OTP is generated and stored in Redis with 10m TTL.
   - Verify cooldown key is created with 60s TTL; attempting another `sendOtp` within 60s throws `OtpRateLimitException`.
   - Verify wrong OTP increments attempt counter and throws `OtpInvalidException`.
   - Verify exceeding 5 failed attempts locks out the OTP and evicts it from Redis.
   - Verify correct OTP creates verified state key with 15m TTL and removes the OTP code.
   - Verify `consumeVerifiedOtp` successfully removes the verified state.
