# GymVerse — Target Technical Architecture & Controlled Refactoring Specification

> **Document Status:** Architectural Target Specification (Revision v2.1 — Unified Identity & Tenant Administration)  
> **Audience:** Engineering Leadership, System Architects, Backend Engineers  
> **Author:** Winston, System Architect 🏗️  
> **Version:** 2.1 (Target)  
> **Project Repository:** `GymVerse-Backend-v2`  
> **Date:** October 2026

---

## 1. Architectural Mission & Core Paradigms

GymVerse is a multi-tenant Gym Management SaaS platform built as a secure, scalable, production-grade modular monolith. 

### 1.1 Invariants & Non-Negotiables
1. **Modular Monolith:** Single deployable Spring Boot application with strictly packaged domain modules, avoiding microservice operational overhead while enforcing clean architectural boundaries.
2. **Database-Per-Tenant Isolation:** Complete physical database separation for each gym tenant (`gym_db_<id>`), backed by a single central platform database (`GymVerse`).
3. **Single Central Identity (`user_accounts`):** Exactly one central identity system for every human who uses GymVerse. The legacy separation between `accounts` and `user_accounts` is retired. All authentication credentials, OTPs, and profile baselines reside centrally.
4. **No Owner Role:** The separate `OWNER` authorization role is eliminated. Business, legal, and operational governance derives from a tenant-level administrative relationship (`TENANT_ADMIN`).
5. **Multi-Admin Hierarchy per Tenant:** Tenants support multiple active administrators. Authority is governed by an explicit attribute: **Primary Tenant Administrator** (exactly one active per tenant) versus **Delegated Tenant Administrators** (zero or more).
6. **Zero-Trust Server-Side Tenant Resolution:** The application never trusts client-provided database names or arbitrary tenant headers. The physical database is resolved solely through verified central mappings and active subscription checks.
7. **Two-Tier Authentication & Token Exchange:** Central authentication produces a central token (`CENTRAL_USER`). Accessing a gym requires exchanging the central token for a verified, cryptographically signed `TENANT` token.
8. **Zero Credential Duplication:** Passwords exist solely in the central database. Tenant databases contain only profile data and stable linking identifiers (`central_user_id`).

---

## 2. System Architecture & High-Level Topology

```mermaid
graph TD
    Client["Web / Mobile Clients"]

    subgraph "Ingress & Security"
        SecurityFilter["SecurityFilterChain"]
        JwtFilter["JwtFilter (Type & Scope Guard)"]
        TenantResolver["Tenant Resolution & Context Guard"]
    end

    subgraph "Modular Application Layer (Spring Boot 3.3.5)"
        subgraph "Central / Platform Domain"
            UserAuthMod["userAuthentication (Unified Central Auth & OTP)"]
            GymMgmtMod["gymManagement & Provisioning"]
            SubscriptionMod["saasSubscriptions (Plans & Billing)"]
            AdminGovMod["adminGovernance (Appointment & Transfers)"]
        end

        subgraph "Tenant Operational Domain"
            TenantAdminMod["tenantAdminFunctionality"]
            MemberMod["membersFunctionality (Attendance, Friends, Streaks)"]
            TrainerMod["trainerFunctionality (Dashboards, Client Tracking)"]
            StaffMod["staffFunctionality (Check-in Overrides, Renewals)"]
            OnboardingMod["onboarding (Join Requests & Invitations)"]
        end

        subgraph "Infrastructure & Multi-Tenancy Core"
            TenantCtx["TenantContext (ThreadLocal Lifecycle Guard)"]
            ConnProvider["GymVerseMultiTenantConnectionProvider"]
            HikariPoolCache["TenantDataSourceCache (HikariCP)"]
            FlywaySvc["TenantFlywayService (Additive Migrations)"]
            EventBus["Kafka Event Pipeline (Async Notifications & OTP)"]
            CacheKV["Redis (OTP, Rate Limits, Token Blacklist)"]
        end
    end

    subgraph "Persistence Layer"
        CentralDB[("Central MySQL DB: GymVerse")]
        TenantDB1[("Tenant MySQL DB: gym_db_001")]
        TenantDB2[("Tenant MySQL DB: gym_db_002")]
        TenantDBN[("Tenant MySQL DB: gym_db_n")]
    end

    Client --> SecurityFilter
    SecurityFilter --> JwtFilter
    JwtFilter --> TenantResolver
    TenantResolver --> TenantCtx

    UserAuthMod --> CentralDB
    GymMgmtMod --> CentralDB
    SubscriptionMod --> CentralDB
    AdminGovMod --> CentralDB

    TenantAdminMod --> ConnProvider
    MemberMod --> ConnProvider
    TrainerMod --> ConnProvider
    StaffMod --> ConnProvider
    OnboardingMod --> ConnProvider

    ConnProvider --> HikariPoolCache
    HikariPoolCache --> CentralDB
    HikariPoolCache --> TenantDB1
    HikariPoolCache --> TenantDB2
    HikariPoolCache --> TenantDBN

    FlywaySvc --> TenantDB1
    FlywaySvc --> TenantDB2
    FlywaySvc --> TenantDBN

    UserAuthMod --> EventBus
    AdminGovMod --> EventBus
    UserAuthMod --> CacheKV
```

