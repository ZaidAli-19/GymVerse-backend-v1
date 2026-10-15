---
id: "story-pa-1"
epic_id: "epic-pa"
title: "Initial Platform Administrator Provisioning Engine (YAML-Configured via platformAdminSeed.yml)"
status: "implemented"
type: "platform-admin-bootstrap"
created_at: "2026-10-15"
amended_at: "2026-10-15"
dependencies: []
files_to_modify:
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
  - "src/main/java/com/company/GymVerse/repositories/central/UserAccountRepository.java"
files_to_create:
  - "src/main/resources/platformAdminSeed.yml"
  - "src/main/java/com/company/GymVerse/config/SecurityCryptoConfig.java"
  - "src/main/java/com/company/GymVerse/config/PlatformAdminSeedProperties.java"
  - "src/main/java/com/company/GymVerse/platformAdmin/service/PlatformAdminProvisioningService.java"
  - "src/main/java/com/company/GymVerse/config/PlatformAdminBootstrapConfig.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminSeedPropertiesTest.java"
  - "src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminProvisioningIntegrationTest.java"
---

# Story PA-1: Initial Platform Administrator Provisioning Engine (YAML-Configured via `platformAdminSeed.yml`)

## ⚠️ MANDATORY DIRECTIVE FOR IMPLEMENTING AGENTS
> **STOP AND READ BEFORE PROCEEDING:**  
> 1. You must thoroughly read and strictly adhere to [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md) and [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) before writing any code.  
> 2. You must inspect existing implementations in `com.company.GymVerse.models.central.UserAccount` and `com.company.GymVerse.repositories.central.UserAccountRepository`.  
> 3. You must preserve all eight protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.  
> 4. You must store the initial Platform Admin configuration in the dedicated file `src/main/resources/platformAdminSeed.yml` and import it into `application.yaml` via Spring Boot configuration import. Do NOT define seed properties directly in `application.yaml`.  
> 5. You must NEVER hardcode literal admin passwords or provide default password fallbacks. Sensitive values must be backed by environment-variable placeholders.  
> 6. You must keep configuration binding (`PlatformAdminSeedProperties`) strictly separated from seeding business logic (`PlatformAdminProvisioningService`).  
> 7. You must document any technical conflicts or unresolved decisions before proceeding.  
> 8. You must log all changes, technical rationale, and verification commands in `antigravity/<task-descriptive-name>.md`.

---

## 1. User Story

**As a** platform system administrator,  
**I want** the initial platform administrator account to be configured in a dedicated `platformAdminSeed.yml` file, imported into `application.yaml`, validated via `@ConfigurationProperties`, and provisioned by an idempotent startup seeder into the central database,  
**So that** the GymVerse platform establishes an authoritative superadmin identity on first boot without hardcoding secrets, committing plaintext credentials to version control, causing password resets on repeated restarts, or silently promoting pre-existing non-admin accounts.

---

## 2. Business & Technical Context

