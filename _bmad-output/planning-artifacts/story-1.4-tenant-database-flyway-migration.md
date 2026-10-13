---
id: "story-1.4"
epic_id: "epic-1"
title: "Tenant Database Flyway Migration (V1__init_tenant_schema.sql)"
status: "implemented"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies:
  - "story-1.1"
files_to_modify: []
files_to_create:
  - "src/main/resources/db/migration/tenant/V1__init_tenant_schema.sql"
---

# Story 1.4: Tenant Database Flyway Migration (`V1__init_tenant_schema.sql`)

## User Story

**As a** database architect,  
**I want** an authoritative, production-grade Flyway migration script for tenant-specific operational databases (`gym_db_<id>`),  
**So that** dynamically provisioned gym schemas contain a standardized set of operational tables, foreign keys, indexes, monetary precision types, and security safeguards—including zero stored passwords and hashed invitation tokens—executable programmatically via `TenantFlywayService`.

---

## Business & Technical Context

In the GymVerse database-per-tenant architecture ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Section 4.2), each gym operates inside a physically separated MySQL database (`gym_db_<id>`). When a gym is provisioned or tested:
1. `TenantDatabaseService.createDatabase(dbName)` creates the raw database.
2. `TenantFlywayService.migrate(dbName)` executes migrations located at `classpath:db/migration/tenant`.
3. The migration initializes the local schema and tracks its state in `flyway_schema_history` within that tenant database.

