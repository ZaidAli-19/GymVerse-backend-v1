---
id: "story-3.8"
epic_id: "epic-phase-3-security-auth"
title: "Unified Controller Gateway Integration (/api/auth), Exception Translation & API Payload Contract Documentation"
status: "planning"
type: "gateway-integration"
created_at: "2026-11-17"
dependencies: ["story-3.1", "story-3.2", "story-3.3", "story-3.4", "story-3.5", "story-3.6", "story-3.7"]
files_to_modify:
  - "src/main/java/com/company/GymVerse/exceptions/handler/GlobalExceptionHandler.java"
  - "docs/payloads.md"
files_to_create:
  - "src/main/java/com/company/GymVerse/auth/controller/AuthController.java"
  - "src/main/java/com/company/GymVerse/auth/factory/AuthFactory.java"
  - "src/main/java/com/company/GymVerse/auth/factory/AuthOperation.java"
  - "src/main/java/com/company/GymVerse/auth/request/AuthRequest.java"
  - "src/main/java/com/company/GymVerse/auth/response/AuthResponse.java"
  - "src/main/java/com/company/GymVerse/auth/response/UserSummaryResponse.java"
  - "src/main/java/com/company/GymVerse/auth/mapper/AuthMapper.java"
  - "src/main/java/com/company/GymVerse/auth/mapper/AuthMapperImpl.java"
  - "src/test/java/com/company/GymVerse/auth/AuthUnifiedGatewayIntegrationTest.java"
---

# Story 3.8: Unified Controller Gateway Integration (`/api/auth`), Exception Translation & API Payload Contract Documentation

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Unified Gateway Invariant: Exactly ONE `@PostMapping("/api/auth")` endpoint. No sub-paths or HTTP GET routes.
> 4. Factory Pattern: `AuthFactory` resolves `AuthOperation` beans via `AuthRequestType` enum.
> 5. All 13 mandatory contract fields must be updated in `docs/payloads.md` with copy-pasteable cURL requests.
> 6. Maintain task logging in `antigravity/story-3.8-unified-security-gateway-integration-and-api-docs.md`.

---

## 1. User Story

**As an** API integration developer and platform client,  
**I want** all authentication and master account lifecycle operations routed through a single, standardized gateway endpoint (`POST /api/auth`) with unified exception translation and comprehensive contract documentation,  
**So that** client applications have a predictable, strongly typed contract for all authentication operations.

---

## 2. Technical Context & Requirements

1. **Unified Request Envelope (`com.company.GymVerse.auth.request.AuthRequest`)**:
   - Fields:
     - `@NotNull AuthRequestType requestType` (discriminator enum)
     - `SignupRequest signup` (nested DTO for signup operations)
     - `LoginRequest login` (nested DTO for login & refresh operations)
     - `ForgotPasswordRequest forgotPassword` (nested DTO for password recovery)
   - Follows clean nesting pattern standardized in Story PA-7.

2. **Unified Response Envelope (`com.company.GymVerse.auth.response.AuthResponse`)**:
   - Fields:
     - `String message`
     - `String accessToken`
     - `String refreshToken`
     - `UserSummaryResponse user`
   - Annotated with `@JsonInclude(JsonInclude.Include.NON_NULL)`.

3. **Unified Controller (`com.company.GymVerse.auth.controller.AuthController`)**:
   - Single endpoint:
     ```java
     @RestController
     @RequestMapping("/api/auth")
     @RequiredArgsConstructor
     public class AuthController {
         private final AuthFactory authFactory;

         @PostMapping
         public ResponseEntity<AuthResponse> processAuth(
                 @Valid @RequestBody final AuthRequest request) {
             AuthOperation operation = authFactory.getOperation(request.getRequestType());
             return ResponseEntity.ok(operation.execute(request));
         }
     }
     ```

4. **Factory & Operations (`AuthFactory` & `AuthOperation`)**:
   - `AuthOperation` contract:
     ```java
     public interface AuthOperation {
         AuthRequestType getRequestType();
         AuthResponse execute(AuthRequest request);
     }
     ```
   - `AuthFactory` holds `Map<AuthRequestType, AuthOperation>` populated via constructor injection of `List<AuthOperation>`.

5. **Exception Translation (`GlobalExceptionHandler.java`)**:
   - Add handlers for:
     - `BadCredentialsException`: mapped to HTTP 401 Unauthorized.
     - `InsufficientAuthenticationException`: mapped to HTTP 401 Unauthorized.
     - `OtpRateLimitException`: mapped to HTTP 429 Too Many Requests.
     - `OtpExpiredException`: mapped to HTTP 400 Bad Request.
     - `OtpInvalidException`: mapped to HTTP 400 Bad Request.

6. **API Payloads Documentation (`docs/payloads.md`)**:
   - Document all 10 operations with 13 required fields:
     1. Operation Name
     2. HTTP Method & Path (`POST /api/auth`)
     3. Request Type Discriminator
     4. Purpose / Description
     5. Authorization / Security Requirements (PermitAll or Bearer)
     6. Request Headers
     7. Request Payload Schema (JSON)
     8. Example Request Body
     9. Success Response Schema (HTTP 200)
     10. Example Success Response Body
     11. Error Response Schemas (400, 401, 404, 409, 429)
     12. Validation & Invariant Rules
     13. Copy-Pasteable cURL Command

---

## 3. Verification & Acceptance Criteria

1. **Gateway Integration Test (`AuthUnifiedGatewayIntegrationTest.java`)**:
   - Verify all 10 operations route successfully through `POST /api/auth`.
   - Verify unmapped/null `requestType` returns HTTP 400 with descriptive error.
   - Verify exception handler correctly returns HTTP 401 for bad credentials and HTTP 429 for rate limit violations.
2. **Regression Test Suite**:
   - Execute all test suites across Phase 1, Phase 2, and Phase 3.
   - Ensure 100% test pass rate with zero failures and zero regressions.