In the GymVerse unified central identity model ([docs/architecture.md](file:///D:/projects/GymVerse-Backend-v2/docs/architecture.md) Sections 3.1 & 7), all platform users exist as records in `user_accounts` in the central MySQL database (`GymVerse`).

Platform Administrators are distinguished by the boolean attribute:
- `user_accounts.is_platform_admin = TRUE`
- `user_accounts.status = 'ACTIVE'`
- `user_accounts.email_verified = TRUE`

### Requirements Imposed by this Specification:
1. **Dedicated Configuration File (`platformAdminSeed.yml`)**:
   - The seed configuration must reside in `src/main/resources/platformAdminSeed.yml`.
   - `application.yaml` must import this file via Spring Boot's configuration import mechanism (`spring.config.import: classpath:platformAdminSeed.yml`).
   - The import is **mandatory**; if `platformAdminSeed.yml` is missing, application startup fails fast so misconfigurations are caught immediately.
   - Environments that do not provision the initial admin can explicitly disable execution via `gymverse.platform-admin.seed.enabled: false`.
2. **Configuration Prefix & Property Model**:
   - Configuration is bound under prefix `gymverse.platform-admin.seed` into `PlatformAdminSeedProperties`.
   - Descriptively defines: `enabled`, `email`, `first-name`, `last-name`, `password`, `phone-number`, `email-verified`, `is-platform-admin`, `status`.
3. **Zero Secrets in Git**:
   - Plaintext passwords must NEVER be committed to version control.
   - The `password` property must use the placeholder `${PLATFORM_ADMIN_PASSWORD}` with **NO literal default fallback**. When seeding is enabled, the password must be supplied via the environment variable.
4. **Idempotency & Password Protection**:
   - Repeated application restarts must never overwrite or reset an existing administrator's password.
5. **Safe Conflict Policy (No Silent Promotion)**:
   - If the configured seed email already exists in `user_accounts` and has `is_platform_admin = false`, the seeder must **NOT** silently promote that account. It must abort the seeding operation and log a critical configuration conflict to prevent unauthorized privilege escalation.
6. **Zero Privilege Bleed**:
   - Standard user registration flows must never grant platform-admin privileges (`UserAccount.isPlatformAdmin` strictly defaults to `false`).

---

## 3. Prerequisites & Dependencies

- **Prerequisites**:
  - Phase 1 central persistence foundation completed (`V1__init_central_schema.sql` applied).
  - Central entities (`UserAccount`) and repository (`UserAccountRepository`) active under `centralTransactionManager`.
- **Dependencies**:
  - None. Foundational story for platform-level identity and administration.

---

## 4. Scope & Boundaries

### In Scope
1. **Dedicated Configuration File (`src/main/resources/platformAdminSeed.yml`)**:
   ```yaml
   gymverse:
     platform-admin:
       seed:
         enabled: ${PLATFORM_ADMIN_SEED_ENABLED:true}
         email: ${PLATFORM_ADMIN_EMAIL:admin@gymverse.com}
         first-name: ${PLATFORM_ADMIN_FIRST_NAME:Platform}
         last-name: ${PLATFORM_ADMIN_LAST_NAME:Superadmin}
         password: ${PLATFORM_ADMIN_PASSWORD}
         phone-number: ${PLATFORM_ADMIN_PHONE:+10000000000}
         email-verified: ${PLATFORM_ADMIN_EMAIL_VERIFIED:true}
         is-platform-admin: ${PLATFORM_ADMIN_IS_PLATFORM_ADMIN:true}
         status: ${PLATFORM_ADMIN_STATUS:ACTIVE}
   ```
2. **Configuration Import in `src/main/resources/application.yaml`**:
   ```yaml
   spring:
     config:
       import:
         - classpath:platformAdminSeed.yml
   ```
3. **Configuration Properties Class (`PlatformAdminSeedProperties`)**:
   - Annotated with `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")` and `@Component` (or enabled via `@EnableConfigurationProperties`).
   - Fields matching YAML keys: `enabled`, `email`, `firstName`, `lastName`, `password`, `phoneNumber`, `emailVerified`, `isPlatformAdmin`, `status`.
   - Validation constraints:
     - When `enabled = true`, `email` must be valid and non-blank; `firstName`, `lastName`, and `password` must be non-blank.
     - If `password` is absent or empty when `enabled = true`, validation throws `IllegalStateException("Initial Platform Admin password must be provided via PLATFORM_ADMIN_PASSWORD environment variable when seeding is enabled")`.
4. **Security Crypto Configuration (`SecurityCryptoConfig`)**:
   - Exposes shared `@Bean PasswordEncoder passwordEncoder()` returning `new BCryptPasswordEncoder()`.
5. **Repository Enhancements (`UserAccountRepository`)**:
   - Add: `boolean existsByIsPlatformAdminTrue();`
   - Add: `Optional<UserAccount> findFirstByIsPlatformAdminTrue();`
6. **Provisioning Service (`PlatformAdminProvisioningService`)**:
   - Injected with `PlatformAdminSeedProperties`, `UserAccountRepository`, and `PasswordEncoder`.
   - Method: `void seedInitialPlatformAdmin()`.
   - Executes under `@Transactional("centralTransactionManager")`.
   - **Operational Workflow**:
     1. If `properties.isEnabled()` is `false`, log info: `"Initial platform admin seeding is disabled via configuration. Skipping."` and return.
     2. Validates that `properties.getPassword()` is non-blank.
     3. Checks `userAccountRepository.findByEmail(properties.getEmail())`:
        - **Case 1 (Admin Already Exists - Idempotent Skip)**: If an account exists and `userAccount.isPlatformAdmin() == true`:
          Log info: `"Platform administrator account already initialized for {email}. Skipping seed."`
          **DO NOT** modify password, timestamps, or profile attributes.
        - **Case 2 (Conflict with Non-Admin Account - Safe Conflict Guard)**: If an account exists and `userAccount.isPlatformAdmin() == false`:
          Log error: `"Seed conflict: Account with email '{email}' already exists but is NOT a platform administrator. Aborting seeding to prevent unauthorized privilege escalation."`
          Throw `IllegalStateException("Cannot seed platform administrator: email already registered to a non-admin user")`.
        - **Case 3 (Fresh Provisioning)**: If no account exists:
          Hash password via `passwordEncoder.encode(properties.getPassword())`.
          Build `UserAccount` with `isPlatformAdmin = true`, `emailVerified = true`, `status = UserAccountStatus.ACTIVE`.
          Persist via `userAccountRepository.save(admin)`.
          Log info: `"Initial platform administrator created successfully for: {email}"`.
7. **Application Startup Hook (`PlatformAdminBootstrapConfig`)**:
   - An `ApplicationRunner` bean invoking `provisioningService.seedInitialPlatformAdmin()`.
8. **Automated Unit & Integration Tests**:
   - Property binding and validation tests.
   - Idempotency, BCrypt hashing, and conflict handling tests against live MySQL.

### Out of Scope
- Implementing login, token generation, or authentication endpoints (separate Auth Epic).
- Modifying tenant databases or tenant-local user tables.
- Modifying the 8 protected multitenancy classes.

---

## 5. Detailed Acceptance Criteria

### AC-PA1.1: Mandatory YAML Import & Configuration Binding
- **Given** `application.yaml` imports `classpath:platformAdminSeed.yml`,
- **When** the Spring application context initializes,
- **Then** Spring Boot loads `platformAdminSeed.yml` and binds all properties to `PlatformAdminSeedProperties`,
- **And** if `platformAdminSeed.yml` is missing, context startup fails with `ConfigDataResourceNotFoundException`,
- **And** if `seed.enabled = true` and `PLATFORM_ADMIN_PASSWORD` is not set, validation fails with an explicit error message.

### AC-PA1.2: Idempotent Bootstrapping on Fresh Database
- **Given** a clean central database with zero records in `user_accounts`,
- **When** the application starts up with `seed.enabled = true` and valid `PLATFORM_ADMIN_PASSWORD`,
- **Then** `PlatformAdminProvisioningService` persists a new `UserAccount` record with:
  - `email` matching the configured YAML value.
  - `firstName` and `lastName` matching the configured YAML values.
  - `password` stored as a valid BCrypt hash matching `PLATFORM_ADMIN_PASSWORD` (verifiable by `passwordEncoder.matches()`).
  - `isPlatformAdmin = true`.
  - `emailVerified = true`.
  - `status = UserAccountStatus.ACTIVE`.
  - `createdAt` and `updatedAt` non-null UTC timestamps.
- **And** logs: `"Initial platform administrator created successfully for: {email}"`.

### AC-PA1.3: Idempotent Safe Skip on Subsequent Startup
- **Given** a central database where a platform administrator already exists with the configured email,
- **When** the application restarts or boots again,
- **Then** `PlatformAdminProvisioningService` detects that the account exists and is already a platform admin,
- **And** it **DOES NOT** overwrite the password hash, alter timestamps, or modify credentials,
- **And** logs: `"Platform administrator account already initialized for {email}. Skipping seed."`

### AC-PA1.4: Safe Conflict Guard on Existing Non-Admin Account
- **Given** a central user account exists with the configured email but `isPlatformAdmin = false`,
- **When** the seeder executes,
- **Then** the service **DOES NOT** silently promote the account or reset its password,
- **And** throws `IllegalStateException` with a descriptive message, preventing startup in an inconsistent authorization state.

### AC-PA1.5: Explicit Seeding Disable Invariant
- **Given** configuration `gymverse.platform-admin.seed.enabled = false`,
- **When** the application starts up,
- **Then** the seeder performs an immediate no-op and logs: `"Initial platform admin seeding is disabled via configuration. Skipping."`

### AC-PA1.6: Zero Privilege Bleed Invariant
- **Given** standard user registration operations in `user_accounts`,
- **When** an ordinary user is registered,
- **Then** `UserAccount.isPlatformAdmin` defaults strictly to `false` and is never granted through ordinary registration.

---

## 6. Expected Code Impact

### Files to Modify:
1. `src/main/resources/application.yaml`:
   - Add `spring.config.import: classpath:platformAdminSeed.yml`.
2. `src/test/resources/application-test.yaml`:
   - Add test profile overrides and test import for `platformAdminSeed.yml`.
3. `src/main/java/com/company/GymVerse/repositories/central/UserAccountRepository.java`:
   - Add: `boolean existsByIsPlatformAdminTrue();`
   - Add: `Optional<UserAccount> findFirstByIsPlatformAdminTrue();`

### Files to Create:
1. `src/main/resources/platformAdminSeed.yml`:
   - Dedicated YAML configuration for initial Platform Admin seed properties.
2. `src/main/java/com/company/GymVerse/config/SecurityCryptoConfig.java`:
   - Exposes `PasswordEncoder` bean (`BCryptPasswordEncoder`).
3. `src/main/java/com/company/GymVerse/config/PlatformAdminSeedProperties.java`:
   - POJO with `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")` and validation constraints.
4. `src/main/java/com/company/GymVerse/platformAdmin/service/PlatformAdminProvisioningService.java`:
   - `@Service` component containing idempotent seeding logic and conflict guard.
5. `src/main/java/com/company/GymVerse/config/PlatformAdminBootstrapConfig.java`:
   - `@Configuration` defining `ApplicationRunner initialPlatformAdminRunner(...)`.
6. `src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminSeedPropertiesTest.java`:
   - Unit tests verifying YAML property binding, missing password failure, and invalid email validation.
7. `src/test/java/com/company/GymVerse/platformAdmin/PlatformAdminProvisioningIntegrationTest.java`:
   - Integration tests verifying fresh creation, BCrypt verification, idempotent restart, non-admin conflict rejection, and disabled flag handling against live MySQL.

---

## 7. Error Handling & Security Considerations

1. **Zero Secret Exposure in Version Control**:
   - Plaintext passwords must NEVER appear in `platformAdminSeed.yml`, `application.yaml`, or source code.
   - The password placeholder `${PLATFORM_ADMIN_PASSWORD}` has no literal default fallback.
2. **Transaction Isolation**:
   - Seeding executes under `@Transactional("centralTransactionManager")`.
3. **Safe Conflict Prevention**:
   - Attempting to seed over an existing regular member/staff account aborts cleanly without privilege escalation.
4. **Mandatory Import Policy**:
   - The YAML import is mandatory for early misconfiguration detection. Environments where seeding is not desired disable execution via `enabled = false`.

---

## 8. Test Requirements

1. **`PlatformAdminSeedPropertiesTest`**:
   - Verify `platformAdminSeed.yml` properties bind cleanly to `PlatformAdminSeedProperties`.
   - Verify validation fails when `password` is missing/blank while `enabled = true`.
   - Verify validation fails when `email` is invalid.
2. **`PlatformAdminProvisioningIntegrationTest`**:
   - **Test 1 (Fresh Boot)**: Seeds admin into `user_accounts` with BCrypt hash and `isPlatformAdmin = true`.
   - **Test 2 (BCrypt Verification)**: Verifies `passwordEncoder.matches(rawPassword, persisted.getPassword())` is `true`.
   - **Test 3 (Idempotent Restart)**: Running seeder again leaves row count unchanged and password hash identical.
   - **Test 4 (Non-Admin Conflict)**: Pre-existing user with `isPlatformAdmin = false` causes seeder to abort with `IllegalStateException` without modifying user.
   - **Test 5 (Disabled Flag)**: Seed execution when `enabled = false` performs a clean no-op.

---

## 9. Definition of Done

- Code compiles cleanly with zero warnings (`./mvnw compile`).
- Configuration resides in `src/main/resources/platformAdminSeed.yml` and is imported by `application.yaml`.
- Seed properties bind via `@ConfigurationProperties(prefix = "gymverse.platform-admin.seed")` without hardcoded secrets.
- Initial admin account is created with `isPlatformAdmin = true` and verified BCrypt hash.
- Repeated startups are strictly idempotent with zero password resets.
- Existing non-admin accounts are protected from silent promotion.
- Automated tests pass against live MySQL central database.
- Task log created in `antigravity/` detailing execution and rationale.