---

## 3. Unified Identity & Tenant Administration Model

```mermaid
classDiagram
    class UserAccount {
        +Long id
        +String email
        +String firstName
        +String lastName
        +String passwordHash
        +String phoneNumber
        +boolean emailVerified
        +boolean isPlatformAdmin
        +UserAccountStatus status
        +LocalDateTime createdAt
    }

    class UserTenantRole {
        +Long id
        +Long userAccountId
        +Long gymTenantId
        +TenantRole role
        +AdminAuthority adminAuthority
        +TenantMembershipStatus status
        +LocalDateTime joinedAt
        +LocalDateTime updatedAt
    }

    class GymTenant {
        +Long id
        +String gymName
        +String contactEmail
        +String phoneNumber
        +String address
        +String city
        +String gymCode
        +String dbName
        +GymStatus status
        +LocalDateTime registeredAt
    }

    class GymSubscription {
        +Long id
        +Long gymTenantId
        +Long planId
        +double pricePaid
        +BillingCycle billingCycle
        +LocalDate startDate
        +LocalDate endDate
        +GymSubscriptionStatus status
    }

    class AdminAuditLog {
        +Long id
        +Long gymTenantId
        +Long actorUserAccountId
        +Long targetUserAccountId
        +AdminAuditEvent action
        +String metadata
        +LocalDateTime timestamp
    }

    UserAccount "1" --> "0..*" UserTenantRole : assigned
    GymTenant "1" <-- "0..*" UserTenantRole : authorizes
    GymTenant "1" --> "1..*" GymSubscription : holds
    GymTenant "1" --> "0..*" AdminAuditLog : logs
```

### 3.1 Single Central Identity (`user_accounts`)
- **Scope:** Every human using GymVerse.
- **Capabilities:**
  - Can register a gym and become its Primary Administrator.
  - Can be appointed as a Delegated Administrator of another gym.
  - Can join one or more gyms as a Member.
  - Can be hired as a Trainer or Staff member in one or more gyms.
- **Credentials:** A single email, password, and multi-factor/OTP verification lifecycle.

### 3.2 Tenant Administrative Hierarchy
- **Primary Tenant Administrator (`TENANT_ADMIN` with `adminAuthority = PRIMARY`):**
  - Assigned upon registering the gym tenant.
  - Exactly **one active** Primary Administrator per gym tenant.
  - Has sovereign authority to:
    - Manage SaaS subscription, billing cycles, and payment methods.
    - Appoint Delegated Administrators from existing central identities or via invite.
    - Revoke Delegated Administrators.
    - Initiate and execute an explicit, audited transfer of primary authority to an active Delegated Administrator.
- **Delegated Tenant Administrator (`TENANT_ADMIN` with `adminAuthority = DELEGATED`):**
  - Appointed by the Primary Administrator.
  - Zero or more active per gym tenant.
  - Has full operational authority over daily facility operations (memberships, join requests, invitations, finances, geofencing).
  - Restricted from appointing/revoking other administrators, transferring primary authority, or deleting/suspending the gym tenant.

### 3.3 Tenant-Scoped Role Taxonomy
Roles exist strictly within the context of a specific gym tenant:
1. `TENANT_ADMIN`: Primary or delegated facility governance.
2. `MEMBER`: Workout facility access, check-ins, social streaks.
3. `TRAINER`: Client assignment, workout logs, trainer dashboards.
4. `STAFF`: Front-desk check-in overrides, membership plan sales.

---

## 4. Database Schema Specifications

### 4.1 Central Platform Schema (`GymVerse`)

