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

---

### 1.2 Fetch SaaS Subscription Plans Catalog (Dynamic Query & Pagination)

1. **Feature / Module:** Platform Administration (`platformAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/platform-admin`
4. **Purpose:** Single command gateway query dispatching `FETCH_ALL_SAAS_PLANS`, dynamically filtering, sorting, and paginating SaaS subscription plans via Spring Data JPA Specifications.
5. **Authentication & Authorization:** 
   - Role: `ROLE_PLATFORM_ADMIN` / `isPlatformAdmin = true` (central token).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <PLATFORM_ADMIN_JWT_TOKEN>` *(when auth is enabled)*
7. **Path Parameters:** None
8. **Query Parameters:** None (All query criteria, pagination, and sorting parameters are supplied in the request body).
9. **Request Payload (`application/json`):**
   ```json
   {
     "platformAdminRequestType": "FETCH_ALL_SAAS_PLANS",
     "active": true,
     "billingCycle": "MONTHLY",
     "name": "Pro",
     "minPrice": 20.00,
     "maxPrice": 100.00,
     "page": 0,
     "size": 10,
     "sortBy": "price",
     "sortDirection": "asc"
   }
   ```
   *Note: Can also be passed as a nested object using `"planFilter": { ... }`.*

   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`FETCH_ALL_SAAS_PLANS`).
   - `active`: Boolean, optional (filters active/inactive status).
   - `billingCycle`: Enum string, optional (`MONTHLY`, `QUARTERLY`, `ANNUAL`).
   - `name`: String, optional (case-insensitive partial substring match `%name%`).
   - `minPrice`: Decimal/BigDecimal, optional, non-negative (`>= 0.00`).
   - `maxPrice`: Decimal/BigDecimal, optional, non-negative (`>= 0.00`). `minPrice` must be `<= maxPrice`.
   - `page`: Integer, optional, default `0`. Must not be negative (`>= 0`).
   - `size`: Integer, optional, default `10`. Must be between `1` and `100`.
   - `sortBy`: String, optional, default `"id"`. Must match sort whitelist: `[id, name, price, billingCycle, maxMembers, maxTrainers]`.
   - `sortDirection`: String, optional, default `"asc"`. Must be `"asc"` or `"desc"` (case-insensitive).

10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "1 SaaS plan(s) retrieved successfully",
        "currentPage": 0,
        "pageSize": 10,
        "totalRecords": 1,
        "totalPages": 1,
        "last": true,
        "plans": [
          {
            "id": 1,
            "name": "Monthly Pro",
            "description": "Professional monthly tier",
            "price": 49.99,
            "billingCycle": "MONTHLY",
            "maxMembers": 100,
            "maxTrainers": 5,
            "analyticsEnabled": true,
            "multiTrainerEnabled": true,
            "customBrandingEnabled": false,
            "active": true
          }
        ],
        "pagedPlans": {
          "content": [
            {
              "id": 1,
              "name": "Monthly Pro",
              "description": "Professional monthly tier",
              "price": 49.99,
              "billingCycle": "MONTHLY",
              "maxMembers": 100,
              "maxTrainers": 5,
              "analyticsEnabled": true,
              "multiTrainerEnabled": true,
              "customBrandingEnabled": false,
              "active": true
            }
          ],
          "page": 0,
          "size": 10,
          "totalElements": 1,
          "totalPages": 1,
          "last": true
        }
      }
      ```

11. **Error Responses:**
    - **HTTP 400 Bad Request — Invalid Sort Field:**
      ```json
      {
        "timestamp": "2026-10-15T12:00:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Invalid sort field: 'secretField'. Allowed fields: [id, name, price, billingCycle, maxMembers, maxTrainers]",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Negative Page Index:**
      ```json
      {
        "timestamp": "2026-10-15T12:00:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Page index must not be negative",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Page Size Out of Bounds:**
      ```json
      {
        "timestamp": "2026-10-15T12:00:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Page size must be between 1 and 100",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Invalid Price Range:**
      ```json
      {
        "timestamp": "2026-10-15T12:00:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "minPrice cannot be greater than maxPrice",
        "path": "/api/platform-admin"
      }
      ```

12. **Implementation References:**
    - **Controller:** `com.company.GymVerse.platformAdmin.controller.PlatformAdminController`
    - **Factory:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory`
    - **Factory Operation:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation`
    - **Operation Service:** `com.company.GymVerse.platformAdmin.service.GetAllSaasPlansService`
    - **JPA Specification:** `com.company.GymVerse.platformAdmin.specifications.SaasPlanSpecification`
    - **Validation:** `com.company.GymVerse.platformAdmin.validations.PlatformAdminValidationsImpl`
    - **Mapper:** `com.company.GymVerse.platformAdmin.mapper.SaasPlanMapperImpl`
    - **Request DTO:** `com.company.GymVerse.platformAdmin.request.PlatformAdminRequest` / `SaasPlanFilterRequest`
    - **Response DTO:** `com.company.GymVerse.platformAdmin.response.PlatformAdminResponse` (with nested `PagedResponse<SaasPlanResponse>`)
    - **Request Type Enum:** `com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS`
    - **Entity:** `com.company.GymVerse.models.central.SaasSubscriptionPlan`
    - **Repository:** `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`
    - **Exception Handler:** `com.company.GymVerse.exceptions.GlobalExceptionHandler`

13. **Business Rules:**
    - Dynamic query execution happens entirely in MySQL via JPA `Specification` and `CriteriaBuilder`.
    - In-memory post-filtering is strictly forbidden.
    - Sort parameters are validated against an explicit whitelist to prevent property traversal attacks.
    - Read transactions execute under `centralTransactionManager` with `readOnly = true`.
    - When no records match, an empty list with `totalRecords = 0`, `totalPages = 0`, `last = true` is returned with HTTP 200 OK.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/platform-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "platformAdminRequestType": "FETCH_ALL_SAAS_PLANS",
       "active": true,
       "billingCycle": "MONTHLY",
       "minPrice": 10.00,
       "maxPrice": 100.00,
       "page": 0,
       "size": 10,
       "sortBy": "price",
       "sortDirection": "asc"
     }'
```

---

### 1.3 Fetch Single SaaS Subscription Plan by ID

1. **Feature / Module:** Platform Administration (`platformAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/platform-admin`
4. **Purpose:** Single command gateway operation retrieving the complete domain details of an individual SaaS subscription plan by its identifier (`FETCH_SAAS_PLAN_BY_ID`).
5. **Authentication & Authorization:** 
   - Role: `ROLE_PLATFORM_ADMIN` / `isPlatformAdmin = true` (central token).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <PLATFORM_ADMIN_JWT_TOKEN>` *(when auth is enabled)*
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "platformAdminRequestType": "FETCH_SAAS_PLAN_BY_ID",
     "planId": 5
   }
   ```
   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`FETCH_SAAS_PLAN_BY_ID`).
   - `planId`: Long, required. Must be a positive integer (`> 0`).

10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "SaaS subscription plan retrieved successfully",
        "planId": 5,
        "id": 5,
        "name": "Growth Tier",
        "price": 79.50,
        "active": true,
        "saasPlan": {
          "id": 5,
          "name": "Growth Tier",
          "description": "Mid-market gym growth tier with analytics",
          "price": 79.50,
          "billingCycle": "MONTHLY",
          "maxMembers": 300,
          "maxTrainers": 8,
          "analyticsEnabled": true,
          "multiTrainerEnabled": true,
          "customBrandingEnabled": false,
          "active": true
        }
      }
      ```

11. **Error Responses:**
    - **HTTP 400 Bad Request — Missing Plan ID:**
      ```json
      {
        "timestamp": "2026-10-15T12:30:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Plan ID is required",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Non-Positive Plan ID:**
      ```json
      {
        "timestamp": "2026-10-15T12:30:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Plan ID must be a positive number",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 404 Not Found — Resource Does Not Exist:**
      ```json
      {
        "timestamp": "2026-10-15T12:30:00Z",
        "status": 404,
        "error": "Not Found",
        "message": "SaasSubscriptionPlan not found with id: 999999",
        "path": "/api/platform-admin"
      }
      ```

12. **Implementation References:**
    - **Controller:** `com.company.GymVerse.platformAdmin.controller.PlatformAdminController`
    - **Factory:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory`
    - **Factory Operation:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation`
    - **Operation Service:** `com.company.GymVerse.platformAdmin.service.GetSaasPlanByIdService`
    - **Validation:** `com.company.GymVerse.platformAdmin.validations.PlatformAdminValidationsImpl`
    - **Mapper:** `com.company.GymVerse.platformAdmin.mapper.SaasPlanMapperImpl`
    - **Request DTO:** `com.company.GymVerse.platformAdmin.request.PlatformAdminRequest`
    - **Response DTO:** `com.company.GymVerse.platformAdmin.response.PlatformAdminResponse` (with nested `SaasPlanResponse`)
    - **Request Type Enum:** `com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType.FETCH_SAAS_PLAN_BY_ID`
    - **Entity:** `com.company.GymVerse.models.central.SaasSubscriptionPlan`
    - **Repository:** `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`
    - **Exception Handler:** `com.company.GymVerse.exceptions.GlobalExceptionHandler`

13. **Business Rules:**
    - Single entry point per module delegates lookup via `PlatformAdminFactory`.
    - If the plan is present, returns full configuration in both scalar fields and nested `saasPlan`.
    - If the plan does not exist, throws domain `ResourceNotFoundException`, mapped to HTTP 404.
    - Transaction executes under `centralTransactionManager` with `readOnly = true`.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/platform-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "platformAdminRequestType": "FETCH_SAAS_PLAN_BY_ID",
       "planId": 5
     }'
```

---

### 1.4 Update SaaS Subscription Plan Availability (Activation / Deactivation)

1. **Feature / Module:** Platform Administration (`platformAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/platform-admin`
4. **Purpose:** Single command gateway operation to surgically activate or deactivate a SaaS subscription plan's availability (`UPDATE_PLAN_AVAILABILITY`) without altering prices, capacity limits, or entitlements.
5. **Authentication & Authorization:** 
   - Role: `ROLE_PLATFORM_ADMIN` / `isPlatformAdmin = true` (central token).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <PLATFORM_ADMIN_JWT_TOKEN>` *(when auth is enabled)*
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "platformAdminRequestType": "UPDATE_PLAN_AVAILABILITY",
     "planId": 3,
     "active": false
   }
   ```
   *Note: Can also be passed as a nested object using `"planAvailability": { "planId": 3, "active": false }`.*

   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`UPDATE_PLAN_AVAILABILITY`).
   - `planId`: Long, required. Must be a positive integer (`> 0`).
   - `active`: Boolean, required (`true` or `false`). Cannot be null.

10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "SaaS subscription plan availability updated to: INACTIVE",
        "planId": 3,
        "id": 3,
        "name": "Summer Special",
        "price": 49.99,
        "active": false,
        "saasPlan": {
          "id": 3,
          "name": "Summer Special",
          "description": "Seasonal promotional tier",
          "price": 49.99,
          "billingCycle": "MONTHLY",
          "maxMembers": 50,
          "maxTrainers": 2,
          "analyticsEnabled": false,
          "multiTrainerEnabled": false,
          "customBrandingEnabled": false,
          "active": false
        }
      }
      ```

11. **Error Responses:**
    - **HTTP 400 Bad Request — Missing Active Status:**
      ```json
      {
        "timestamp": "2026-10-15T12:40:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Plan availability status must be specified as true or false",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Missing Plan ID:**
      ```json
      {
        "timestamp": "2026-10-15T12:40:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Plan ID is required",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Non-Positive Plan ID:**
      ```json
      {
        "timestamp": "2026-10-15T12:40:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Plan ID must be a positive number",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 404 Not Found — Plan Does Not Exist:**
      ```json
      {
        "timestamp": "2026-10-15T12:40:00Z",
        "status": 404,
        "error": "Not Found",
        "message": "SaasSubscriptionPlan not found with id: 8888",
        "path": "/api/platform-admin"
      }
      ```

12. **Implementation References:**
    - **Controller:** `com.company.GymVerse.platformAdmin.controller.PlatformAdminController`
    - **Factory:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory`
    - **Factory Operation:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation`
    - **Operation Service:** `com.company.GymVerse.platformAdmin.service.UpdatePlanAvailabilityService`
    - **Validation:** `com.company.GymVerse.platformAdmin.validations.PlatformAdminValidationsImpl`
    - **Mapper:** `com.company.GymVerse.platformAdmin.mapper.SaasPlanMapperImpl`
    - **Request DTO:** `com.company.GymVerse.platformAdmin.request.PlatformAdminRequest` / `UpdatePlanAvailabilityRequest`
    - **Response DTO:** `com.company.GymVerse.platformAdmin.response.PlatformAdminResponse` (with nested `SaasPlanResponse`)
    - **Request Type Enum:** `com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType.UPDATE_PLAN_AVAILABILITY`
    - **Entity:** `com.company.GymVerse.models.central.SaasSubscriptionPlan`
    - **Repository:** `com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository`
    - **Exception Handler:** `com.company.GymVerse.exceptions.GlobalExceptionHandler`

13. **Business Rules:**
    - Critical Immutability Invariant: Mutates *only* the `active` column. Prices, billing cycles, capacity limits, and entitlements are strictly untouched to prevent corruption of existing gym tenant subscriptions.
    - Soft-Retirement: Obsolete plans are deactivated (`active = false`) rather than hard-deleted from the database, preserving referential integrity with `gym_subscriptions`.
    - Inactive plans are automatically excluded from tenant-facing registration catalogs (`findByActiveTrue()`).
    - Transaction executes under `centralTransactionManager` targeting the central database `GymVerse`.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/platform-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "platformAdminRequestType": "UPDATE_PLAN_AVAILABILITY",
       "planId": 3,
       "active": false
     }'
```
