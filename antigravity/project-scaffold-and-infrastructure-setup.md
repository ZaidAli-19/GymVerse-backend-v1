# Project Scaffold & Infrastructure Setup Report: GymVerse-Backend-v2

**Date:** October 13, 2026  
**Status:** COMPLETE & VERIFIED  
**Destination Path:** `D:/projects/GymVerse-Backend-v2`  
**Source Path:** `D:/projects/GymVerse-Backend` (Treated strictly as Read-Only)

---

## 1. Executive Summary

As specified in the controlled evolution plan, a clean, independent Spring Boot 3.3.5 / Java 21 backend project has been created at `D:/projects/GymVerse-Backend-v2`. This project selectively reuses proven infrastructure, configuration structures, and database-routing mechanisms from the original GymVerse backend while intentionally excluding legacy business implementations (such as legacy authentication split, legacy owner roles, and coupled domain controllers/entities).

The scaffold successfully compiles both main sources and test sources with zero compilation errors, preserves all protected multitenancy contracts verbatim, and sanitizes all environment configurations.

---

## 2. Protection & Constraint Compliance

| Constraint | Requirement | Status | Verification Evidence |
| :--- | :--- | :--- | :--- |
| **Source Immutability** | Treat `GymVerse-Backend` as read-only; no edits, deletions, or moves. | **COMPLIED** | Git status in `D:/projects/GymVerse-Backend` confirms zero modifications to source files during this task. |
| **Protected Multitenancy Module** | Copy `com.company.GymVerse.multitanancy` verbatim as-is without redesign. | **COMPLIED** | SHA256 hash validation confirms all 8 source files match bit-for-bit (see Section 4). |
| **Exclude Business Implementations** | Exclude legacy controllers, services, repositories, and domain models. | **COMPLIED** | Excluded `platformAuthentication`, `TenantAuthentication`, `membersFunctionality`, `tenantAdminFunctionality`, and `trainerFunctionality`. |
| **Sanitize Configuration** | Externalize credentials and secrets into environment variable references. | **COMPLIED** | Plaintext database credentials, Gmail SMTP app passwords, and JWT secret replaced with `${ENV_VAR:default}` syntax. |
| **Clean Git History** | Independent git tracking without copying legacy commits. | **COMPLIED** | Initialized a fresh Git repository at `D:/projects/GymVerse-Backend-v2/.git/`. |

---

## 3. Inventory of Project Components

### 3.1 Build & Wrapper Foundation
- **`pom.xml`**:
  - Spring Boot `3.3.5` starter parent.
  - Java `21` source and release targeting.
  - Dependencies: Spring Data JPA, Spring Web, MySQL Connector/J, Spring Test, Lombok, Spring Kafka, Spring Mail, JJWT (`0.12.5`/`0.12.6`), Spring Security, Spring Data Redis, Spring WebSocket, Flyway Core & Flyway MySQL.
  - Removed duplicate Lombok dependency block from legacy pom and aligned compiler plugin with Java 21.
- **Maven Wrapper**:
  - `.mvn/wrapper/maven-wrapper.properties` (targeting Maven 3.9.16).
  - `mvnw` (Linux/macOS shell script).
  - `mvnw.cmd` (Windows batch script).
- **Project Root Files**:
  - `.gitignore` (ignores target, IDE configs, credentials).
  - `.gitattributes`.
  - `PROMPT.md` (copied project guidelines).

### 3.2 Configuration & Sanitization (`src/main/resources/application.yaml`)
All secrets from the legacy configuration have been sanitized:
- **Central Datasource**:
  - URL: `${DB_URL:jdbc:mysql://localhost:3306/GymVerse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}`
  - User: `${DB_USERNAME:root}`
  - Password: `${DB_PASSWORD:root123}`
- **Mail Service**:
  - Host: `${MAIL_HOST:smtp.gmail.com}`
  - Username: `${MAIL_USERNAME:}` (removed hardcoded personal Gmail address)
  - Password: `${MAIL_PASSWORD:}` (removed hardcoded Gmail app password)
- **JWT**:
  - Secret: `${JWT_SECRET:...}`
  - Expirations: access (15 min) and refresh (30 days) parameterized with defaults.
- **Multitenancy**:
  - `gymverse.central.db-name`: `${CENTRAL_DB_NAME:GymVerse}`
  - `gymverse.datasource.host`: `${TENANT_DB_HOST:localhost}`
  - `gymverse.datasource.port`: `${TENANT_DB_PORT:3306}`