```mermaid
erDiagram
    user_accounts ||--o{ user_tenant_roles : "holds"
    gym_tenants ||--o{ user_tenant_roles : "authorizes"
    gym_tenants ||--o{ gym_subscriptions : "subscribes"
    saas_subscription_plans ||--o{ gym_subscriptions : "defines"
    gym_tenants ||--o{ admin_audit_logs : "records"

    user_accounts {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR password
        VARCHAR phone_number
        BOOLEAN email_verified
        BOOLEAN is_platform_admin
        VARCHAR status
        DATETIME created_at
        DATETIME updated_at
    }

    user_tenant_roles {
        BIGINT id PK
        BIGINT user_account_id FK
        BIGINT gym_tenant_id FK
        VARCHAR role
        VARCHAR admin_authority
        VARCHAR status
        DATETIME joined_at
        DATETIME updated_at
    }

    gym_tenants {
        BIGINT id PK
        VARCHAR gym_name
        VARCHAR contact_email
        VARCHAR phone_number
        VARCHAR address
        VARCHAR city
        VARCHAR logo_url
        VARCHAR gym_code UK
        VARCHAR db_name UK
        VARCHAR status
        DATETIME registered_at
        DATETIME suspended_at
        TEXT suspension_reason
    }

    saas_subscription_plans {
        BIGINT id PK
        VARCHAR name
        TEXT description
        DOUBLE price
        VARCHAR billing_cycle
        INT max_members
        INT max_trainers
        BOOLEAN analytics_enabled
        BOOLEAN multi_trainer_enabled
        BOOLEAN custom_branding_enabled
        BOOLEAN active
    }

    gym_subscriptions {
        BIGINT id PK
        BIGINT gym_tenant_id FK
        BIGINT plan_id FK
        DOUBLE price_paid
        VARCHAR billing_cycle
        DATE start_date
        DATE end_date
        VARCHAR status
        DATETIME subscribed_at
        VARCHAR transaction_reference
    }

    admin_audit_logs {
        BIGINT id PK
        BIGINT gym_tenant_id FK
        BIGINT actor_user_account_id FK
        BIGINT target_user_account_id FK
        VARCHAR action
        TEXT metadata
        DATETIME timestamp
    }
```

#### Central Constraints & Invariants:
1. `uq_utr_user_gym_role`: `UNIQUE(user_account_id, gym_tenant_id, role)` on `user_tenant_roles`.
2. `idx_utr_user_status`: Index on `user_tenant_roles(user_account_id, status)`.
3. `idx_utr_gym_role`: Index on `user_tenant_roles(gym_tenant_id, role, admin_authority)`.
4. **Primary Admin Invariant:** A filtered unique index or application-layer guarded transaction enforces that at most one row per `gym_tenant_id` can have `role = 'TENANT_ADMIN'` AND `admin_authority = 'PRIMARY'` AND `status = 'ACTIVE'`.
5. `idx_gym_code`: `UNIQUE(gym_code)` on `gym_tenants`.
6. `idx_db_name`: `UNIQUE(db_name)` on `gym_tenants`.

---

### 4.2 Tenant Operational Schema (`gym_db_<id>`)

