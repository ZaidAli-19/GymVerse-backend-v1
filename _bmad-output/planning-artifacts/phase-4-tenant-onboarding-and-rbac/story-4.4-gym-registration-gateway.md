---
id: "story-4.4"
epic_id: "epic-phase-4-tenant-onboarding-and-rbac"
title: "Gym Registration Gateway (REGISTER_GYM)"
status: "completed"
type: "gateway-operation"
created_at: "2026-11-18"
dependencies:
  - "story-4.1"
  - "story-4.2"
  - "story-4.3"
files_to_modify:
  - "src/main/java/com/company/GymVerse/config/SecurityConfig.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/controller/TenantAdminController.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminFactory.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminOperation.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/RegisterGymRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/RegisterGymResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/RegisterGymService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/RegisterGymIntegrationTest.java"
---

# Story 4.4: Gym Registration Gateway (REGISTER_GYM)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Unified single-endpoint gateway: `@PostMapping("/api/tenant-admin")`. No ad-hoc endpoints.
> 4. Factory pattern dispatching to `TenantAdminOperation` beans.
> 5. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 6. Maintain task logging in `antigravity/story-4.4-gym-registration-gateway.md`.

---

## 1. User Story

**As a** prospective gym owner authenticated via Master Auth on a Web or Electron desktop application,  
**I want** to register my gym via a secure single-endpoint gateway `@PostMapping("/api/tenant-admin")` with request type `REGISTER_GYM`,  
**So that** my gym database is automatically provisioned, my free trial is activated, and I immediately receive tenant-scoped access and refresh tokens for entering my gym dashboard, while mobile registration requests are barred.

---

## 2. Technical Context & Requirements

1. **Security & Platform Guard**:
   - Gateway `@PostMapping("/api/tenant-admin")` is secured by Spring Security, requiring authenticated caller with `MASTER_ACCOUNT` token.
   - Caller account ID is retrieved from `SecurityContextHolder.getContext().getAuthentication().getName()`.
   - `TenantAdminValidations` inspects the HTTP header `X-Client-Platform`:
     - If `MOBILE`: throws `ForbiddenException("Gym registration is restricted to Web and Desktop platforms")` (HTTP 403).
     - Permitted values: `WEB`, `DESKTOP_ELECTRON`.

2. **Unified Gateway Architecture**:
   - `TenantAdminController`: Exposes single `@PostMapping` accepting `TenantAdminRequest`.
   - `TenantAdminFactory`: Resolves beans implementing `TenantAdminOperation`.
   - `TenantAdminRequestType`: includes enum `REGISTER_GYM`.

3. **Register Gym Operation (`RegisterGymService.java`)**:
   - Validates input: gym name, contact email, phone number, address, city, operating hours, coordinates.
   - Invokes `TenantProvisioningService` to create DB, run migrations, seed tenant user, and activate trial.
   - Invokes `JwtUtil` to generate tenant-scoped access and refresh tokens.
   - Returns `RegisterGymResponse` containing:
     - `tenantId`
     - `gymCode`
     - `gymName`
     - `databaseName`
     - `tenantAccessToken`
     - `tenantRefreshToken`
     - `trialEndDate`
     - `trialDaysRemaining`

---

## 3. Verification & Acceptance Criteria

1. **Integration Test (`RegisterGymIntegrationTest.java`)**:
   - **Successful Registration (Web/Desktop):** Valid Master Bearer token + `X-Client-Platform: WEB` returns HTTP 200 with tenant tokens and gym metadata.
   - **Mobile Denial Test:** `X-Client-Platform: MOBILE` returns HTTP 403 Forbidden with proper error message.
   - **Unauthenticated Test:** Missing Bearer token returns HTTP 401 Unauthorized.
   - **Validation Error Test:** Missing required gym fields returns HTTP 400 Bad Request.
2. **Context Compilation Check**:
   - Clean build via `./mvnw.cmd test-compile`.
