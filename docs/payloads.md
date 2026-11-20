# API Contract & Payloads Reference (`docs/payloads.md`)

> **Master API Registry:** This document specifies the exact request and response schemas, validation rules, HTTP status codes, implementation references, and cURL commands for GymVerse APIs per `PROMPT.md` Section 10.

---

## 📑 Quick Navigation Index

Click on any module or operation to jump directly to its API specification and cURL example:

| # | Module Gateway | Endpoint URL | Auth Scope | Available Operations |
|---|---|---|---|---|
| **1** | [**Platform Administration**](#section-1) | `POST /api/platform-admin` | Platform Admin (`ROLE_PLATFORM_ADMIN`) | • [1.1 Create SaaS Plan](#op-1-1)<br>• [1.2 Catalog Query & Pagination](#op-1-2)<br>• [1.3 Fetch Plan by ID](#op-1-3)<br>• [1.4 Update Plan Availability](#op-1-4)<br>• [1.5 Gym Tenants Directory](#op-1-5) |
| **2** | [**Authentication & Security**](#section-2) | `POST /api/auth` | Public & Master Account | • [2.1 Signup Initiate](#op-2-1)<br>• [2.2 Signup Verify OTP](#op-2-2)<br>• [2.3 Signup Complete](#op-2-3)<br>• [2.4 Password Login](#op-2-4)<br>• [2.5 Login OTP Initiate](#op-2-5)<br>• [2.6 Login OTP Verify](#op-2-6)<br>• [2.7 Token Refresh](#op-2-7)<br>• [2.8 Forgot Password Initiate](#op-2-8)<br>• [2.9 Forgot Password Verify OTP](#op-2-9)<br>• [2.10 Forgot Password Reset](#op-2-10)<br>• [2.11 Switch Tenant & Token Exchange](#op-2-11) |
| **3** | [**Central User Gateway**](#section-3) | `POST /api/user` | Master User (`MASTER_ACCOUNT`) | • [3.1 Register Gym & Dynamic Provisioning](#op-3-1)<br>• [3.2 Fetch My Tenants Directory](#op-3-2)<br>• [3.3 Fetch Gym by Code (Discovery)](#op-3-3) |
| **4** | [**Tenant Administration**](#section-4) | `POST /api/tenant-admin` | Tenant Admin (`TENANT_ACCOUNT` + `ROLE_TENANT_ADMIN`) | • [4.1 Fetch SaaS Subscription & Entitlements](#op-4-1) |

---

<a id="section-1"></a>
<a id="platform-admin-module"></a>
## 1. Platform Administration Module (`POST /api/platform-admin`)

<a id="op-1-1"></a>
<a id="create-saas-plan"></a>
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

<a id="op-1-2"></a>
<a id="fetch-all-saas-plans"></a>
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
     "page": 0,
     "size": 10,
     "sortBy": "price",
     "sortDirection": "asc",
     "planFilter": {
       "active": true,
       "billingCycle": "MONTHLY",
       "name": "Pro",
       "minPrice": 20.00,
       "maxPrice": 100.00
     }
   }
   ```
   *Note: Pagination (`page`, `size`) and sorting (`sortBy`, `sortDirection`) are consolidated strictly at the root level. Domain filter criteria are encapsulated inside `planFilter` (optional).*

   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`FETCH_ALL_SAAS_PLANS`).
   - `page`: Integer, optional, default `0`. Must not be negative (`>= 0`).
   - `size`: Integer, optional, default `10`. Must be between `1` and `100`.
   - `sortBy`: String, optional, default `"id"`. Must match sort whitelist: `[id, name, price, billingCycle, maxMembers, maxTrainers]`.
   - `sortDirection`: String, optional, default `"asc"`. Must be `"asc"` or `"desc"` (case-insensitive).
   - `planFilter`: Object, optional containing:
     - `active`: Boolean, optional (filters active/inactive status).
     - `billingCycle`: Enum string, optional (`MONTHLY`, `QUARTERLY`, `ANNUAL`).
     - `name`: String, optional (case-insensitive partial substring match `%name%`).
     - `minPrice`: Decimal/BigDecimal, optional, non-negative (`>= 0.00`).
     - `maxPrice`: Decimal/BigDecimal, optional, non-negative (`>= 0.00`). `minPrice` must be `<= maxPrice`.

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
       "page": 0,
       "size": 10,
       "sortBy": "price",
       "sortDirection": "asc",
       "planFilter": {
         "active": true,
         "billingCycle": "MONTHLY",
         "minPrice": 10.00,
         "maxPrice": 100.00
       }
     }'
```

---

<a id="op-1-3"></a>
<a id="fetch-saas-plan-by-id"></a>
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

<a id="op-1-4"></a>
<a id="update-plan-availability"></a>
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

---

<a id="op-1-5"></a>
<a id="fetch-all-gym-tenants"></a>
### 1.5 Registered Gym Tenants Directory Retrieval

1. **Feature / Module:** Platform Administration (`platformAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/platform-admin`
4. **Purpose:** Single command gateway query retrieving a filtered, sorted, and paginated directory of all registered gym tenants from the central database (`gym_tenants`) when `platformAdminRequestType = FETCH_ALL_GYM_TENANTS`.
5. **Authentication & Authorization:**
   - Role: `ROLE_PLATFORM_ADMIN` / `isPlatformAdmin = true` (central token).
   - *(Note: In Phase 2 Epic PA, live token verification is planned for the subsequent Auth Epic; endpoints are currently open for controller testing).*
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <PLATFORM_ADMIN_JWT_TOKEN>` *(when auth is enabled)*
7. **Path Parameters:** None
8. **Query Parameters:** None (All filters supplied in JSON body per single gateway standard)
9. **Request Payload (`application/json`):**
   ```json
   {
     "platformAdminRequestType": "FETCH_ALL_GYM_TENANTS",
     "page": 0,
     "size": 10,
     "sortBy": "registeredAt",
     "sortDirection": "desc",
     "tenantFilter": {
       "status": "ACTIVE",
       "city": "Austin",
       "gymName": "Iron",
       "gymCode": "IR"
     }
   }
   ```
   *Note: Pagination (`page`, `size`) and sorting (`sortBy`, `sortDirection`) are consolidated strictly at the root level. Domain filter criteria are encapsulated inside `tenantFilter` (optional).*

   **Validation Rules:**
   - `platformAdminRequestType`: Enum string, required (`FETCH_ALL_GYM_TENANTS`).
   - `page`: Integer, optional. Defaults to `0`. Must be `>= 0`.
   - `size`: Integer, optional. Defaults to `10`. Must be between `1` and `100`.
   - `sortBy`: String, optional. Defaults to `"registeredAt"`. Whitelist-guarded to allowed entity properties: `["id", "gymName", "gymCode", "city", "status", "registeredAt"]`.
   - `sortDirection`: String, optional. Defaults to `"desc"`. Allowed values: `["asc", "desc"]` (case-insensitive).
   - `tenantFilter`: Object, optional containing:
     - `status`: Enum string, optional (`ACTIVE`, `SUSPENDED`, `INACTIVE`).
     - `city`: String, optional. Evaluated as case-insensitive partial match (`%city%`).
     - `gymName`: String, optional. Evaluated as case-insensitive partial match (`%gymName%`).
     - `gymCode`: String, optional. Evaluated as case-insensitive prefix match (`gymCode%`).

10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "1 gym tenant(s) retrieved successfully",
        "tenants": [
          {
            "id": 1,
            "gymName": "Iron Fitness",
            "contactEmail": "admin@ironfitness.com",
            "phoneNumber": "+1-512-555-0199",
            "address": "100 Muscle Way",
            "city": "Austin",
            "logoUrl": "https://assets.ironfitness.com/logo.png",
            "gymCode": "IRONFIT",
            "dbName": "gym_db_1",
            "status": "ACTIVE",
            "registeredAt": "2026-10-15T10:00:00Z",
            "suspendedAt": null,
            "suspensionReason": null
          }
        ],
        "pagedTenants": {
          "content": [
            {
              "id": 1,
              "gymName": "Iron Fitness",
              "contactEmail": "admin@ironfitness.com",
              "phoneNumber": "+1-512-555-0199",
              "address": "100 Muscle Way",
              "city": "Austin",
              "logoUrl": "https://assets.ironfitness.com/logo.png",
              "gymCode": "IRONFIT",
              "dbName": "gym_db_1",
              "status": "ACTIVE",
              "registeredAt": "2026-10-15T10:00:00Z"
            }
          ],
          "page": 0,
          "size": 10,
          "totalElements": 1,
          "totalPages": 1,
          "last": true
        },
        "currentPage": 0,
        "pageSize": 10,
        "totalRecords": 1,
        "totalPages": 1,
        "last": true
      }
      ```

11. **Error Responses:**
    - **HTTP 400 Bad Request — Invalid Sort Property:**
      ```json
      {
        "timestamp": "2026-10-15T18:20:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Invalid sort field: 'contactEmail'. Allowed fields: [id, gymName, gymCode, city, status, registeredAt]",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Invalid Sort Direction:**
      ```json
      {
        "timestamp": "2026-10-15T18:20:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Invalid sort direction: 'down'. Allowed values: [asc, desc]",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Negative Page Index:**
      ```json
      {
        "timestamp": "2026-10-15T18:20:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Page index must not be negative",
        "path": "/api/platform-admin"
      }
      ```
    - **HTTP 400 Bad Request — Out of Bounds Page Size:**
      ```json
      {
        "timestamp": "2026-10-15T18:20:00Z",
        "status": 400,
        "error": "Bad Request",
        "message": "Page size must be between 1 and 100",
        "path": "/api/platform-admin"
      }
      ```

12. **Implementation References:**
    - **Controller:** `com.company.GymVerse.platformAdmin.controller.PlatformAdminController`
    - **Factory:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory`
    - **Factory Operation:** `com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation`
    - **Operation Service:** `com.company.GymVerse.platformAdmin.service.GetGymTenantsService`
    - **Specification:** `com.company.GymVerse.platformAdmin.specifications.GymTenantSpecification`
    - **Validation:** `com.company.GymVerse.platformAdmin.validations.PlatformAdminValidationsImpl`
    - **Mapper:** `com.company.GymVerse.platformAdmin.mapper.GymTenantSummaryMapperImpl`
    - **Request DTO:** `com.company.GymVerse.platformAdmin.request.PlatformAdminRequest` / `GymTenantFilterRequest`
    - **Response DTO:** `com.company.GymVerse.platformAdmin.response.PlatformAdminResponse` / `GymTenantSummaryResponse` / `PagedResponse`
    - **Request Type Enum:** `com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS`
    - **Entity:** `com.company.GymVerse.models.central.GymTenant`
    - **Repository:** `com.company.GymVerse.repositories.central.GymTenantRepository`
    - **Exception Handler:** `com.company.GymVerse.exceptions.GlobalExceptionHandler`

13. **Business Rules:**
    - Zero Tenant Routing Invariant: Executes exclusively under `centralTransactionManager` (`readOnly = true`) against the central database (`gym_tenants`). Never accesses `TenantContext` or tenant databases.
    - SQL Pushdown: All filtering, sorting, and pagination execute strictly inside MySQL via Spring Data JPA Specifications (`JpaSpecificationExecutor<GymTenant>`). No in-memory filtering.
    - Security Property Whitelist: Client sort parameter is verified against `[id, gymName, gymCode, city, status, registeredAt]` to eliminate property path traversal and SQL injection vulnerabilities.
    - Default Sorting: Registered gym tenants are ordered by `registeredAt` descending by default to surface newly onboarded gyms first.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/platform-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "platformAdminRequestType": "FETCH_ALL_GYM_TENANTS",
       "page": 0,
       "size": 10,
       "sortBy": "registeredAt",
       "sortDirection": "desc",
       "tenantFilter": {
         "status": "ACTIVE",
         "city": "Austin"
       }
     }'
```


---

<a id="section-2"></a>
<a id="auth-module"></a>
## 2. Authentication & Security Module (`POST /api/auth`)

<a id="op-2-1"></a>
<a id="signup-initiate"></a>
### 2.1 Signup Initiate (`SIGNUP_INITIATE`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 1 of Master user registration. Validates email and name constraints, verifies that the email is not already registered, caches pending profile, and asynchronously dispatches a 6-digit OTP code to the user's email address via Kafka.
5. **Authentication & Authorization:** Public access (`permitAll()`). No token required.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "SIGNUP_INITIATE",
     "signupRequest": {
       "email": "alex.walker@example.com",
       "firstName": "Alex",
       "lastName": "Walker",
       "phoneNumber": "+15551234567"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`SIGNUP_INITIATE`).
   - `email`: Required, valid RFC 5322 format. Must not exist in central `user_accounts`.
   - `firstName`: Required, 2–50 characters.
   - `lastName`: Required, 2–50 characters.
   - `phoneNumber`: Optional, valid E.164 phone format (7–20 digits).
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Verification OTP has been sent to your email"
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Validation failure on name, email format, or missing request type.
    - `409 Conflict`: Email is already registered in central `user_accounts`.
    - `429 Too Many Requests`: Resend cooldown active (60 seconds).
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.SignupInitiateService`
    - **Request DTO:** `com.company.GymVerse.auth.request.AuthRequest` / `SignupRequest`
    - **Response DTO:** `com.company.GymVerse.auth.response.AuthResponse`
    - **Event:** `com.company.GymVerse.messaging.kafka.events.OtpEmailEvent`
13. **Business Rules:**
    - Cooldown Rate Limiting: 60-second minimum cooldown before another OTP can be requested for the same email.
    - Pending Profile Caching: Basic profile details cached in Redis for 15 minutes (`gymverse:signup:pending:{email}`).

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "SIGNUP_INITIATE",
       "signupRequest": {
         "email": "alex.walker@example.com",
         "firstName": "Alex",
         "lastName": "Walker",
         "phoneNumber": "+15551234567"
       }
     }'
```

---

<a id="op-2-2"></a>
<a id="signup-verify"></a>
### 2.2 Signup Verify OTP (`SIGNUP_VERIFY`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 2 of Master user registration. Validates submitted 6-digit OTP against Redis. On match, establishes a 15-minute verified token state in Redis.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "SIGNUP_VERIFY",
     "signupRequest": {
       "email": "alex.walker@example.com",
       "otp": "482910"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`SIGNUP_VERIFY`).
   - `email`: Required, valid RFC 5322 email.
   - `otp`: Required, exactly 6 numeric digits.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Email successfully verified. Please submit your password to complete registration."
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Invalid OTP code (reports remaining attempts), expired OTP, or attempts exceeded lockout.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.SignupVerifyService`
    - **Service:** `com.company.GymVerse.otp.service.OtpService`
13. **Business Rules:**
    - 5 Max Attempts: After 5 incorrect attempts, OTP is permanently evicted from Redis.
    - Verified State: On successful verification, writes `gymverse:otp:verified:SIGNUP:{email}` with 15m TTL.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "SIGNUP_VERIFY",
       "signupRequest": {
         "email": "alex.walker@example.com",
         "otp": "482910"
       }
     }'
```

---

<a id="op-2-3"></a>
<a id="signup-complete"></a>
### 2.3 Signup Complete (`SIGNUP_COMPLETE`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 3 of Master user registration. Validates verified email state, enforces password complexity, encodes password via BCrypt, provisions central `UserAccount` record, consumes verified OTP token, and issues initial `MASTER_ACCOUNT` JWT access and refresh tokens.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "SIGNUP_COMPLETE",
     "signupRequest": {
       "email": "alex.walker@example.com",
       "password": "SecurePassword#2026",
       "firstName": "Alex",
       "lastName": "Walker",
       "phoneNumber": "+15551234567"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`SIGNUP_COMPLETE`).
   - `email`: Required, valid RFC 5322 email.
   - `password`: Required, min 8 chars, max 100 chars, containing at least 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Account registration completed successfully",
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "user": {
          "id": 10,
          "email": "alex.walker@example.com",
          "firstName": "Alex",
          "lastName": "Walker",
          "phoneNumber": "+15551234567",
          "status": "ACTIVE"
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Email not verified, weak password, or missing required fields.
    - `409 Conflict`: Account already exists.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.SignupCompleteService`
    - **Security:** `com.company.GymVerse.security.jwt.JwtUtil`
13. **Business Rules:**
    - Central Transaction: Executes under `centralTransactionManager` persisting to `user_accounts`.
    - Privilege Escalation Guard: `isPlatformAdmin` strictly set to `false`.
    - Single-Use Verification: Consumes verified token state from Redis upon account creation.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "SIGNUP_COMPLETE",
       "signupRequest": {
         "email": "alex.walker@example.com",
         "password": "SecurePassword#2026",
         "firstName": "Alex",
         "lastName": "Walker",
         "phoneNumber": "+15551234567"
       }
     }'
```

---

<a id="op-2-4"></a>
<a id="login-password"></a>
### 2.4 Password Login (`LOGIN_PASSWORD`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Authenticates existing Master DB user via email and password using Spring Security `AuthenticationManager`, enforces account active status check, and issues `MASTER_ACCOUNT` access and refresh tokens.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "LOGIN_PASSWORD",
     "loginRequest": {
       "email": "alex.walker@example.com",
       "password": "SecurePassword#2026"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`LOGIN_PASSWORD`).
   - `email`: Required, valid RFC 5322 email.
   - `password`: Required, non-blank string.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Login successful",
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "user": {
          "id": 10,
          "email": "alex.walker@example.com",
          "firstName": "Alex",
          "lastName": "Walker",
          "phoneNumber": "+15551234567",
          "status": "ACTIVE"
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Account is suspended or inactive (`status != ACTIVE`).
    - `401 Unauthorized`: Bad credentials (incorrect email or password).
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.LoginPasswordService`
    - **Security:** `org.springframework.security.authentication.AuthenticationManager`
13. **Business Rules:**
    - Inactive Account Guard: Suspended or deactivated accounts are blocked from generating tokens.
    - Token Claims: Embeds `sub: <userId>`, `tokenType: MASTER_ACCOUNT`, `type: ACCESS` / `REFRESH`.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "LOGIN_PASSWORD",
       "loginRequest": {
         "email": "alex.walker@example.com",
         "password": "SecurePassword#2026"
       }
     }'
```

---

<a id="op-2-5"></a>
<a id="login-otp-initiate"></a>
### 2.5 Passwordless Login OTP Initiate (`LOGIN_OTP_INITIATE`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 1 of passwordless OTP login. Verifies registered active account existence, and dispatches a 6-digit OTP code to the registered email via Kafka.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "LOGIN_OTP_INITIATE",
     "loginRequest": {
       "email": "alex.walker@example.com"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`LOGIN_OTP_INITIATE`).
   - `email`: Required, valid RFC 5322 email.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Login verification OTP sent to your email"
      }
      ```
11. **Error Responses:**
    - `404 Not Found`: No registered account found with provided email.
    - `400 Bad Request`: Account suspended or inactive.
    - `429 Too Many Requests`: 60s resend cooldown active.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.LoginOtpInitiateService`
13. **Business Rules:**
    - Cooldown Rate Limiting: 60-second cooldown enforced between dispatch requests.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "LOGIN_OTP_INITIATE",
       "loginRequest": {
         "email": "alex.walker@example.com"
       }
     }'
```

---

<a id="op-2-6"></a>
<a id="login-otp-verify"></a>
### 2.6 Passwordless Login OTP Verify (`LOGIN_OTP_VERIFY`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 2 of passwordless OTP login. Validates submitted OTP against Redis, consumes verified token state, and issues `MASTER_ACCOUNT` access and refresh tokens.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "LOGIN_OTP_VERIFY",
     "loginRequest": {
       "email": "alex.walker@example.com",
       "otp": "951234"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`LOGIN_OTP_VERIFY`).
   - `email`: Required, valid RFC 5322 email.
   - `otp`: Required, exactly 6 numeric digits.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Login successful",
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "user": {
          "id": 10,
          "email": "alex.walker@example.com",
          "firstName": "Alex",
          "lastName": "Walker",
          "phoneNumber": "+15551234567",
          "status": "ACTIVE"
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Invalid OTP (reports remaining attempts), expired OTP, or attempts exceeded lockout.
    - `404 Not Found`: Account not found.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.LoginOtpVerifyService`
13. **Business Rules:**
    - Single-Use Verification: Consumes verified token state from Redis immediately after issuing JWTs.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "LOGIN_OTP_VERIFY",
       "loginRequest": {
         "email": "alex.walker@example.com",
         "otp": "951234"
       }
     }'
```

---

<a id="op-2-7"></a>
<a id="refresh-token"></a>
### 2.7 Token Refresh (`REFRESH_TOKEN`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Renews an expired access token using a valid `MASTER_ACCOUNT` refresh token. Validates signature, expiry, token type, and account status, and rotates tokens.
5. **Authentication & Authorization:** Public access (`permitAll()`). Requires valid refresh token in payload.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "REFRESH_TOKEN",
     "loginRequest": {
       "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`REFRESH_TOKEN`).
   - `refreshToken`: Required, non-blank string. Must be a valid JJWT with claims `type: REFRESH` and `tokenType: MASTER_ACCOUNT`.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Token refreshed successfully",
        "accessToken": "eyJhbGciOiJIUzI1NiJ9.newAccess...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9.newRefresh...",
        "user": {
          "id": 10,
          "email": "alex.walker@example.com",
          "firstName": "Alex",
          "lastName": "Walker",
          "phoneNumber": "+15551234567",
          "status": "ACTIVE"
        }
      }
      ```
11. **Error Responses:**
    - `401 Unauthorized`: Token is not a refresh token, expired, tampered, or mismatched token type.
    - `400 Bad Request`: Associated user account is suspended or inactive.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.RefreshTokenService`
    - **Security:** `com.company.GymVerse.security.jwt.JwtUtil`
13. **Business Rules:**
    - Strict Token Type Guard: Reject access tokens passed in as refresh tokens.
    - Account Active Check: Verify user account is active in central DB before reissuing tokens.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "REFRESH_TOKEN",
       "loginRequest": {
         "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
       }
     }'
```

---

<a id="op-2-8"></a>
<a id="forgot-password-initiate"></a>
### 2.8 Forgot Password Initiate (`FORGOT_PASSWORD_INITIATE`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 1 of password recovery. Validates registered active account existence, and dispatches a password recovery OTP code via Kafka.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "FORGOT_PASSWORD_INITIATE",
     "forgotPasswordRequest": {
       "email": "alex.walker@example.com"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`FORGOT_PASSWORD_INITIATE`).
   - `email`: Required, valid RFC 5322 email.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Password reset OTP has been sent to your email"
      }
      ```
11. **Error Responses:**
    - `404 Not Found`: No account registered with provided email.
    - `400 Bad Request`: Account is suspended or inactive.
    - `429 Too Many Requests`: 60s resend cooldown active.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.ForgotPasswordInitiateService`
13. **Business Rules:**
    - 60s Cooldown: Rate limiting enforced via Redis cooldown key.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "FORGOT_PASSWORD_INITIATE",
       "forgotPasswordRequest": {
         "email": "alex.walker@example.com"
       }
     }'
```

---

<a id="op-2-9"></a>
<a id="forgot-password-verify"></a>
### 2.9 Forgot Password Verify OTP (`FORGOT_PASSWORD_VERIFY`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 2 of password recovery. Validates submitted recovery OTP against Redis. On match, establishes 15-minute verified token state in Redis.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "FORGOT_PASSWORD_VERIFY",
     "forgotPasswordRequest": {
       "email": "alex.walker@example.com",
       "otp": "773120"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`FORGOT_PASSWORD_VERIFY`).
   - `email`: Required, valid RFC 5322 email.
   - `otp`: Required, exactly 6 numeric digits.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "OTP verified successfully. Please submit your new password."
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Invalid OTP code (reports remaining attempts), expired OTP, or attempts exceeded lockout.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.ForgotPasswordVerifyService`
13. **Business Rules:**
    - Verified State: Writes `gymverse:otp:verified:FORGOT_PASSWORD:{email}` with 15m TTL on success.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "FORGOT_PASSWORD_VERIFY",
       "forgotPasswordRequest": {
         "email": "alex.walker@example.com",
         "otp": "773120"
       }
     }'
```

---

<a id="op-2-10"></a>
<a id="forgot-password-reset"></a>
### 2.10 Forgot Password Reset (`FORGOT_PASSWORD_RESET`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Step 3 of password recovery. Validates presence of active verified OTP token state in Redis, validates new password complexity, encodes password via BCrypt, updates `user_accounts` record under `centralTransactionManager`, and consumes verified OTP state.
5. **Authentication & Authorization:** Public access (`permitAll()`).
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "FORGOT_PASSWORD_RESET",
     "forgotPasswordRequest": {
       "email": "alex.walker@example.com",
       "newPassword": "NewSecurePassword#2026"
     }
   }
   ```
   **Validation Rules:**
   - `authRequestType`: Required enum string (`FORGOT_PASSWORD_RESET`).
   - `email`: Required, valid RFC 5322 email.
   - `newPassword`: Required, min 8 chars, max 100 chars, containing at least 1 uppercase, 1 lowercase, 1 number, and 1 special symbol.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Password reset successfully. Please login with your new credentials."
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: OTP not verified, expired, or weak new password.
    - `404 Not Found`: Account not found.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Factory Operation:** `com.company.GymVerse.auth.service.ForgotPasswordResetService`
    - **Repository:** `com.company.GymVerse.repositories.central.UserAccountRepository`
13. **Business Rules:**
    - Central Transaction: Executes under `centralTransactionManager`.
    - Single-Use Consumption: Deletes verified OTP state from Redis upon password reset completion.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "authRequestType": "FORGOT_PASSWORD_RESET",
       "forgotPasswordRequest": {
         "email": "alex.walker@example.com",
         "newPassword": "NewSecurePassword#2026"
       }
     }'