```mermaid
erDiagram
    users ||--o| members : "member profile"
    users ||--o{ membership_requests : "requests"
    members ||--o{ member_memberships : "purchases"
    membership_plans ||--o{ member_memberships : "defines"
    members ||--o{ attendance : "records"
    member_memberships ||--o| revenue : "generates"
    members ||--o{ friend_requests : "sends/receives"
    members ||--o{ custom_friends : "friends with"
    members ||--o{ friend_streaks : "streaks with"

    users {
        BIGINT id PK
        BIGINT central_user_id UK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email UK
        VARCHAR phone_number
        VARCHAR role
        BOOLEAN active
        DATETIME created_at
    }

    gym_config {
        BIGINT id PK
        DOUBLE latitude
        DOUBLE longitude
        DOUBLE radius
        VARCHAR time_zone
        TIME opening_time
        TIME closing_time
        TIME auto_checkout_time
    }

    members {
        BIGINT id PK
        BIGINT user_id FK,UK
        BIGINT trainer_id FK
        BOOLEAN independent
        DOUBLE height
        DOUBLE weight
        VARCHAR goal
        DATE join_date
        VARCHAR gym_id UK
        VARCHAR type
        DATE trial_end_date
        VARCHAR specialization
    }

    membership_plans {
        BIGINT id PK
        VARCHAR name
        TEXT description
        VARCHAR tier
        VARCHAR facility
        DOUBLE base_price
        VARCHAR discount_type
        DOUBLE discount_value
        DOUBLE final_price
        INT duration_days
        BOOLEAN active
    }

    member_memberships {
        BIGINT id PK
        BIGINT member_id FK
        BIGINT plan_id FK
        DOUBLE price_at_purchase
        VARCHAR discount_type
        DOUBLE discount_value
        DOUBLE final_price
        DATE start_date
        DATE end_date
        BOOLEAN active
    }

    membership_requests {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT plan_id FK
        VARCHAR status
        DATETIME request_date
        TEXT member_notes
        TEXT admin_notes
        DATETIME reviewed_at
        BIGINT reviewed_by_admin_id
    }

    attendance {
        BIGINT id PK
        BIGINT member_id FK
        DATE date
        DATETIME check_in_time
        DATETIME check_out_time
        BOOLEAN auto_checked_out
        VARCHAR status
    }

    revenue {
        BIGINT id PK
        BIGINT member_membership_id FK,UK
        DOUBLE amount
        VARCHAR payment_mode
        VARCHAR transaction_reference
        DATETIME received_at
        BIGINT recorded_by_admin_id
    }

    expenses {
        BIGINT id PK
        VARCHAR title
        TEXT description
        DOUBLE amount
        VARCHAR category
        DATE expense_date
        BIGINT recorded_by_admin_id
    }

    join_requests {
        BIGINT id PK
        BIGINT central_user_id
        VARCHAR email
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR phone_number
        VARCHAR status
        TEXT request_notes
        TEXT admin_notes
        DATETIME created_at
        DATETIME reviewed_at
        BIGINT reviewed_by_admin_id
    }

    gym_invitations {
        BIGINT id PK
        VARCHAR email
        VARCHAR role
        VARCHAR invitation_token UK
        VARCHAR status
        DATETIME expires_at
        DATETIME created_at
        DATETIME accepted_at
        BIGINT created_by_admin_id
    }

    friend_requests {
        BIGINT id PK
        BIGINT sender_member_id
        BIGINT receiver_member_id
        VARCHAR status
    }

    custom_friends {
        BIGINT id PK
        BIGINT user_member_id
        BIGINT friend_member_id
        DATETIME created_at
    }

    friend_streaks {
        BIGINT id PK
        BIGINT member1_id
        BIGINT member2_id
        INT current_streak
        INT longest_streak
        DATE last_streak_date
        INT together_check_ins
        INT together_check_outs
        DATE last_checkout_together_date
    }
```

---

## 5. Token Contract & Authorization Architecture

### 5.1 Token Classification Matrix

| Token Name | Token Scope | Issued By | `sub` Claim | Primary Claims | Intended Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Central Access Token** | Platform / Global | `USER_LOGIN_ENDPOINT` | `user:<id>` | `tokenType: CENTRAL_USER`, `type: ACCESS`, `isPlatformAdmin: boolean` | Query accessible gyms, register gyms, exchange for tenant-scoped token |
| **Tenant Access Token** | Gym / Tenant | `EXCHANGE_TOKEN_ENDPOINT` | `user:<id>` | `tokenType: TENANT`, `tenantId: <id>`, `uid: <local_user_id>`, `role: <ROLE>`, `adminAuthority: PRIMARY/DELEGATED/NONE`, `type: ACCESS` | Access operational gym APIs (Member, Trainer, Staff, Tenant-Admin) |
| **Central Refresh Token** | Global Refresh | Central Login | `user:<id>` | `type: REFRESH`, `jti: <uuid>` | Rotate and renew Central Access Tokens |
| **Tenant Refresh Token** | Tenant Refresh | Token Exchange | `user:<id>` | `type: REFRESH`, `tenantId: <id>`, `jti: <uuid>` | Rotate and renew Tenant Access Tokens |

### 5.2 Exact Tenant Access Token Claims Contract

```json
{
  "iss": "https://auth.gymverse.com",
  "sub": "user:10042",
  "uid": 852,
  "tenantId": 12,
  "tokenType": "TENANT",
  "type": "ACCESS",
  "role": "TENANT_ADMIN",
  "adminAuthority": "PRIMARY",
  "iat": 1715000000,
  "exp": 1715000900,
  "jti": "e4b6c3d1-4f18-4b82-9c1a-289d0cf31821"
}
```

#### Detailed Claim Definitions:
- `sub`: Global identity namespace: `"user:<id>"`. Prevents identifier collisions.
- `uid`: The `users.id` primary key in that specific tenant's database. Enables tenant domain services to resolve local profiles without querying central DB.
- `tenantId`: Central database ID (`gym_tenants.id`).
- `tokenType`: Security domain identifier (`TENANT`).
- `role`: Active tenant authorization role (`TENANT_ADMIN`, `TRAINER`, `STAFF`, `MEMBER`).
- `adminAuthority`: Administrative level (`PRIMARY`, `DELEGATED`, or `NONE` for non-admins).
- `exp`: 15 minutes expiration (`900,000 ms`).

