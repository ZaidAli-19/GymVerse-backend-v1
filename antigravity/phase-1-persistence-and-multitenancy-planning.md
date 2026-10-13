# Engineering Record: Phase 1 Planning — Persistence & Multi-Tenancy Foundation (Refined)

**Date:** October 13, 2026  
**Status:** FORENSIC REVIEW & REFINEMENT COMPLETE — PENDING USER APPROVAL  
**Author:** Winston, System Architect 🏗️  
**Project:** `GymVerse-Backend-v2` (`D:/projects/GymVerse-Backend-v2`)  
**Target Artifact:** `_bmad-output/planning-artifacts/phase-1-epics.md`  

---

## 1. Executive Summary

In response to forensic review directives, the planning breakdown for **Phase 1: Enterprise Multi-Tenant Persistence Foundation** has been thoroughly audited and refined.

All updates were applied exclusively to `_bmad-output/planning-artifacts/phase-1-epics.md`. No Java code, configuration files, migration scripts, dependencies, tests, or architecture documents were modified.

---

## 2. Forensic Findings & Technical Refinements

### 2.1 Multitenancy Subsystem Inspection & Story 1.6 Correction
- **Protected Classes Inspection:**
  - `CentralJpaConfig`: Binds `@Primary` `centralEntityManagerFactory` and `@Primary` `centralTransactionManager` over the central `DataSource` (`GymVerse`).
  - `TenantJpaConfig`: Binds `tenantEntityManagerFactory` and `tenantTransactionManager` over Hibernate's `MULTI_TENANT_CONNECTION_PROVIDER` and `MULTI_TENANT_IDENTIFIER_RESOLVER`.
  - `GymVerseCurrentTenantIdentifierResolver`: Resolves tenant identifier from `TenantContext.getCurrentTenant()`, falling back to `"central"`.
  - `GymVerseMultiTenantConnectionProvider`: Injects `TenantDataSourceCache` and delegates connections based on tenant identifier.
  - `TenantDataSourceCache`: Dynamically caches HikariCP pools targeting `jdbc:mysql://host:port/tenantDbName`.
  - `TenantDatabaseService`: Administrative SQL execution for `CREATE DATABASE IF NOT EXISTS` and `DROP DATABASE IF EXISTS`.
  - `TenantFlywayService`: Programmatic Flyway runner against tenant datasource pools.
  - `TenantContext`: `InheritableThreadLocal` tenant store.
- **Story 1.6 Transaction & Routing Lifecycle Correction:**
  - In Hibernate and Spring `JpaTransactionManager`, when a transaction boundary begins, the connection and tenant resolver are queried **immediately at transaction initiation**.
  - **Correction Applied:** `TenantContext.setCurrentTenant(tenantDbName)` **must be set before** entering the `@Transactional("tenantTransactionManager")` boundary (or transaction initiation block). Calling it inside an active transaction is ineffective as Hibernate will have already resolved `"central"`.
  - **Cleanup Invariant:** `TenantContext.clear()` **must be called in a `finally` block** after transaction completion to prevent thread-local leakage.

### 2.2 Monetary Field Precision Standards
- **Discrepancy:** `docs/architecture.md` Section 4 listed `DOUBLE` for financial amounts (e.g., `saas_subscription_plans.price`, `gym_subscriptions.price_paid`, `membership_plans.base_price`, `member_memberships.price_at_purchase`, `revenue.amount`, `expenses.amount`).
- **Resolution:** In strict accordance with `PROMPT.md` Section 4.1 ("Monetary & Decimal Precision"), all monetary columns in both Central and Tenant schemas are mandated as **`DECIMAL(10,2) NOT NULL`** in SQL DDL, and mapped to **`BigDecimal`** with `@Column(precision = 10, scale = 2)` in JPA entities.

### 2.3 Invitation Token Security Flag
- **Finding:** `docs/architecture.md` specifies `gym_invitations.invitation_token VARCHAR(255) UK`.
- **Security Flag:** Plaintext bearer tokens stored in database tables risk unauthorized redemption if database reads are compromised.
- **Recommendation:** Formally recommended storing a SHA-256 hash (`invitation_token_hash VARCHAR(64) NOT NULL UNIQUE`). Flagged for user approval while leaving business logic out of Phase 1.

### 2.4 Complete Schema & Entity Audit Matrix
- Audited all 6 central tables and all 14 tenant tables against `docs/architecture.md` Sections 4.1 and 4.2.
- Verified that cross-database references (`TenantUser.centralUserId`, `JoinRequest.centralUserId`) are scalar `Long` fields rather than JPA entity relationships.
- Verified that tenant `users` strictly contains **zero password columns**.

### 2.5 Automated Testing & MySQL Test Environment
- **Story 1.7 Test Suite Structure:**
  - `ApplicationContextStartupIntegrationTest`: Context startup with both entity managers active.
  - `CentralPersistenceIntegrationTest`: Automatic Flyway migration, CRUD on `user_accounts`, `gym_tenants`, `user_tenant_roles`, and constraint violation assertions.
  - `TenantPersistenceIntegrationTest`: Programmatic database creation (`TenantDatabaseService`), migration (`TenantFlywayService`), and CRUD on tenant entities.
  - `TenantIsolationIntegrationTest`: Physical database isolation between `gym_db_test_001` and `gym_db_test_002`.
  - **Teardown Routines:** Mandatory `TenantDataSourceCache.evict()`, `TenantDatabaseService.dropDatabase()`, and `TenantContext.clear()` in `@AfterEach` / `@AfterAll`.
- **Test Environment & Commands:**
  - Target MySQL 8.0+ on `localhost:3306` with database administration privileges.
  - Exact execution command:
    ```powershell
    ./mvnw test -Dtest=ApplicationTests,CentralPersistenceIntegrationTest,TenantPersistenceIntegrationTest,TenantIsolationIntegrationTest
    ```

---

## 3. Seven-Story Structure Preservation

The refined Phase 1 plan retains the clean, 7-story sequential architecture:
1. **Story 1.1:** Database Configuration & Test Environment Profiles
2. **Story 1.2:** Central Database Flyway Migration (`V1__init_central_schema.sql`)
3. **Story 1.3:** Central JPA Entities and Repositories
4. **Story 1.4:** Tenant Database Flyway Migration (`V1__init_tenant_schema.sql`)
5. **Story 1.5:** Tenant-Local JPA Entities and Repositories
6. **Story 1.6:** Multitenancy Bridge Integration & Tenant Routing Verification
7. **Story 1.7:** Comprehensive Integration Test Suite (Context Startup, Migrations & Isolation)