```

<a id="op-2-11"></a>
<a id="switch-tenant"></a>
### 2.11 Tenant Switching & Token Exchange (`SWITCH_TENANT`)

1. **Feature / Module:** Authentication & Core Security (`auth`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/auth`
4. **Purpose:** Authenticated token exchange airlock bridge trading an active `MASTER_ACCOUNT` token for a scoped `TENANT_ACCOUNT` token pair bound to an isolated tenant database.
5. **Authentication & Authorization:** Requires authenticated caller with `MASTER_ACCOUNT` JWT Bearer token.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <MASTER_ACCESS_TOKEN>`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "authRequestType": "SWITCH_TENANT",
     "switchTenantRequest": {
       "gymTenantId": 101,
       "gymCode": "OLYMPUS-01",
       "role": "TENANT_ADMIN"
     }
   }
   ```
   *Note: Either `gymTenantId` or `gymCode` must be provided. `role` is optional if user holds only one role in the gym, but required if user holds multiple roles.*
   **Validation Rules:**
   - `authRequestType`: Required enum string (`SWITCH_TENANT`).
   - `gymTenantId` or `gymCode`: At least one target identifier must be non-null and non-blank.
   - `role`: Optional enum (`TENANT_ADMIN`, `MEMBER`, `TRAINER`, `STAFF`). Required if user holds multiple associations in the gym.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Tenant switch successful. Scoped tokens issued for gym: Olympus Fitness",
        "accessToken": "<TENANT_SCOPED_ACCESS_TOKEN>",
        "refreshToken": "<TENANT_SCOPED_REFRESH_TOKEN>",
        "switchTenantResponse": {
          "tenantAccessToken": "<TENANT_SCOPED_ACCESS_TOKEN>",
          "tenantRefreshToken": "<TENANT_SCOPED_REFRESH_TOKEN>",
          "tenantId": 101,
          "gymCode": "OLYMPUS-01",
          "gymName": "Olympus Fitness",
          "tenantDbName": "gymverse_tenant_olympus_fitness_100201",
          "role": "TENANT_ADMIN",
          "adminAuthority": "PRIMARY"
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Missing both `gymTenantId` and `gymCode`, conflicting `gymTenantId` and `gymCode`, or user holds multiple roles in gym and `role` parameter was omitted.
    - `401 Unauthorized`: Missing or invalid Master Bearer token, or tenant-scoped token used.
    - `403 Forbidden`: Gym tenant is not active (`SUSPENDED`/`INACTIVE`) or user's gym membership is not active (`SUSPENDED`/`PENDING`/`REVOKED`).
    - `404 Not Found`: Gym tenant does not exist or user has no association with the target gym / does not hold requested role.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.auth.controller.AuthController`
    - **Operation Bean:** `com.company.GymVerse.auth.service.SwitchTenantService`
    - **Token Engine:** `com.company.GymVerse.security.jwt.JwtUtil`

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/auth" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer <MASTER_ACCESS_TOKEN>" \
     -d '{
       "authRequestType": "SWITCH_TENANT",
       "switchTenantRequest": {
         "gymCode": "OLYMPUS-01"
       }
     }'
```

---

<a id="section-3"></a>
<a id="user-module"></a>
## 3. Central User Gateway Module (`POST /api/user`)

<a id="op-3-1"></a>
<a id="register-gym"></a>
### 3.1 Register Gym (`REGISTER_GYM`)

1. **Operation Name:** Register Gym & Dynamic Database Provisioning
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `/api/user`
4. **Purpose:** Central Master User single-endpoint command gateway operation for onboarding a new gym tenant. Dynamically provisions an isolated physical MySQL database, runs tenant Flyway schema migrations, seeds the initial tenant administrator profile (with `central_user_id` and zero passwords in tenant DB), activates a 14-day free SaaS trial subscription, and immediately issues tenant-scoped access and refresh tokens.
5. **Authentication & Authorization:** Requires authenticated caller with `MASTER_ACCOUNT` JWT Bearer token.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <MASTER_ACCESS_TOKEN>`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "userRequestType": "REGISTER_GYM",
     "registerGymRequest": {
       "gymName": "Olympus Fitness",
       "contactEmail": "contact@olympus.com",
       "phoneNumber": "+1555987654",
       "address": "500 Pantheon Way",
       "city": "Athens",
       "logoUrl": "https://cdn.gymverse.com/logos/olympus.png",
       "latitude": 37.9838,
       "longitude": 23.7275,
       "radius": 200.0,
       "openingTime": "06:00:00",
       "closingTime": "22:00:00",
       "autoCheckoutTime": "22:30:00",
       "timeZone": "Europe/Athens",
       "customGymCode": "OLYMPUS-01"
     }
   }
   ```
   **Validation Rules:**
   - `userRequestType`: Required enum string (`REGISTER_GYM`).
   - `gymName`: Required string, non-blank, max 200 chars.
   - `address`: Required string, non-blank.
   - `city`: Required string, non-blank, max 100 chars.
   - `customGymCode`: Optional string, 4 to 10 alphanumeric/hyphen characters, must be unique across all tenants if provided.
   - `latitude`: Optional decimal, must be between -90 and 90.
   - `longitude`: Optional decimal, must be between -180 and 180.
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Gym registered successfully and 14-day free trial activated",
        "tenantId": 101,
        "gymCode": "OLYMPUS-01",
        "gymName": "Olympus Fitness",
        "databaseName": "gymverse_tenant_olympus_fitness_100201",
        "tenantAccessToken": "<TENANT_SCOPED_ACCESS_TOKEN>",
        "tenantRefreshToken": "<TENANT_SCOPED_REFRESH_TOKEN>",
        "trialEndDate": "2026-12-02",
        "trialDaysRemaining": 14,
        "registerGym": {
          "tenantId": 101,
          "gymCode": "OLYMPUS-01",
          "gymName": "Olympus Fitness",
          "databaseName": "gymverse_tenant_olympus_fitness_100201",
          "tenantAccessToken": "<TENANT_SCOPED_ACCESS_TOKEN>",
          "tenantRefreshToken": "<TENANT_SCOPED_REFRESH_TOKEN>",
          "trialEndDate": "2026-12-02",
          "trialDaysRemaining": 14,
          "role": "TENANT_ADMIN",
          "adminAuthority": "PRIMARY"
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Missing required fields (gymName, address, city) or duplicate/invalid custom gym code.
    - `401 Unauthorized`: Missing or invalid Bearer token.
    - `500 Internal Server Error`: Tenant database provisioning or Flyway migration failure (triggers automatic compensation rollback to drop orphan database).
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.user.controller.UserController`
    - **Factory Operation:** `com.company.GymVerse.user.service.RegisterGymService`
    - **Orchestration Service:** `com.company.GymVerse.user.service.TenantProvisioningService`
    - **Validation:** `com.company.GymVerse.user.validations.UserValidations`, `com.company.GymVerse.user.validations.TenantProvisioningValidations`
    - **Mapper:** `com.company.GymVerse.user.mapper.UserMapper`
13. **Business Rules:**
    - **Master-Scoped Lifecycle:** Executed by authenticated master user identity against central catalog before any tenant context is bound.
    - **Zero Passwords in Tenant Database:** Admin operational profile references master identity via `central_user_id`.
    - **Automatic SaaS Trial:** Grants 14 days on default plan ID 1 with 0.00 price paid.
    - **Zero-Lookup Token:** Returned JWT contains `tenantDbName`, enabling zero-central-DB-lookup dynamic routing on subsequent tenant API calls.
    - **Client Neutrality:** The backend provides a single, uniform contract without enforcing client platform headers; client presentation is managed at the UI/frontend level.

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/user" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer <MASTER_ACCESS_TOKEN>" \
     -d '{
       "userRequestType": "REGISTER_GYM",
       "registerGymRequest": {
         "gymName": "Olympus Fitness",
         "contactEmail": "contact@olympus.com",
         "phoneNumber": "+1555987654",
         "address": "500 Pantheon Way",
         "city": "Athens",
         "logoUrl": "https://cdn.gymverse.com/logos/olympus.png",
         "latitude": 37.9838,
         "longitude": 23.7275,
         "radius": 200.0,
         "openingTime": "06:00:00",
         "closingTime": "22:00:00",
         "timeZone": "Europe/Athens"
       }
     }'
```

<a id="op-3-2"></a>
<a id="fetch-my-tenants"></a>
### 3.2 Fetch My Tenants Directory (`FETCH_MY_TENANTS`)

1. **Operation Name:** Fetch User Tenant Directory
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `/api/user`
4. **Purpose:** Central Master User single-endpoint query operation that returns all gym franchises/centers where the authenticated user holds an association (Admin, Member, Trainer, Staff), complete with operational status, authority level, and active SaaS subscription status.
5. **Authentication & Authorization:** Requires authenticated caller with `MASTER_ACCOUNT` JWT Bearer token.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <MASTER_ACCESS_TOKEN>`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "userRequestType": "FETCH_MY_TENANTS",
     "fetchMyTenantsRequest": {
       "role": "TENANT_ADMIN",
       "gymStatus": "ACTIVE"
     }
   }
   ```
   *Note: `fetchMyTenantsRequest` is optional. An empty body `{ "userRequestType": "FETCH_MY_TENANTS" }` returns all associated tenants without filters.*
   **Validation Rules:**
   - `userRequestType`: Required enum string (`FETCH_MY_TENANTS`).
   - `role`: Optional enum string (`TENANT_ADMIN`, `MEMBER`, `TRAINER`, `STAFF`).
   - `gymStatus`: Optional enum string (`ACTIVE`, `SUSPENDED`).
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "User tenant directory retrieved successfully",
        "status": 200,
        "fetchMyTenantsResponse": {
          "totalTenants": 1,
          "tenants": [
            {
              "tenantId": 101,
              "gymCode": "OLYMPUS-01",
              "gymName": "Olympus Fitness",
              "logoUrl": "https://cdn.gymverse.com/logos/olympus.png",
              "city": "Athens",
              "address": "500 Pantheon Way",
              "userRole": "TENANT_ADMIN",
              "adminAuthority": "PRIMARY",
              "roleStatus": "ACTIVE",
              "gymStatus": "ACTIVE",
              "subscriptionStatus": "TRIAL",
              "joinedAt": "2026-11-19T06:00:00Z"
            }
          ]
        }
      }
      ```
11. **Error Responses:**
    - `401 Unauthorized`: Missing or invalid Bearer token.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.user.controller.UserController`
    - **Operation Bean:** `com.company.GymVerse.user.service.FetchMyTenantsService`

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/user" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer <MASTER_ACCESS_TOKEN>" \
     -d '{
       "userRequestType": "FETCH_MY_TENANTS"
     }'