### 5.3 Zero-Trust Server-Side Tenant Resolution

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Filter as JwtFilter
    participant Cache as TenantDataSourceCache
    participant Ctx as TenantContext
    participant Controller as Tenant Controller
    participant Service as Domain Service

    Client->>Filter: Request with Authorization: Bearer <tenant_jwt>
    Filter->>Filter: Validate Signature, Expiry & Claims (tokenType == TENANT)
    Note over Filter: Extract tenantId (e.g. 12)
    Filter->>Cache: Resolve dbName for tenantId 12
    Note over Filter,Cache: Cached in-memory map (tenantId -> dbName)<br/>Validated against central DB metadata
    Filter->>Ctx: TenantContext.setCurrentTenant(resolvedDbName)
    Filter->>Filter: Set SecurityContext(sub, authorities=[ROLE_role, AUTH_adminAuthority])
    Filter->>Controller: Chain doFilter(request, response)
    Controller->>Service: Execute Domain Operation
    Note over Service: Queries use Tenant JPA EntityManager<br/>Hibernate routes connection to resolvedDbName
    Service-->>Controller: Return Domain Result
    Controller-->>Filter: Return HTTP Response
    Filter->>Ctx: TenantContext.clear() (in finally block)
    Filter-->>Client: HTTP 200 OK
```

---

## 6. Detailed Workflows & Sequence Diagrams

### 6.1 Central User Registration & OTP Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Any Person
    participant Auth as AuthController
    participant OtpSvc as SendOtpService
    participant Redis as Redis Cache
    participant Kafka as Kafka (otp-email-topic)
    participant Worker as Notification Worker
    participant VerSvc as VerifyOtpService
    participant CompSvc as CompleteSignupService
    participant DB as Central Database (user_accounts)

    User->>Auth: POST [USER_SIGNUP_INITIATE_ENDPOINT] {email}
    Auth->>OtpSvc: Execute signup initiation
    OtpSvc->>DB: Check if email exists
    OtpSvc->>Redis: Check cooldown & attempt limits
    OtpSvc->>Redis: SET signup:otp:<email> (TTL 10m)
    OtpSvc->>Kafka: Publish SendOtpEvent
    OtpSvc-->>User: {message: "OTP dispatched to email"}
    Kafka->>Worker: Consume SendOtpEvent
    Worker->>User: Deliver HTML Email with OTP

    User->>Auth: POST [USER_SIGNUP_VERIFY_ENDPOINT] {email, otp}
    Auth->>VerSvc: Verify OTP
    VerSvc->>Redis: GET signup:otp:<email>
    alt Invalid OTP or Max Attempts Exceeded
        VerSvc-->>User: 400 Bad Request ("Invalid OTP")
    else Valid OTP
        VerSvc->>Redis: DEL signup:otp:<email>
        VerSvc->>Redis: SET signup:verified:<email> = true (TTL 10m)
        VerSvc-->>User: {message: "OTP verified"}
    end

    User->>Auth: POST [USER_SIGNUP_COMPLETE_ENDPOINT] {email, password, firstName, lastName}
    Auth->>CompSvc: Complete signup
    CompSvc->>Redis: GET signup:verified:<email>
    CompSvc->>DB: INSERT INTO user_accounts (BCrypt password)
    CompSvc->>Redis: DEL signup:verified:<email>
    CompSvc-->>User: Issue Central Access & Refresh Tokens
```

---

### 6.2 Gym Registration & Primary Admin Establishment

```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated Central User
    participant Auth as AuthController
    participant RegSvc as RegisterGymService
    participant CentralDB as Central Database
    participant DBMgr as TenantDatabaseService
    participant Flyway as TenantFlywayService
    participant TenantDB as Tenant Database
    participant Jwt as JwtUtil

    User->>Auth: POST [REGISTER_GYM_ENDPOINT] {gymName, address, coordinates, ...}<br/>Bearer <central_user_token>
    Auth->>RegSvc: Register Gym
    RegSvc->>CentralDB: Validate Registrant Central Identity (`user_accounts`)
    RegSvc->>RegSvc: Generate unique gymCode & sanitized dbName
    RegSvc->>DBMgr: createDatabase(dbName)
    DBMgr->>CentralDB: CREATE DATABASE dbName

    alt Provisioning Failure
        Note over RegSvc,DBMgr: If migration or record creation fails,<br/>trigger compensatory rollback: dropDatabase(dbName)
    end

    RegSvc->>Flyway: migrate(dbName)
    Note over Flyway: Executes tenant migrations
    RegSvc->>TenantDB: Switch TenantContext(dbName)
    RegSvc->>TenantDB: INSERT INTO users (central_user_id, email, role='TENANT_ADMIN')
    RegSvc->>TenantDB: INSERT INTO gym_config (coordinates, hours)
    RegSvc->>CentralDB: Clear TenantContext
    RegSvc->>CentralDB: INSERT INTO gym_tenants (gymCode, dbName, status='ACTIVE')
    RegSvc->>CentralDB: INSERT INTO gym_subscriptions (14-day TRIAL plan)
    RegSvc->>CentralDB: INSERT INTO user_tenant_roles (user_id, gym_id, role='TENANT_ADMIN', admin_authority='PRIMARY', status='ACTIVE')
    RegSvc->>Jwt: Generate Tenant Access Token (role='TENANT_ADMIN', authority='PRIMARY')
    RegSvc-->>User: 201 Created {gymCode, tenantAccessToken}
```

