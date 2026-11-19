# Task Log: Story 4.4 - Gym Registration Gateway (REGISTER_GYM)

## Status
- **Status:** COMPLETED
- **Branch:** `GymVerse/dev`
- **Execution Date:** 2026-11-18
- **Epic:** Phase 4 - Tenant Onboarding, Dynamic Database Provisioning, Scoped RBAC, & Trial Engine

---

## 1. Objectives & Scope
Implement the unified Gym Registration Gateway operation (`REGISTER_GYM`) exposed through single endpoint `@PostMapping("/api/tenant-admin")`:
1. Expose unified command gateway `@PostMapping("/api/tenant-admin")` accepting `TenantAdminRequest`.
2. Implement `TenantAdminFactory` and `TenantAdminOperation` pattern.
3. Secure the gateway via Spring Security, requiring `MASTER_ACCOUNT` authentication.
4. Enforce platform restriction: inspect `X-Client-Platform` header via `TenantAdminValidations`; reject `MOBILE` requests with HTTP 403 Forbidden.
5. Implement `RegisterGymService` executing validations, resolving authenticated user, invoking `TenantProvisioningService` for dynamic provisioning and trial attachment, generating tenant-scoped JWT tokens (`TENANT_ADMIN` role, `PRIMARY` authority), and returning `RegisterGymResponse`.
6. Maintain validation layer separation (`TenantAdminValidations` + `TenantAdminValidationsImpl`) and handwritten mapper (`TenantAdminMapper` + `TenantAdminMapperImpl`).
7. Update API documentation in `docs/payloads.md` with complete 13-field specifications and cURL examples.

---

## 2. Changes Made

### Files Created:
1. [`ForbiddenException.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/common/ForbiddenException.java):
   - Domain exception mapped to HTTP 403 Forbidden.
2. [`TenantAdminRequestType.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java):
   - Discriminator enum (`REGISTER_GYM`, `FETCH_MY_TENANTS`, `FETCH_TENANT_SUBSCRIPTION`).
3. [`RegisterGymRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/request/RegisterGymRequest.java):
   - Request DTO for facility registration payload.
4. [`TenantAdminRequest.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java):
   - Unified request envelope with Jackson aliases and platform parameter.
5. [`RegisterGymResponse.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/response/RegisterGymResponse.java):
   - Registration outcome payload with tenant tokens and gym metadata.
6. [`TenantAdminResponse.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java):
   - Unified response envelope for all TenantAdmin operations.
7. [`TenantAdminOperation.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminOperation.java):
   - Non-generic operation contract for module factory.
8. [`TenantAdminFactory.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/factory/TenantAdminFactory.java):
   - Factory dispatching requests to registered operations.
9. [`TenantAdminMapper.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java) & [`TenantAdminMapperImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java):
   - Handwritten mapper translating requests, commands, and responses without reflection.
10. [`RegisterGymService.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/service/RegisterGymService.java):
    - Core operation handler coordinating platform checks, domain validations, provisioning, and token generation.
11. [`TenantAdminController.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/controller/TenantAdminController.java):
    - Single REST endpoint `@PostMapping("/api/tenant-admin")`.
12. [`RegisterGymIntegrationTest.java`](file:///D:/projects/GymVerse-Backend-v2/src/test/java/com/company/GymVerse/tenantAdmin/RegisterGymIntegrationTest.java):
    - End-to-end integration tests verifying Web registration, Desktop registration, Mobile platform rejection (403), unauthenticated access (401), and validation failures (400).

### Files Modified:
1. [`SecurityConfig.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/config/SecurityConfig.java):
   - Explicitly configured `.requestMatchers("/api/tenant-admin/**").authenticated()`.
2. [`GlobalExceptionHandler.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/exceptions/handler/GlobalExceptionHandler.java):
   - Added handler for `ForbiddenException` returning HTTP 403 `ErrorResponse`.
3. [`TenantAdminValidations.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java) & [`TenantAdminValidationsImpl.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java):
   - Added `validateClientPlatform` and `validateRegisterGym`.
4. [`TenantAdminValidationsTest.java`](file:///D:/projects/GymVerse-Backend-v2/src/test/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsTest.java):
   - Expanded tests to 16 test cases covering platform checks, payload null guards, blank fields, and custom code collisions.
5. [`docs/payloads.md`](file:///D:/projects/GymVerse-Backend-v2/docs/payloads.md):
   - Appended Section 3.1 with 13 mandatory contract fields and cURL snippet.

---

## 3. Verification & Test Execution
- Run command: `.\mvnw.cmd test "-Dtest=TenantAdminValidationsTest,TenantProvisioningServiceTest,TenantConfigPropertiesTest,SubscriptionDateUtilTest,TenantJwtTokenTest,JwtUtilTest,RegisterGymIntegrationTest"`
- Results: 55 tests run, 0 failures, 0 errors.
- Architectural invariant check: All 8 multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