```

<a id="op-3-3"></a>
<a id="fetch-gym-by-code"></a>
### 3.3 Fetch Gym Details by Code (`FETCH_GYM_BY_CODE`)

1. **Operation Name:** Fetch Gym Details by Code (Discovery Gateway)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `/api/user`
4. **Purpose:** Allows an authenticated central master user to look up and inspect verified gym profile details using a unique discovery `gymCode` (scanned from a QR code or entered manually). Validates that the gym exists, is currently `ACTIVE`, and has an active or trial SaaS subscription before displaying the gym card on the client UI.
5. **Authentication & Authorization:** Requires authenticated caller with `MASTER_ACCOUNT` JWT Bearer token. Tenant tokens (`TENANT_ACCOUNT`) or anonymous requests are rejected with 401 Unauthorized.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <MASTER_ACCESS_TOKEN>`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "requestType": "FETCH_GYM_BY_CODE",
     "fetchGymByCodeRequest": {
       "gymCode": "FIT-101"
     }
   }
   ```
   **Validation Rules:**
   - `requestType` (or `userRequestType`): String enum, required (`FETCH_GYM_BY_CODE`).
   - `fetchGymByCodeRequest`: Object, required.
   - `gymCode`: String, required, non-blank, 4 to 10 alphanumeric characters.
10. **Success Response (`200 OK`):**
    ```json
    {
      "message": "Gym details retrieved successfully",
      "status": 200,
      "timestamp": "2026-11-20T15:37:12Z",
      "fetchGymByCodeResponse": {
        "tenantId": 101,
        "gymCode": "FIT-101",
        "gymName": "Iron Paradise Gym",
        "address": "77 Muscle Beach",
        "city": "Venice",
        "phoneNumber": "+1-555-0199",
        "contactEmail": "info@ironparadise.com",
        "logoUrl": "https://cdn.gymverse.com/logos/iron-paradise.png",
        "status": "ACTIVE",
        "subscriptionStatus": "ACTIVE"
      }
    }
    ```
11. **Error Responses:**
    - `400 Bad Request`:
      - Missing or blank `gymCode`.
      - Target gym is `SUSPENDED` or `INACTIVE` (`Gym 'XYZ' is currently SUSPENDED and not accepting new members`).
      - Target gym SaaS subscription is `EXPIRED` or `CANCELLED`.
    - `401 Unauthorized`: Missing, expired, or invalid Master Account Bearer token.
    - `404 Not Found`: No gym exists with the requested `gymCode` (`Gym not found with code: XYZ`).
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.user.controller.UserController`
    - **Operation Bean:** `com.company.GymVerse.user.service.FetchGymByCodeService`
    - **Validations Component:** `com.company.GymVerse.user.validations.UserValidationsImpl`
    - **Helper Component:** `com.company.GymVerse.user.util.UserHelper`
    - **Mapper:** `com.company.GymVerse.user.mapper.UserMapperImpl`

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/user" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer <MASTER_ACCESS_TOKEN>" \
     -d '{
       "requestType": "FETCH_GYM_BY_CODE",
       "fetchGymByCodeRequest": {
         "gymCode": "FIT-101"
       }
     }'
