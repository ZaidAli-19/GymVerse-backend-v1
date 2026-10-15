# API Contract & Payloads Reference (`docs/payloads.md`)

> **Master API Registry:** This document specifies the exact request and response schemas, validation rules, HTTP status codes, implementation references, and cURL commands for GymVerse APIs per `PROMPT.md` Section 10.

---

## 1. Platform Administration Module

### 1.1 Create SaaS Subscription Plan

1. **Feature / Module:** Platform Administration (`platformAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/platform-admin`
4. **Purpose:** Single command gateway for platform administration, creating a new tiered SaaS subscription plan in the central catalog when `platformAdminRequestType = CREATE_SAAS_PLAN`.
5. **Authentication & Authorization:** 
   - Role: `ROLE_PLATFORM_ADMIN` / `isPlatformAdmin = true` (central token).
   - *(Note: In Phase 2 Epic PA, live token verification is planned for the subsequent Auth Epic; endpoints are currently open for controller testing).*
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <PLATFORM_ADMIN_JWT_TOKEN>` *(when auth is enabled)*
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "platformAdminRequestType": "CREATE_SAAS_PLAN",
     "name": "Enterprise Gold",
     "description": "Full tier gym plan with unlimited members and custom branding",
     "price": 199.99,
     "billingCycle": "MONTHLY",
     "maxMembers": 1000,
     "maxTrainers": 25,
     "analyticsEnabled": true,
     "multiTrainerEnabled": true,
     "customBrandingEnabled": true
   }
   ```
   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`CREATE_SAAS_PLAN`).
   - `name`: String, required, non-blank, max 100 characters. Unique across plans (case-insensitive).
   - `description`: String, optional.
   - `price`: Decimal/BigDecimal, required, non-negative (`>= 0.00`), max 2 decimal places.
   - `billingCycle`: Enum string, required (`MONTHLY`, `QUARTERLY`, `ANNUAL`).
   - `maxMembers`: Integer, required, `>= 1`.
   - `maxTrainers`: Integer, required, `>= 0`.
   - `analyticsEnabled`: Boolean, optional, defaults to `false`.
   - `multiTrainerEnabled`: Boolean, optional, defaults to `false`.
   - `customBrandingEnabled`: Boolean, optional, defaults to `false`.

10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "id": 1,
        "name": "Enterprise Gold",
        "price": 199.99,
        "active": true,
        "message": "SaaS subscription plan created successfully",
        "saasPlan": {
          "id": 1,
          "name": "Enterprise Gold",
          "description": "Full tier gym plan with unlimited members and custom branding",
          "price": 199.99,
          "billingCycle": "MONTHLY",
          "maxMembers": 1000,
          "maxTrainers": 25,
          "analyticsEnabled": true,
          "multiTrainerEnabled": true,
          "customBrandingEnabled": true,
          "active": true
        }
      }
      ```

11. **Error Responses:**
    - **HTTP 400 Bad Request (Validation, Missing RequestType, or Invariant Failure):**
      ```json
      {
        "timestamp": "2026-10-15T16:30:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Price must be a non-negative decimal value",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 409 Conflict (Duplicate Plan Name):**
      ```json
      {
        "timestamp": "2026-10-15T16:30:00Z",
        "status": 409,
        "error": "Conflict",
        "message": "A plan with name 'Enterprise Gold' already exists",
        "path": "/api/platform-admin"
      }
      ```

12. **Implementation References:**
    - **Controller:** `com.company.GymVerse.platformAdmin.controller.PlatformAdminController`
    - **Factory:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory`
    - **Factory Operation:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation`
    - **Operation Service:** `com.company.GymVerse.platformAdmin.service.CreateSaasPlanService`
    - **Validation:** `com.company.GymVerse.platformAdmin.validations.PlatformAdminValidationsImpl`
    - **Mapper:** `com.company.GymVerse.platformAdmin.mapper.SaasPlanMapperImpl`
    - **Request DTO:** `com.company.GymVerse.platformAdmin.request.PlatformAdminRequest`
    - **Response DTO:** `com.company.GymVerse.platformAdmin.response.PlatformAdminResponse` (with nested `SaasPlanResponse`)
    - **Request Type Enum:** `com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType`
    - **Entity:** `com.company.GymVerse.models.central.SaasSubscriptionPlan`
    - **Repository:** `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`
    - **Exception Handler:** `com.company.GymVerse.exceptions.GlobalExceptionHandler`

13. **Business Rules:**
    - Single entry point per module delegates all mutations via `PlatformAdminFactory`.
    - New plans are automatically initialized with `active = true`.
    - Plan names are unique in a case-insensitive manner (`Enterprise Gold` conflicts with `enterprise gold`).
    - Transaction executes under `centralTransactionManager` targeting the central database `GymVerse`.
    - Tenant databases and `TenantDataSourceCache` are never accessed.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/platform-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "platformAdminRequestType": "CREATE_SAAS_PLAN",
       "name": "Enterprise Gold",
       "description": "Full tier gym plan with unlimited members and custom branding",
       "price": 199.99,
       "billingCycle": "MONTHLY",
       "maxMembers": 1000,
       "maxTrainers": 25,
       "analyticsEnabled": true,
       "multiTrainerEnabled": true,
       "customBrandingEnabled": true
     }'
```