---

### 6.3 Delegated Administrator Appointment & Primary Authority Transfer

```mermaid
sequenceDiagram
    autonumber
    actor PrimaryAdmin as Primary Administrator
    actor Successor as Delegated Administrator
    participant AdminAPI as TenantAdminController
    participant GovSvc as AdminGovernanceService
    participant CentralDB as Central Database
    participant TenantDB as Tenant Database

    Note over PrimaryAdmin,CentralDB: SCENARIO A: Appoint Delegated Admin
    PrimaryAdmin->>AdminAPI: POST [APPOINT_ADMIN_ENDPOINT] {targetUserEmail}<br/>Bearer <tenant_token (authority='PRIMARY')>
    AdminAPI->>GovSvc: Appoint Admin
    GovSvc->>CentralDB: Verify Caller has authority='PRIMARY'
    GovSvc->>CentralDB: Find target in user_accounts
    GovSvc->>CentralDB: INSERT/UPDATE user_tenant_roles (role='TENANT_ADMIN', admin_authority='DELEGATED', status='ACTIVE')
    GovSvc->>TenantDB: INSERT/UPDATE users (central_user_id, role='TENANT_ADMIN')
    GovSvc->>CentralDB: INSERT INTO admin_audit_logs (action='APPOINT_DELEGATED_ADMIN')
    GovSvc-->>PrimaryAdmin: 200 OK ("Delegated admin appointed")

    Note over PrimaryAdmin,Successor: SCENARIO B: Transfer Primary Authority
    PrimaryAdmin->>AdminAPI: POST [TRANSFER_PRIMARY_ADMIN_ENDPOINT] {successorUserAccountId}<br/>Bearer <tenant_token (authority='PRIMARY')>
    AdminAPI->>GovSvc: Initiate Transfer
    GovSvc->>CentralDB: Verify Caller authority='PRIMARY' and Successor authority='DELEGATED'
    Note over GovSvc,CentralDB: Atomic Transaction on Central DB:
    GovSvc->>CentralDB: UPDATE user_tenant_roles SET admin_authority='DELEGATED' WHERE user_id=PrimaryAdmin
    GovSvc->>CentralDB: UPDATE user_tenant_roles SET admin_authority='PRIMARY' WHERE user_id=Successor
    GovSvc->>CentralDB: INSERT INTO admin_audit_logs (action='TRANSFER_PRIMARY_AUTHORITY')
    GovSvc-->>PrimaryAdmin: 200 OK ("Primary administration successfully transferred")
```

---

### 6.4 Gym Selection & Token Exchange Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Client App
    participant Auth as AuthController
    participant ExchangeSvc as TokenExchangeService
    participant CentralDB as Central Database
    participant TenantDB as Tenant Database
    participant Jwt as JwtUtil

    User->>Auth: POST [EXCHANGE_TOKEN_ENDPOINT] {targetGymId}<br/>Header: Bearer <central_user_token>
    Auth->>ExchangeSvc: Process Token Exchange
    ExchangeSvc->>CentralDB: Validate Central Identity in user_accounts
    ExchangeSvc->>CentralDB: Query user_tenant_roles for (user_account_id, targetGymId)
    alt No Relationship or Inactive Status
        ExchangeSvc-->>User: 403 Forbidden ("Not authorized for this gym")
    end
    ExchangeSvc->>CentralDB: Query Gym Tenant & Status
    alt Gym Suspended or Inactive
        ExchangeSvc-->>User: 403 Forbidden ("Gym is currently suspended")
    end
    ExchangeSvc->>CentralDB: Query Active SaaS Subscription in gym_subscriptions
    alt Subscription Expired
        ExchangeSvc-->>User: 402 Payment Required ("Gym SaaS subscription expired")
    end
    ExchangeSvc->>TenantDB: Lookup Tenant User Profile (`users.id`) via central_user_id
    ExchangeSvc->>Jwt: Generate Tenant Access Token (sub, uid, tenantId, role, adminAuthority)
    ExchangeSvc->>Jwt: Generate Tenant Refresh Token
    ExchangeSvc-->>User: Return {tenantAccessToken, tenantRefreshToken}