```

---

<a id="section-4"></a>
<a id="tenant-admin-module"></a>
## 4. Tenant Administration Module (`POST /api/tenant-admin`)

<a id="op-4-1"></a>
<a id="fetch-tenant-subscription"></a>
### 4.1 Fetch Tenant SaaS Subscription & Entitlements (`FETCH_TENANT_SUBSCRIPTION`)

1. **Feature / Module:** Tenant Administration (`tenantAdmin`)
2. **HTTP Method:** `POST`
3. **Endpoint URL:** `http://localhost:8080/api/tenant-admin`
4. **Purpose:** Retrieves real-time SaaS subscription tier status, trial/billing countdown, plan pricing, and feature entitlement limits for the operational gym tenant.
5. **Authentication & Authorization:** Requires authenticated caller with `TENANT_ACCOUNT` token and `ROLE_TENANT_ADMIN` authority. Master tokens (`MASTER_ACCOUNT`) or non-admin roles (`MEMBER`, `TRAINER`, `STAFF`) are strictly rejected with 403 Forbidden.
6. **Request Headers:**
   - `Content-Type: application/json`
   - `Accept: application/json`
   - `Authorization: Bearer <TENANT_SCOPED_ACCESS_TOKEN>`
7. **Path Parameters:** None
8. **Query Parameters:** None
9. **Request Payload (`application/json`):**
   ```json
   {
     "tenantAdminRequestType": "FETCH_TENANT_SUBSCRIPTION",
     "fetchTenantSubscriptionRequest": {
       "includeHistory": false
     }
   }
   ```
   *Note: `fetchTenantSubscriptionRequest` is optional. Tenant identity is resolved securely from caller JWT token claims.*
   **Validation Rules:**
   - `tenantAdminRequestType`: Required enum string (`FETCH_TENANT_SUBSCRIPTION`).
