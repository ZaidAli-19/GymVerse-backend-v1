---
id: "story-1.2"
epic_id: "epic-1"
title: "Central Database Flyway Migration (V1__init_central_schema.sql)"
status: "ready-for-dev"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies:
  - "story-1.1"
files_to_modify: []
files_to_create:
  - "src/main/resources/db/migration/central/V1__init_central_schema.sql"
---

# Story 1.2: Central Database Flyway Migration (`V1__init_central_schema.sql`)

## User Story

**As a** database architect,  
**I want** an authoritative, production-grade Flyway migration script for the central platform database (`GymVerse`),  
**So that** all shared platform tables, foreign keys, indexes, unique constraints, and enterprise monetary column types are deterministically provisioned, validated, and versioned on application startup.

---

## Business & Technical Context

In the GymVerse architecture ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md)), the central database (`GymVerse`) serves as the single source of truth for global identity, tenant provisioning, gym authorization, SaaS subscriptions, and administrative audit trails.

### Core Architectural Invariants
1. **Unified Identity Model**: Central identity is established exclusively within `user_accounts`. There is no separate owner table, super-admin identity, or external credentials table.
2. **Strict Financial Precision**: All monetary values (`saas_subscription_plans.price` and `gym_subscriptions.price_paid`) must use SQL `DECIMAL(10,2) NOT NULL` rather than floating-point representations (`DOUBLE`/`FLOAT`) to guarantee zero rounding errors.
3. **Database Naming & Tenant Isolation**: Physical tenant database names (`gym_tenants.db_name`) are strictly governed and stored in the central schema (`uq_gt_db_name`). Client applications discover and reference gyms via `gym_code` (`uq_gt_gym_code`), never by providing raw physical database names.
4. **Tenant Role Registry**: User memberships and authority in gyms are tracked in `user_tenant_roles`. To maintain the single-active primary administrator rule and fast security checks, a composite unique constraint `uq_utr_user_gym_role (user_account_id, gym_tenant_id, role)` and indexes `idx_utr_user_status` and `idx_utr_gym_role` are required.
5. **Flyway Execution Model**: Flyway auto-migration on application startup is configured to execute strictly against `classpath:db/migration/central` on the central datasource.

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - Story 1.1 completed (`application.yaml` and `application-test.yaml` configured with `spring.flyway.locations: classpath:db/migration/central`).
  - Active MySQL 8.0+ instance accessible on `localhost:3306` with database `GymVerse` (or test equivalent).
- **Dependencies**:
  - Depends on Story 1.1.
  - Story 1.3 (Central JPA Entities & Repositories) depends directly on this migration script.

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. Creation of `src/main/resources/db/migration/central/V1__init_central_schema.sql`.
2. Definition of all 6 central platform tables:
   - `user_accounts`
   - `gym_tenants`
   - `user_tenant_roles`
   - `saas_subscription_plans`
   - `gym_subscriptions`
   - `admin_audit_logs`
3. Enforcement of primary keys, foreign keys with referential integrity (`ON DELETE RESTRICT` or `ON DELETE CASCADE` where appropriate), unique indexes, performance lookup indexes, and column defaults.
4. Enforcement of `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` across all created tables.
5. Validation that Flyway automatically detects and applies `V1__init_central_schema.sql` during Spring Boot context startup, recording a successful entry in `flyway_schema_history`.

### Out of Scope
- Writing tenant database migration scripts (Story 1.4: `V1__init_tenant_schema.sql`).
- Implementing Java JPA entities, enums, or repositories (Story 1.3).
- Seed data or business fixtures (only baseline schema DDL is in scope).
- Modifying any of the 8 protected multitenancy classes in `com.company.GymVerse.multitanancy`.

---

## Detailed Acceptance Criteria

