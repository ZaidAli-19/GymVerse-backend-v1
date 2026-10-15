---
id: "story-1.1"
epic_id: "epic-1"
title: "Database Configuration & Test Environment Profiles"
status: "implemented"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies: []
files_to_modify:
  - "src/main/resources/application.yaml"
files_to_create:
  - "src/test/resources/application-test.yaml"
---

# Story 1.1: Database Configuration & Test Environment Profiles

## User Story

**As a** backend engineer,  
**I want** unified, sanitized configuration profiles for central and tenant datasources and an isolated test environment profile,  
**So that** the application connects reliably to the central database, dynamically provisions tenant connection pools, and executes integration test suites without failing on external infrastructure dependencies such as Kafka or SMTP servers.

---

## Business & Technical Context

`GymVerse-Backend-v2` is an enterprise SaaS gym management platform utilizing a modular monolith architecture with a database-per-tenant isolation model:
1. **Central Platform Database (`GymVerse`)**: Stores global user accounts (`user_accounts`), gym tenant metadata (`gym_tenants`), user-tenant memberships (`user_tenant_roles`), SaaS subscription plans (`saas_subscription_plans`), subscription agreements (`gym_subscriptions`), and platform audit logs (`admin_audit_logs`).
2. **Tenant Databases (`gym_db_<id>`)**: Each registered gym operates in its own isolated MySQL database, storing gym configuration, members, attendance, plans, financials, and local social graphs.

To support this architecture, the application relies on an existing, verified multitenancy foundation in `com.company.GymVerse.multitanancy`. Central persistence is backed by a primary Spring Boot autoconfigured `DataSource` managed by `CentralJpaConfig`, whereas tenant persistence is dynamically resolved on demand through `TenantDataSourceCache` and `TenantJpaConfig`.

### The Problem Story 1.1 Solves
- In development and CI/CD test environments, running Spring Boot context tests or integration tests with `application.yaml` causes test failures if external dependencies (specifically Apache Kafka on port 9092 and SMTP mail servers) are offline.
- Configuration parameters required by `TenantDataSourceCache` (`gymverse.datasource.host`, `gymverse.datasource.port`, `spring.datasource.username`, `spring.datasource.password`) and `CentralJpaConfig` must be cleanly synchronized, externalized with robust local defaults, and verified.
- Flyway central migration paths must be explicitly anchored to `classpath:db/migration/central`, ensuring that Spring Boot only auto-migrates the central schema on startup, while tenant migrations remain exclusively governed by programmatic invocations of `TenantFlywayService`.

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - JDK 21 installed.
  - Maven wrapper (`mvnw`) operational.
  - The 8 core multitenancy classes in `com.company.GymVerse.multitanancy` remain intact and unmodified.
- **Dependencies**:
  - None. Story 1.1 is the foundational story of Phase 1 upon which Stories 1.2 through 1.7 depend.

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. **Audit & Refine `src/main/resources/application.yaml`**:
   - Verify that `spring.datasource.*` correctly externalizes `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` with local developer fallback values.
   - Verify that `gymverse.central.db-name`, `gymverse.datasource.host`, and `gymverse.datasource.port` are consistently mapped.
   - Ensure `spring.flyway.enabled: true` with `locations: classpath:db/migration/central` and `baseline-on-migrate: true`.
   - Ensure Hibernate DDL generation remains disabled (`spring.jpa.hibernate.ddl-auto: none`) in favor of Flyway migration management.
2. **Author `src/test/resources/application-test.yaml`**:
   - Create a dedicated test profile configuration active during test execution (`@ActiveProfiles("test")`).
   - Configure isolated test database credentials pointing to local MySQL with fallback defaults.
   - Disable automatic startup of Kafka listeners (`spring.kafka.listener.auto-startup: false`) to allow tests to run without an active Kafka broker.
   - Disable mail server connection attempts and SMTP handshakes in test environments.
   - Configure test-appropriate HikariCP timeouts and pool sizes.
   - Enable `DEBUG` logging for `com.company.GymVerse.multitanancy` to assist in diagnosing connection routing and pool caching during tests.

### Out of Scope
- Writing Flyway migration SQL scripts (Story 1.2 for Central schema; Story 1.4 for Tenant schema).
- Modifying any of the 8 protected multitenancy classes in `com.company.GymVerse.multitanancy`.
- Creating JPA entities, enums, or repositories (Stories 1.3 and 1.5).
- Creating the `TenantRoutingBridge` helper component (Story 1.6).
- Creating full integration test fixtures and test cases (Story 1.7).

---

## Detailed Acceptance Criteria

### AC-1.1.1: Central Datasource & Property Externalization
- **Given** the application configuration in `src/main/resources/application.yaml`,
- **When** the Spring environment is initialized,
- **Then** the primary datasource resolves:
  - `spring.datasource.url`: `${DB_URL:jdbc:mysql://localhost:3306/GymVerse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}`
  - `spring.datasource.username`: `${DB_USERNAME:root}`
  - `spring.datasource.password`: `${DB_PASSWORD:root123}`
  - `spring.datasource.driver-class-name`: `com.mysql.cj.jdbc.Driver`
- **And** Hibernate schema management is explicitly set to `spring.jpa.hibernate.ddl-auto: none`.

### AC-1.1.2: Tenant Datasource Cache Property Resolution
- **Given** the configuration parameters required by `TenantDataSourceCache`,
- **When** properties are read from the environment,
- **Then** `gymverse.datasource.host` resolves with default `localhost` (`${TENANT_DB_HOST:localhost}`),
- **And** `gymverse.datasource.port` resolves with default `3306` (`${TENANT_DB_PORT:3306}`),
- **And** `gymverse.central.db-name` resolves with default `GymVerse` (`${CENTRAL_DB_NAME:GymVerse}`),
- **And** `TenantDataSourceCache` can successfully construct valid JDBC URLs formatted as:
  `jdbc:mysql://<host>:<port>/<dbName>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`.