```

---

## 7. Role & Permission Matrix

| Role | Admin Authority | Permitted Operations | Restricted Operations |
| :--- | :--- | :--- | :--- |
| **PLATFORM_ADMIN** | N/A (Platform Scope) | Manage global SaaS plans, inspect all gym metadata, suspend/restore gyms, system health | Operational gym attendance, member records |
| **TENANT_ADMIN** | **PRIMARY** | Manage gym SaaS billing, appoint/revoke delegated admins, transfer primary authority, all operational duties | Modify other gyms' data, access platform admin APIs |
| **TENANT_ADMIN** | **DELEGATED** | Review join requests, invite members/staff/trainers, create membership plans, record revenue/expenses, configure geofencing | Appoint/revoke admins, transfer primary authority, alter SaaS billing |
| **TRAINER** | NONE | View trainer dashboard, view client streaks & attendance, record workout coaching notes | Manage plans, review join requests, access financial records |
| **STAFF** | NONE | Front-desk check-in overrides, browse member list, assist plan renewals | Create/delete plans, record facility expenses, change geofencing |
| **MEMBER** | NONE | GPS Check-in/out, browse catalog, submit plan request, search peers, friend requests, streaks | Any admin or staff endpoint, view other members' financial data |

---

## 8. Cross-Database Transaction & Compensation Protocol

Because the central database and tenant databases are physically distinct MySQL instances, a standard Spring `@Transactional` cannot provide two-phase commit (2PC) guarantees across them.

### 8.1 Dual-Write Compensation Strategy
For operations that modify both Central and Tenant databases (e.g. Gym Registration, Admin Appointment, Join Request Approval):

1. **Local-First Ordering:**
   - Execute the non-reversible or physical infrastructure step first (e.g., MySQL database creation).
   - If subsequent steps fail, trigger an explicit compensatory action:
     ```java
     try {
         tenantDatabaseService.createDatabase(dbName);
         tenantFlywayService.migrate(dbName);
         seedTenantConfigAndUser(dbName, ...);
         saveCentralTenantAndRole(...);
     } catch (Exception ex) {
         log.error("Provisioning failed. Initiating compensatory rollback for {}", dbName, ex);
         tenantDatabaseService.dropDatabase(dbName);
         throw new GymProvisioningException("Provisioning failed and was rolled back", ex);
     }
     ```
2. **Idempotency Guards:**
   - All cross-database synchronization operations must be idempotent.
   - If an administrative record exists in the central database but the tenant user record is missing, the reconciliation service creates the missing record on first token exchange without failing.
3. **Outbox / Async Event Replay:**
   - Asynchronous side effects (sending OTP emails, invitation notifications) are published through Kafka after the primary transaction commits. Consumers use idempotent message handlers keyed on event UUIDs.

---

## 9. Architectural Decision Records (ADRs)

### ADR-1: Unified Central Identity Model (`user_accounts`)
- **Status:** Approved
- **Context:** The legacy separation of `accounts` (owners/admins) and `user_accounts` (members/trainers) created artificial barriers, prevented gym owners from being members elsewhere, and led to duplicate identity schemas.
- **Decision:** Consolidate all humans into a single central identity table: `user_accounts`.
- **Consequences:** Simplifies authentication controllers and JWT generation. Enables true multi-gym participation across any role under a single login.

### ADR-2: Elimination of the Owner Role in Favor of Primary Tenant Administration
- **Status:** Approved
- **Context:** An `OWNER` authorization role conflated business equity with operational permissions and made delegating gym administration awkward.
- **Decision:** Eliminate the `OWNER` role completely. Represent facility governance via `TENANT_ADMIN`, distinguished by an explicit authority flag (`admin_authority = PRIMARY | DELEGATED`).
- **Consequences:** Clear governance boundaries. Supports multi-admin gyms naturally while guaranteeing exactly one active Primary Administrator responsible for billing and administrative governance.

### ADR-3: Single Authoritative Credential Store
- **Status:** Approved
- **Context:** Legacy designs stored password hashes in both central and tenant databases.
- **Decision:** Eliminate password columns from tenant databases. All authentication occurs centrally against `user_accounts`. Tenant `users` tables store only profile attributes and `central_user_id`.
- **Consequences:** Eliminates credential desynchronization. Centralizes password rotation, reset, and account lockout.

### ADR-4: Zero-Trust Server-Side Tenant Resolution
- **Status:** Approved
- **Context:** Earlier prototypes accepted `dbName` directly from client JWT claims, trusting client tokens for database routing.
- **Decision:** Remove `dbName` from tokens. Tokens carry only `tenantId`. The server resolves `dbName` via a cached central lookup and enforces active subscription status on every request.
- **Consequences:** Internal database topology remains completely concealed from clients. Facilitates instant tenant suspension and migration.

### ADR-5: Two-Tier Token Exchange Architecture
- **Status:** Approved
- **Context:** Single global tokens grant excessive blast radius and cannot carry tenant-specific role context safely.
- **Decision:** Central authentication returns a Central Access Token. Accessing tenant APIs requires calling `EXCHANGE_TOKEN_ENDPOINT` to acquire a Tenant Access Token scoped strictly to the selected gym.
- **Consequences:** Eliminates privilege bleed between gyms. Allows users to switch gym contexts seamlessly without re-authenticating centrally.

---

## 10. Clean-Slate Schema Design for GymVerse-Backend-v2

Because `GymVerse-Backend-v2` is scaffolded as a clean foundation, the database migrations can be initialized directly with clean target schemas rather than incrementally patching legacy columns:

1. **Central Schema (`V1__init_central_schema.sql`):**
   - `user_accounts` (unified central identity, BCrypt passwords, verification flags).
   - `gym_tenants` (gym code, database name, status).
   - `user_tenant_roles` (relationships connecting central identity to gym tenants with roles and `admin_authority`).
   - `saas_subscription_plans` (platform tiers and billing cycles).
   - `gym_subscriptions` (gym tenant subscription records).
   - `admin_audit_logs` (audit history for admin appointments and transfers).

2. **Tenant Schema (`V1__init_tenant_schema.sql`):**
   - `users` (tenant-local profile with `central_user_id`, no password column).
   - `gym_config` (geofence latitude/longitude/radius, hours).
   - `members` (member profile linked to `users.id`).
   - `membership_plans` (catalog of plans offered by this gym).
   - `member_memberships` (purchased plans).
   - `membership_requests` (plan purchase requests).
   - `attendance` (geofenced check-in/out records).
   - `revenue` & `expenses` (financial records).
   - `join_requests` (membership applications from central users).
   - `gym_invitations` (invitations issued to trainers, staff, or members).
   - `friend_requests`, `custom_friends`, `friend_streaks` (social features).

---

## 11. Ordered Implementation Phases

```mermaid
graph LR
    P1["Phase 1: Unified Central Schema & Identity Entities"] --> P2["Phase 2: Unified Central Auth & JWT Engine"]
    P2 --> P3["Phase 3: Gym Registration & Primary Admin Establishment"]
    P3 --> P4["Phase 4: Multi-Admin Governance & Token Exchange"]
    P4 --> P5["Phase 5: Member Onboarding & Security Hardening"]
    P5 --> P6["Phase 6: Tenant Operations & Social Feature Migration"]