### 3.3 Protected Multitenancy Module (`com.company.GymVerse.multitanancy`)
All 8 components copied verbatim:
1. `TenantContext.java` — `InheritableThreadLocal` tenant store with thread-safety and propagation.
2. `GymVerseCurrentTenantIdentifierResolver.java` — Hibernate `CurrentTenantIdentifierResolver` defaulting to `"central"`.
3. `GymVerseMultiTenantConnectionProvider.java` — Hibernate connection provider routing between central and tenant connection pools.
4. `TenantDataSourceCache.java` — `ConcurrentHashMap` caching dynamically built HikariCP connection pools.
5. `TenantDatabaseService.java` — Administrative SQL service executing UTF8MB4 schema creation/deletion.
6. `TenantFlywayService.java` — Programmatic Flyway runner targeting `classpath:db/migration/tenant`.
7. `CentralJpaConfig.java` — Primary `EntityManagerFactory` and `PlatformTransactionManager` scanning `models.central` and `repositories.central`.
8. `TenantJpaConfig.java` — Secondary multitenant `EntityManagerFactory` and `PlatformTransactionManager` scanning `models.tenant` and `repositories.tenant`.

### 3.4 Shared Infrastructure & Utilities
- `Application.java` — Standard Spring Boot entry point in `com.company.GymVerse`.
- `config/RedisConfig.java` — Configures `LettuceConnectionFactory`, `RedisTemplate<String, String>`, and connection verification.
- `config/KafkaConfig.java` — Preserved in commented state from source repository pending messaging setup.
- `util/GeoUtil.java` — Pure mathematical Haversine formula utility for gym geofencing.

### 3.5 Package Anchors & Migration Baselines
- `com.company.GymVerse.models.central` (with `package-info.java`)
- `com.company.GymVerse.models.tenant` (with `package-info.java`)
- `com.company.GymVerse.repositories.central` (with `package-info.java`)
- `com.company.GymVerse.repositories.tenant` (with `package-info.java`)
- `src/main/resources/db/migration/central/` (clean baseline ready for v2.1 migrations)
- `src/main/resources/db/migration/tenant/` (clean baseline ready for v2.1 migrations)
- `src/test/java/com/company/GymVerse/ApplicationTests.java` (baseline test scaffold)

---

## 4. Verification Evidence

### 4.1 SHA256 Multitenancy Integrity Matrix
Every file in `com.company.GymVerse.multitanancy` in `GymVerse-Backend-v2` has been compared against `GymVerse-Backend/4Unknown`:

| File Name | Match | SHA256 Hash |
| :--- | :---: | :--- |
| `CentralJpaConfig.java` | **TRUE** | `E4AABC91880871B5DF2FFE6B6E7364E5D4D6D4C366E3B3E1842AFA662D1B8D0C` |
| `GymVerseCurrentTenantIdentifierResolver.java` | **TRUE** | `5C374DDBA9C1DB1552DEECA1410A670C8F4F6FBEA6D88367C5998E3FBD99B4B3` |
| `GymVerseMultiTenantConnectionProvider.java` | **TRUE** | `B22340CE4761E8400CE57AA888B8E6B1EC899D45C7D8701B847A89984E4D3642` |
| `TenantContext.java` | **TRUE** | `EE43434949620EA2B1CA4042987E1DB594146035AE2EAECD87729B04BF5BF911` |
| `TenantDatabaseService.java` | **TRUE** | `4AB56FD6A1F338A3C0AADAC6AA6D9CFBC009AD68F53FBAA732F38E8AC64D39D2` |
| `TenantDataSourceCache.java` | **TRUE** | `5D9A3C12DB5168F61A057DB37942DC1D7D85D67B0862FF089C97696329903C25` |
| `TenantFlywayService.java` | **TRUE** | `FC5D5F8D6E6630B3BB45EC8F4CBC9402F1A961CB3BBA407B9A94994E311FD026` |
| `TenantJpaConfig.java` | **TRUE** | `F0E538AF19EC1698D8C724509C4E5DCB6AF1107434AB88878BE315A04EE926AC` |

### 4.2 Build Execution Verification
- **`mvn compile`**:
  ```
  [INFO] Compiling 16 source files with javac [debug parameters release 21] to target\classes
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- **`mvn test-compile`**:
  ```
  [INFO] Compiling 1 source file with javac [debug parameters release 21] to target\test-classes
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```

---

## 5. Next Steps

With the project scaffold and shared infrastructure established and verified:
1. **User Sign-Off**: Confirm the scaffold and package layout meet expectations.
2. **Phase 1 Implementation**: Implement the Unified Central Identity database schema and JPA entities (`UserAccount`, `GymTenant`, `UserTenantRole`, `SuperAdmin`).
3. **Phase 2 Implementation**: Implement central authentication, OTP verification, password hashing, and the two-stage JWT token exchange.