### AC-1.2.1: Table Structure for `user_accounts`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `user_accounts` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `email VARCHAR(255) NOT NULL UNIQUE KEY uq_ua_email`
  - `first_name VARCHAR(100) NOT NULL`
  - `last_name VARCHAR(100) NOT NULL`
  - `password VARCHAR(255) NOT NULL` (stores BCrypt hash)
  - `phone_number VARCHAR(30) NULL`
  - `email_verified BOOLEAN NOT NULL DEFAULT FALSE`
  - `is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE`
  - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (values: PENDING, ACTIVE, SUSPENDED, DEACTIVATED)
  - `created_at DATETIME(6) NOT NULL`
  - `updated_at DATETIME(6) NOT NULL`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.2: Table Structure for `gym_tenants`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `gym_tenants` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `gym_name VARCHAR(200) NOT NULL`
  - `contact_email VARCHAR(255) NOT NULL`
  - `phone_number VARCHAR(30) NULL`
  - `address VARCHAR(255) NOT NULL`
  - `city VARCHAR(100) NOT NULL`
  - `logo_url VARCHAR(500) NULL`
  - `gym_code VARCHAR(50) NOT NULL UNIQUE KEY uq_gt_gym_code`
  - `db_name VARCHAR(100) NOT NULL UNIQUE KEY uq_gt_db_name`
  - `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'` (values: ACTIVE, SUSPENDED, INACTIVE)
  - `registered_at DATETIME(6) NOT NULL`
  - `suspended_at DATETIME(6) NULL`
  - `suspension_reason TEXT NULL`
  - `INDEX idx_gt_gym_code (gym_code)`
  - `INDEX idx_gt_db_name (db_name)`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.3: Table Structure for `user_tenant_roles`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `user_tenant_roles` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `user_account_id BIGINT NOT NULL`
  - `gym_tenant_id BIGINT NOT NULL`
  - `role VARCHAR(30) NOT NULL` (values: TENANT_ADMIN, MEMBER, TRAINER, STAFF)
  - `admin_authority VARCHAR(30) NOT NULL DEFAULT 'NONE'` (values: PRIMARY, DELEGATED, NONE)
  - `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'` (values: ACTIVE, PENDING, SUSPENDED, REVOKED)
  - `joined_at DATETIME(6) NOT NULL`
  - `updated_at DATETIME(6) NOT NULL`
  - `CONSTRAINT fk_utr_user_account FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE`
  - `CONSTRAINT fk_utr_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE`
  - `UNIQUE KEY uq_utr_user_gym_role (user_account_id, gym_tenant_id, role)`
  - `INDEX idx_utr_user_status (user_account_id, status)`
  - `INDEX idx_utr_gym_role (gym_tenant_id, role, admin_authority)`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.4: Table Structure for `saas_subscription_plans`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `saas_subscription_plans` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `name VARCHAR(100) NOT NULL`
  - `description TEXT NULL`
  - `price DECIMAL(10,2) NOT NULL` (Strict monetary precision)
  - `billing_cycle VARCHAR(30) NOT NULL` (values: MONTHLY, QUARTERLY, ANNUAL)
  - `max_members INT NOT NULL`
  - `max_trainers INT NOT NULL`
  - `analytics_enabled BOOLEAN NOT NULL DEFAULT FALSE`
  - `multi_trainer_enabled BOOLEAN NOT NULL DEFAULT FALSE`
  - `custom_branding_enabled BOOLEAN NOT NULL DEFAULT FALSE`
  - `active BOOLEAN NOT NULL DEFAULT TRUE`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.5: Table Structure for `gym_subscriptions`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `gym_subscriptions` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `gym_tenant_id BIGINT NOT NULL`
  - `plan_id BIGINT NOT NULL`
  - `price_paid DECIMAL(10,2) NOT NULL` (Strict monetary precision)
  - `billing_cycle VARCHAR(30) NOT NULL`
  - `start_date DATE NOT NULL`
  - `end_date DATE NOT NULL`
  - `status VARCHAR(30) NOT NULL DEFAULT 'TRIAL'` (values: TRIAL, ACTIVE, EXPIRED, CANCELLED)
  - `subscribed_at DATETIME(6) NOT NULL`
  - `transaction_reference VARCHAR(100) NULL`
  - `CONSTRAINT fk_gs_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE`
  - `CONSTRAINT fk_gs_plan FOREIGN KEY (plan_id) REFERENCES saas_subscription_plans (id) ON DELETE RESTRICT`
  - `INDEX idx_gs_tenant_status (gym_tenant_id, status)`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.6: Table Structure for `admin_audit_logs`
- **Given** execution of `V1__init_central_schema.sql`,
- **When** the migration runs against the central schema,
- **Then** `admin_audit_logs` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `gym_tenant_id BIGINT NOT NULL`
  - `actor_user_account_id BIGINT NOT NULL`
  - `target_user_account_id BIGINT NULL`
  - `action VARCHAR(50) NOT NULL` (values: APPOINT_DELEGATED_ADMIN, REVOKE_DELEGATED_ADMIN, TRANSFER_PRIMARY_AUTHORITY)
  - `metadata TEXT NULL`
  - `timestamp DATETIME(6) NOT NULL`
  - `CONSTRAINT fk_aal_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE`
  - `CONSTRAINT fk_aal_actor_user FOREIGN KEY (actor_user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE`
  - `CONSTRAINT fk_aal_target_user FOREIGN KEY (target_user_account_id) REFERENCES user_accounts (id) ON DELETE SET NULL`
  - `INDEX idx_aal_tenant_timestamp (gym_tenant_id, timestamp)`
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.