### Core Architectural Invariants
1. **Zero Passwords in Tenant Schema**: Tenant `users` stores operational profile information (`central_user_id`, `email`, `first_name`, `last_name`, `phone_number`, `role`, `active`, `created_at`). It **must never contain a password or password hash column**. All authentication credentials reside exclusively in the central `user_accounts` table.
2. **Scalar Cross-Database References**: References to central users (such as `users.central_user_id` and `join_requests.central_user_id`) are scalar `BIGINT NOT NULL` columns. They **must not have physical foreign keys** targeting the central database because MySQL cannot guarantee cross-database referential integrity across dynamic schemas.
3. **Local Referential Integrity**: All foreign keys (e.g., `members.user_id -> users(id)`, `attendance.member_id -> members(id)`) exist strictly within the local tenant database.
4. **Hashed Invitation Token Storage**: To eliminate bearer token compromise via database dumps or unauthorized inspection, `gym_invitations` stores tokens as SHA-256 cryptographic hashes: `invitation_token_hash VARCHAR(64) NOT NULL UNIQUE`.
5. **Strict Monetary Precision**: All financial columns (`membership_plans.base_price`, `membership_plans.final_price`, `member_memberships.price_at_purchase`, `member_memberships.final_price`, `revenue.amount`, `expenses.amount`) must use SQL `DECIMAL(10,2) NOT NULL`. Optional discounts use `DECIMAL(10,2) NULL`.

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - Story 1.1 completed (properties configured).
  - Programmatic migration engine [`TenantFlywayService.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/multitanancy/TenantFlywayService.java) intact.
- **Dependencies**:
  - Depends on Story 1.1.
  - Story 1.5 (Tenant JPA Entities & Repositories) and Story 1.7 (Tenant Persistence & Isolation Integration Tests) depend on this migration.

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. Creation of `src/main/resources/db/migration/tenant/V1__init_tenant_schema.sql`.
2. Definition of all 14 tenant operational tables:
   - `users` (tenant operational profile with scalar `central_user_id`, zero passwords)
   - `gym_config` (geofence center, radius, business hours, timezone)
   - `members` (member profile linked 1:1 with tenant user, fitness metrics)
   - `membership_plans` (tier, facilities, duration, `DECIMAL(10,2)` pricing)
   - `member_memberships` (active and historical subscriptions, `DECIMAL(10,2)` purchase prices)
   - `membership_requests` (plan request workflow)
   - `attendance` (daily attendance, GPS check-in/out timestamps, auto-checkout flag)
   - `revenue` (financial intake linked to memberships, `DECIMAL(10,2)` amounts)
   - `expenses` (gym expenditure records, `DECIMAL(10,2)` amounts)
   - `join_requests` (membership applicant intake from mobile/web before onboarding)
   - `gym_invitations` (staff/trainer/member invitations with `invitation_token_hash VARCHAR(64) NOT NULL UNIQUE`)
   - `friend_requests` (member social connections)
   - `custom_friends` (confirmed friend pairs)
   - `friend_streaks` (attendance streaks and shared workout counters)
3. Application of `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` across all 14 tables.
4. Execution verification via `TenantFlywayService.migrate(dbName)`.

### Out of Scope
- Central database migrations (Story 1.2).
- Implementing tenant Java JPA entities or repositories (Story 1.5).
- Modifying `TenantFlywayService.java`, `TenantDatabaseService.java`, or any other protected multitenancy class.

---

## Detailed Acceptance Criteria

### AC-1.4.1: Table Structure for `users` (Zero-Password Invariant)
- **Given** execution of `V1__init_tenant_schema.sql` by `TenantFlywayService`,
- **When** the migration runs against a tenant database,
- **Then** `users` is created with:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `central_user_id BIGINT NOT NULL UNIQUE KEY uq_u_central_user_id` (scalar link, no physical cross-DB FK)
  - `first_name VARCHAR(100) NOT NULL`
  - `last_name VARCHAR(100) NOT NULL`
  - `email VARCHAR(255) NOT NULL UNIQUE KEY uq_u_email`
  - `phone_number VARCHAR(30) NULL`
  - `role VARCHAR(30) NOT NULL` (values: TENANT_ADMIN, MEMBER, TRAINER, STAFF)
  - `active BOOLEAN NOT NULL DEFAULT TRUE`
  - `created_at DATETIME(6) NOT NULL`
  - **No password or hash column is created under any circumstances**.

### AC-1.4.2: Table Structure for `gym_config`
- **Given** table `gym_config`,
- **When** created,
- **Then** columns include:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `latitude DECIMAL(10,8) NOT NULL`
  - `longitude DECIMAL(11,8) NOT NULL`
  - `radius DOUBLE NOT NULL` (radius in meters for geofence validation)
  - `time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC'`
  - `opening_time TIME NOT NULL`
  - `closing_time TIME NOT NULL`
  - `auto_checkout_time TIME NULL`.

### AC-1.4.3: Table Structure for `members`
- **Given** table `members`,
- **When** created,
- **Then** columns and constraints include:
  - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
  - `user_id BIGINT NOT NULL UNIQUE KEY uq_m_user_id`
  - `trainer_id BIGINT NULL`
  - `independent BOOLEAN NOT NULL DEFAULT FALSE`
  - `height DECIMAL(5,2) NULL`
  - `weight DECIMAL(5,2) NULL`
  - `goal VARCHAR(255) NULL`
  - `join_date DATE NOT NULL`
  - `gym_id VARCHAR(50) NULL UNIQUE KEY uq_m_gym_id` (external badge identifier)
  - `type VARCHAR(50) NULL`
  - `trial_end_date DATE NULL`
  - `specialization VARCHAR(100) NULL`
  - `CONSTRAINT fk_m_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE`
  - `CONSTRAINT fk_m_trainer FOREIGN KEY (trainer_id) REFERENCES users (id) ON DELETE SET NULL`.

### AC-1.4.4: Table Structure for `membership_plans` & `member_memberships`
- **Given** tables `membership_plans` and `member_memberships`,
- **When** created,
- **Then**:
  - `membership_plans`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `name VARCHAR(100) NOT NULL`, `description TEXT NULL`, `tier VARCHAR(50) NULL`, `facility VARCHAR(100) NULL`
    - `base_price DECIMAL(10,2) NOT NULL`
    - `discount_type VARCHAR(30) NULL`
    - `discount_value DECIMAL(10,2) NULL`
    - `final_price DECIMAL(10,2) NOT NULL`
    - `duration_days INT NOT NULL`
    - `active BOOLEAN NOT NULL DEFAULT TRUE`
  - `member_memberships`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `member_id BIGINT NOT NULL`, `plan_id BIGINT NOT NULL`
    - `price_at_purchase DECIMAL(10,2) NOT NULL`
    - `discount_type VARCHAR(30) NULL`
    - `discount_value DECIMAL(10,2) NULL`
    - `final_price DECIMAL(10,2) NOT NULL`
    - `start_date DATE NOT NULL`, `end_date DATE NOT NULL`
    - `active BOOLEAN NOT NULL DEFAULT TRUE`
    - `CONSTRAINT fk_mm_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_mm_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE RESTRICT`
    - `INDEX idx_mm_member_active (member_id, active)`.

### AC-1.4.5: Table Structure for `membership_requests` & `attendance`
- **Given** tables `membership_requests` and `attendance`,
- **When** created,
- **Then**:
  - `membership_requests`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `user_id BIGINT NOT NULL`, `plan_id BIGINT NOT NULL`
    - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (PENDING, APPROVED, REJECTED)
    - `request_date DATETIME(6) NOT NULL`, `member_notes TEXT NULL`, `admin_notes TEXT NULL`, `reviewed_at DATETIME(6) NULL`
    - `reviewed_by_admin_id BIGINT NULL`
    - `CONSTRAINT fk_mr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_mr_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_mr_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users (id) ON DELETE SET NULL`
  - `attendance`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `member_id BIGINT NOT NULL`, `date DATE NOT NULL`
    - `check_in_time DATETIME(6) NOT NULL`, `check_out_time DATETIME(6) NULL`
    - `auto_checked_out BOOLEAN NOT NULL DEFAULT FALSE`
    - `status VARCHAR(30) NOT NULL` (PRESENT, CHECKED_OUT, AUTO_CHECKED_OUT)
    - `CONSTRAINT fk_att_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `INDEX idx_att_member_date (member_id, date)`.

### AC-1.4.6: Table Structure for `revenue` & `expenses`
- **Given** tables `revenue` and `expenses`,
- **When** created,
- **Then**:
  - `revenue`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `member_membership_id BIGINT NOT NULL UNIQUE KEY uq_rev_membership`
    - `amount DECIMAL(10,2) NOT NULL`
    - `payment_mode VARCHAR(50) NOT NULL`
    - `transaction_reference VARCHAR(100) NULL`
    - `received_at DATETIME(6) NOT NULL`
    - `recorded_by_admin_id BIGINT NULL`
    - `CONSTRAINT fk_rev_membership FOREIGN KEY (member_membership_id) REFERENCES member_memberships (id) ON DELETE RESTRICT`
    - `CONSTRAINT fk_rev_admin FOREIGN KEY (recorded_by_admin_id) REFERENCES users (id) ON DELETE SET NULL`
  - `expenses`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `title VARCHAR(200) NOT NULL`, `description TEXT NULL`
    - `amount DECIMAL(10,2) NOT NULL`
    - `category VARCHAR(50) NOT NULL`
    - `expense_date DATE NOT NULL`
    - `recorded_by_admin_id BIGINT NULL`
    - `CONSTRAINT fk_exp_admin FOREIGN KEY (recorded_by_admin_id) REFERENCES users (id) ON DELETE SET NULL`
    - `INDEX idx_exp_date (expense_date)`.

### AC-1.4.7: Table Structure for `join_requests` & `gym_invitations` (Hashed Token Invariant)
- **Given** tables `join_requests` and `gym_invitations`,
- **When** created,
- **Then**:
  - `join_requests`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `central_user_id BIGINT NOT NULL` (scalar central reference)
    - `email VARCHAR(255) NOT NULL`, `first_name VARCHAR(100) NULL`, `last_name VARCHAR(100) NULL`, `phone_number VARCHAR(30) NULL`
    - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (PENDING, APPROVED, REJECTED)
    - `request_notes TEXT NULL`, `admin_notes TEXT NULL`
    - `created_at DATETIME(6) NOT NULL`, `reviewed_at DATETIME(6) NULL`
    - `reviewed_by_admin_id BIGINT NULL`
    - `CONSTRAINT fk_jr_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users (id) ON DELETE SET NULL`
    - `INDEX idx_jr_status (status)`
  - `gym_invitations`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `email VARCHAR(255) NOT NULL`
    - `role VARCHAR(30) NOT NULL` (TENANT_ADMIN, MEMBER, TRAINER, STAFF)
    - **`invitation_token_hash VARCHAR(64) NOT NULL UNIQUE KEY uq_gi_token_hash`** (Stores SHA-256 digest)
    - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (PENDING, ACCEPTED, EXPIRED, REVOKED)
    - `expires_at DATETIME(6) NOT NULL`
    - `created_at DATETIME(6) NOT NULL`
    - `accepted_at DATETIME(6) NULL`
    - `created_by_admin_id BIGINT NULL`
    - `CONSTRAINT fk_gi_admin FOREIGN KEY (created_by_admin_id) REFERENCES users (id) ON DELETE SET NULL`
    - `INDEX idx_gi_email_status (email, status)`.

### AC-1.4.8: Table Structure for Social Features (`friend_requests`, `custom_friends`, `friend_streaks`)
- **Given** tables `friend_requests`, `custom_friends`, and `friend_streaks`,
- **When** created,
- **Then**:
  - `friend_requests`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `sender_member_id BIGINT NOT NULL`, `receiver_member_id BIGINT NOT NULL`
    - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (PENDING, ACCEPTED, REJECTED, REMOVED)
    - `CONSTRAINT fk_fr_sender FOREIGN KEY (sender_member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_fr_receiver FOREIGN KEY (receiver_member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `UNIQUE KEY uq_fr_sender_receiver (sender_member_id, receiver_member_id)`
  - `custom_friends`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `user_member_id BIGINT NOT NULL`, `friend_member_id BIGINT NOT NULL`
    - `custom_name VARCHAR(100) NULL`
    - `created_at DATETIME(6) NOT NULL`
    - `CONSTRAINT fk_cf_user FOREIGN KEY (user_member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_cf_friend FOREIGN KEY (friend_member_id) REFERENCES members (id) ON DELETE CASCADE`
    - `UNIQUE KEY uq_cf_pair (user_member_id, friend_member_id)`
  - `friend_streaks`:
    - `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
    - `member1_id BIGINT NOT NULL`, `member2_id BIGINT NOT NULL`
    - `current_streak INT NOT NULL DEFAULT 0`
    - `longest_streak INT NOT NULL DEFAULT 0`
    - `last_streak_date DATE NULL`
    - `together_check_ins INT NOT NULL DEFAULT 0`
    - `together_check_outs INT NOT NULL DEFAULT 0`
    - `last_checkout_together_date DATE NULL`
    - `CONSTRAINT fk_fs_m1 FOREIGN KEY (member1_id) REFERENCES members (id) ON DELETE CASCADE`
    - `CONSTRAINT fk_fs_m2 FOREIGN KEY (member2_id) REFERENCES members (id) ON DELETE CASCADE`
    - `UNIQUE KEY uq_fs_members (member1_id, member2_id)`.

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/resources/db/migration/tenant/V1__init_tenant_schema.sql` | Create | Complete DDL script creating all 14 tenant tables, local constraints, indexes, monetary precision types, and hashed token storage. |

---

## Technical Implementation Guidance

1. **Flyway Compatibility**:
   - `TenantFlywayService.java` targets `classpath:db/migration/tenant` and configures table `flyway_schema_history`.
   - The file must follow Flyway naming conventions: `V1__init_tenant_schema.sql`.
2. **Order of Table Creation (Dependency Graph)**:
   ```
   1. users
   2. gym_config
   3. members (references users)
   4. membership_plans
   5. member_memberships (references members, membership_plans)
   6. membership_requests (references users, membership_plans)
   7. attendance (references members)
   8. revenue (references member_memberships, users)
   9. expenses (references users)
   10. join_requests (references users for reviewer)
   11. gym_invitations (references users for creator)
   12. friend_requests (references members)
   13. custom_friends (references members)
   14. friend_streaks (references members)
   ```
3. **Encoding**:
   Every table statement must terminate with:
   `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;`

---

## Security & Multitenancy Considerations

1. **Complete Database Isolation**: Because each tenant possesses its own MySQL database, data corruption or query errors within one tenant database cannot compromise or reveal records of another tenant.
2. **Zero Password Leakage Invariant**: Tenant databases must never store credential secrets, BCrypt hashes, or password resets. If a tenant database backup is leaked, no authentication credentials are compromised.
3. **Cryptographic Token Hashing**: Invitation tokens are stored strictly as SHA-256 hashes (`invitation_token_hash VARCHAR(64)`), ensuring that reading the `gym_invitations` table does not allow impersonating or accepting invitations.

---

## Verification & Testing Plan

### Compilation Check
```powershell
./mvnw clean test-compile
```

### Integration Test Execution (Once Story 1.7 is in place)
```powershell
./mvnw test -Dtest=TenantPersistenceIntegrationTest -Dspring.profiles.active=test
```

---

## Definition of Done (DoD)

- [x] `V1__init_tenant_schema.sql` is placed in `src/main/resources/db/migration/tenant/`.
- [x] All 14 tenant tables are defined with exact column types, lengths, nullability, defaults, and local foreign keys specified in AC-1.4.1 through AC-1.4.8.
- [x] Table `users` contains **no password column**.
- [x] Table `gym_invitations` contains `invitation_token_hash VARCHAR(64) NOT NULL UNIQUE`.
- [x] All monetary columns are typed as `DECIMAL(10,2) NOT NULL` (or `DECIMAL(10,2) NULL` for optional discounts).
- [x] Cross-database user references are scalar `BIGINT` without physical foreign keys.
- [x] Script structured for execution via `TenantFlywayService.migrate(tenantDbName)`.
- [x] Zero modifications to the 8 protected multitenancy classes.