```

- **Phase 1: Unified Central Schema & Identity Entities**
  - Implement `V1__init_central_schema.sql` in `db/migration/central`.
  - Create JPA entities and repositories in `com.company.GymVerse.models.central` and `com.company.GymVerse.repositories.central`.
- **Phase 2: Unified Central Auth & JWT Engine**
  - Implement unified central authentication service (signup initiation, OTP verification, signup completion, login, refresh).
  - Implement namespaced JWT generation (`CENTRAL_USER` token type).
  - Configure Redis-backed OTP caching and rate limiting.
- **Phase 3: Gym Registration & Primary Admin Establishment**
  - Implement gym registration workflow with automated database creation and Flyway migration.
  - Establish registrant as Primary Tenant Administrator in `user_tenant_roles`.
  - Provision default 14-day trial subscription in `gym_subscriptions`.
- **Phase 4: Multi-Admin Governance & Token Exchange**
  - Implement Delegated Admin appointment and authority transfer services.
  - Implement `EXCHANGE_TOKEN_ENDPOINT` with subscription and role verification to issue `TENANT` tokens.
- **Phase 5: Member Onboarding & Security Hardening**
  - Implement `V1__init_tenant_schema.sql` in `db/migration/tenant`.
  - Create tenant entities and repositories in `models.tenant` and `repositories.tenant`.
  - Implement zero-trust `JwtFilter` with server-side database name resolution and route protection.
- **Phase 6: Tenant Operations & Social Feature Migration**
  - Implement member check-in/out with `GeoUtil` geofencing.
  - Implement friendship, streaks, and trainer workflows.
  - Build automated integration test suite validating end-to-end flows.