### AC-1.2.7: Flyway Recording & Idempotence
- **Given** an initialized MySQL database,
- **When** Flyway applies `V1__init_central_schema.sql`,
- **Then** `flyway_schema_history` records a row with `version = '1'`, `type = 'SQL'`, `script = 'V1__init_central_schema.sql'`, and `success = 1`,
- **And** subsequent application boots execute without errors or schema conflicts.

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/resources/db/migration/central/V1__init_central_schema.sql` | Create | Complete DDL script creating all 6 central tables, constraints, foreign keys, indexes, and monetary precision types. |

---

## Technical Implementation Guidance

### DDL Implementation Guidelines
1. **Idempotence & Strict Creation**: Flyway enforces execution of migration files once per database. Tables should use standard `CREATE TABLE` statements (Flyway manages rollback / version tracking).
2. **Foreign Key Constraints**:
   - `user_tenant_roles.user_account_id` -> `user_accounts(id)` ON DELETE CASCADE
   - `user_tenant_roles.gym_tenant_id` -> `gym_tenants(id)` ON DELETE CASCADE
   - `gym_subscriptions.gym_tenant_id` -> `gym_tenants(id)` ON DELETE CASCADE
   - `gym_subscriptions.plan_id` -> `saas_subscription_plans(id)` ON DELETE RESTRICT
   - `admin_audit_logs.gym_tenant_id` -> `gym_tenants(id)` ON DELETE CASCADE
   - `admin_audit_logs.actor_user_account_id` -> `user_accounts(id)` ON DELETE CASCADE
   - `admin_audit_logs.target_user_account_id` -> `user_accounts(id)` ON DELETE SET NULL
3. **Monetary Precision Standard**:
   - `saas_subscription_plans.price DECIMAL(10,2) NOT NULL`
   - `gym_subscriptions.price_paid DECIMAL(10,2) NOT NULL`
4. **Naming Convention**:
   - Snake_case for table and column names to ensure seamless compatibility with `CamelCaseToUnderscoresNamingStrategy` in `CentralJpaConfig`.

---

## Security & Multitenancy Considerations

1. **No Tenant Leakage**: The central database strictly houses tenant *metadata* (`gym_tenants`), never operational tenant records (e.g. attendance, member profiles, workout logs).
2. **Zero Password Storage for Tenants**: Central `user_accounts` is the sole table permitted to store password hashes.
3. **Unique Keys Prevent Duplicate Provisioning**:
   - `uq_gt_gym_code` guarantees globally unique client routing keys.
   - `uq_gt_db_name` guarantees globally unique MySQL database schema bindings.

---

## Verification & Testing Plan

### Automated Verification
```powershell
# Verify syntax and packaging
./mvnw clean test-compile

# Once Story 1.7 integration tests exist:
./mvnw test -Dtest=CentralPersistenceIntegrationTest -Dspring.profiles.active=test
```

### Manual Verification
Inspect the MySQL central schema directly:
```sql
SHOW TABLES FROM GymVerse;
DESCRIBE GymVerse.user_accounts;
DESCRIBE GymVerse.gym_tenants;
DESCRIBE GymVerse.user_tenant_roles;
DESCRIBE GymVerse.saas_subscription_plans;
DESCRIBE GymVerse.gym_subscriptions;
DESCRIBE GymVerse.admin_audit_logs;
SELECT version, description, success FROM GymVerse.flyway_schema_history;
```

---

## Definition of Done (DoD)

- [ ] `V1__init_central_schema.sql` is placed in `src/main/resources/db/migration/central/`.
- [ ] All 6 central tables are defined with exact column types, lengths, nullability, defaults, and constraints specified in AC-1.2.1 through AC-1.2.6.
- [ ] Monetary columns are defined as `DECIMAL(10,2) NOT NULL`.
- [ ] `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` is applied to every table.
- [ ] Flyway successfully applies the migration and records `success = 1` in `flyway_schema_history`.
- [ ] No modifications to the 8 protected multitenancy classes.