10. **Success Response:**
    - **HTTP Status:** `200 OK`
    - **Response Body (`application/json`):**
      ```json
      {
        "message": "Tenant subscription details retrieved successfully",
        "status": 200,
        "timestamp": "2026-11-19T14:45:00Z",
        "tenantSubscriptionDetailsResponse": {
          "subscriptionId": 901,
          "tenantId": 101,
          "gymCode": "GOLD-01",
          "gymName": "Gold Standard Gym",
          "planId": 1,
          "planName": "Growth Tier",
          "planDescription": "All-inclusive growth plan for expanding clubs",
          "planPrice": 149.99,
          "pricePaid": 0.00,
          "billingCycle": "MONTHLY",
          "status": "TRIAL",
          "startDate": "2026-11-14",
          "endDate": "2026-11-28",
          "daysRemaining": 9,
          "isTrial": true,
          "maxMembers": 500,
          "maxTrainers": 15,
          "analyticsEnabled": true,
          "multiTrainerEnabled": true,
          "customBrandingEnabled": true
        }
      }
      ```
11. **Error Responses:**
    - `400 Bad Request`: Missing or null `tenantAdminRequestType`.
    - `401 Unauthorized`: Missing or invalid Bearer token.
    - `403 Forbidden`: Master account token used instead of tenant-scoped token, or caller lacks `ROLE_TENANT_ADMIN`.
    - `404 Not Found`: No subscription record found for the tenant ID in Central DB.
12. **Implementation Reference:**
    - **Controller:** `com.company.GymVerse.tenantAdmin.controller.TenantAdminController`
    - **Factory:** `com.company.GymVerse.tenantAdmin.factory.TenantAdminFactory`
    - **Operation Bean:** `com.company.GymVerse.tenantAdmin.service.FetchTenantSubscriptionService`
    - **Validator:** `com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations`
    - **Mapper:** `com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper`

#### Copy-Pasteable cURL Command
```bash
curl -X POST "http://localhost:8080/api/tenant-admin" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "Authorization: Bearer <TENANT_SCOPED_ACCESS_TOKEN>" \
     -d '{
       "tenantAdminRequestType": "FETCH_TENANT_SUBSCRIPTION"
     }'
```


> **Note:** The Tenant Administration module handles In-Gym Operational management (Tier 2 Tenant-Scoped) requiring an authenticated caller with a `TENANT_ACCOUNT` token and `ROLE_TENANT_ADMIN`. Operational endpoints (e.g. Story 4.6 subscription insights and plan upgrades) are documented here.
```