### AC-1.1.3: Central Flyway Migration Configuration
- **Given** Flyway auto-configuration in `src/main/resources/application.yaml`,
- **When** the central application context starts up,
- **Then** `spring.flyway.enabled` is `true`,
- **And** `spring.flyway.locations` is explicitly set to `classpath:db/migration/central`,
- **And** `spring.flyway.baseline-on-migrate` is `true`,
- **And** tenant Flyway migrations under `classpath:db/migration/tenant` are not scanned or executed by Spring's central Flyway bean.

### AC-1.1.4: Creation of Test Environment Profile (`application-test.yaml`)
- **Given** the test environment under `src/test/resources/`,
- **When** `application-test.yaml` is created and evaluated under the active profile `test`,
- **Then** it provides configuration overrides for test execution that mirror the MySQL central datasource and multitenancy host/port mappings,
- **And** sets `spring.flyway.enabled: true` with `locations: classpath:db/migration/central`.

### AC-1.1.5: Offline Test Isolation (Kafka and Mail Auto-Startup Disabled)
- **Given** test execution without external messaging or mail infrastructure running,
- **When** `application-test.yaml` is active,
- **Then** `spring.kafka.listener.auto-startup` is set to `false`,
- **And** Kafka consumers do not attempt to establish broker connections to `localhost:9092`,
- **And** Mail sender beans do not attempt network connections to external SMTP hosts during context refresh,
- **And** the Spring Boot application context can initialize cleanly offline.

### AC-1.1.6: Protected Multitenancy Invariant Enforcement
- **Given** the 8 protected multitenancy classes in `com.company.GymVerse.multitanancy`:
  - `CentralJpaConfig.java`
  - `TenantJpaConfig.java`
  - `GymVerseCurrentTenantIdentifierResolver.java`
  - `GymVerseMultiTenantConnectionProvider.java`
  - `TenantDataSourceCache.java`
  - `TenantDatabaseService.java`
  - `TenantFlywayService.java`
  - `TenantContext.java`
- **When** Story 1.1 is implemented and verified,
- **Then** all 8 files remain 100% byte-for-byte identical to their original state with zero line or character alterations.

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/resources/application.yaml` | Inspect / Update | Verify central datasource, Flyway location, and tenant host/port configuration properties. |
| `src/test/resources/application-test.yaml` | Create | New test configuration profile disabling Kafka listener auto-startup, configuring offline testing, and enabling multitenancy debug logging. |

---

## Technical Implementation Guidance

### 1. `src/main/resources/application.yaml` Verification
Ensure the following blocks are present and cleanly structured:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/GymVerse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root123}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: none   # Flyway manages schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration/central
    baseline-on-migrate: true

gymverse:
  central:
    db-name: ${CENTRAL_DB_NAME:GymVerse}
  datasource:
    host: ${TENANT_DB_HOST:localhost}
    port: ${TENANT_DB_PORT:3306}
```

### 2. `src/test/resources/application-test.yaml` Structure
Create the file with the following specifications:
```yaml
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/GymVerse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root123}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration/central
    baseline-on-migrate: true

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    listener:
      auto-startup: false   # Prevent blocking when Kafka broker is offline

  mail:
    host: localhost
    port: 2525
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

gymverse:
  central:
    db-name: ${CENTRAL_DB_NAME:GymVerse}
  datasource:
    host: ${TENANT_DB_HOST:localhost}
    port: ${TENANT_DB_PORT:3306}

logging:
  level:
    com.company.GymVerse.multitanancy: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Security & Multitenancy Considerations

1. **Credential Segregation**:
   - No production passwords or sensitive secrets are hardcoded in YAML files.
   - All credentials use environment variable substitution (`${DB_USERNAME:...}`, `${DB_PASSWORD:...}`) with safe defaults for local developer workstations.
2. **MySQL Privilege Requirements**:
   - `TenantDatabaseService` executes administrative SQL statements (`CREATE DATABASE IF NOT EXISTS`, `DROP DATABASE IF EXISTS`).
   - `TenantDataSourceCache` creates connection pools connecting to `gym_db_*` using the shared credentials (`spring.datasource.username` and `spring.datasource.password`).
   - The configured MySQL user must hold `ALL PRIVILEGES` on `GymVerse` and `gym_db_%`.
3. **Multitenancy Class Immutability**:
   - The eight multitenancy classes in `com.company.GymVerse.multitanancy` are verified and protected. Story 1.1 must not modify any Java code.

---

## Verification & Testing Plan

### Build & Compilation Checks
Verify that the configuration files are syntactically valid YAML and that the Maven test compile phase succeeds:
```powershell
./mvnw clean test-compile
```

### Context Load Verification
When executed with `-Dspring.profiles.active=test`, the application context must load without hanging or throwing exceptions due to unreachable Kafka or Mail services:
```powershell
./mvnw test -Dtest=ApplicationTests -Dspring.profiles.active=test
```

---

## Definition of Done (DoD)

- [x] `src/main/resources/application.yaml` is verified to have correct central datasource, Flyway, and tenant host/port properties.
- [x] `src/test/resources/application-test.yaml` is created with active profile `test`.
- [x] Kafka listener auto-startup is disabled (`spring.kafka.listener.auto-startup: false`) in `application-test.yaml`.
- [x] External mail probing is neutralized in `application-test.yaml`.
- [x] Flyway location in both profiles strictly targets `classpath:db/migration/central`.
- [x] All 8 protected multitenancy classes remain 100% bit-for-bit unchanged.
- [x] `./mvnw clean test-compile` succeeds without errors or warnings.
