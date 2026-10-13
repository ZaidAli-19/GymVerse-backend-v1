---
id: "story-1.3"
epic_id: "epic-1"
title: "Central JPA Entities and Repositories"
status: "implemented"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies:
  - "story-1.2"
files_to_modify: []
files_to_create:
  - "src/main/java/com/company/GymVerse/models/central/UserAccount.java"
  - "src/main/java/com/company/GymVerse/models/central/GymTenant.java"
  - "src/main/java/com/company/GymVerse/models/central/UserTenantRole.java"
  - "src/main/java/com/company/GymVerse/models/central/SaasSubscriptionPlan.java"
  - "src/main/java/com/company/GymVerse/models/central/GymSubscription.java"
  - "src/main/java/com/company/GymVerse/models/central/AdminAuditLog.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/UserAccountStatus.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/GymStatus.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/TenantRole.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/AdminAuthority.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/TenantMembershipStatus.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/BillingCycle.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/GymSubscriptionStatus.java"
  - "src/main/java/com/company/GymVerse/models/central/enums/AdminAuditEvent.java"
  - "src/main/java/com/company/GymVerse/repositories/central/UserAccountRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/central/GymTenantRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/central/UserTenantRoleRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/central/SaasSubscriptionPlanRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/central/GymSubscriptionRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/central/AdminAuditLogRepository.java"
---

# Story 1.3: Central JPA Entities and Repositories

## User Story

**As a** backend engineer,  
**I want** strongly typed JPA entities, enums, and Spring Data repositories for the central platform domain,  
**So that** the application can execute type-safe persistence and query operations against `user_accounts`, `gym_tenants`, `user_tenant_roles`, `saas_subscription_plans`, `gym_subscriptions`, and `admin_audit_logs` under the management of `centralEntityManagerFactory` and `centralTransactionManager`.

---

## Business & Technical Context

The central domain model manages platform-level concerns: authentication identity, gym provisioning, membership authorizations, SaaS subscriptions, and administrative audit logging.

In [`CentralJpaConfig.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/multitanancy/CentralJpaConfig.java):
- Base package scanned for entities: `com.company.GymVerse.models.central`
- Base package scanned for repositories: `com.company.GymVerse.repositories.central`
- Physical naming strategy: `org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy`
- DDL auto: `none`
- Entity manager factory bean: `@Primary centralEntityManagerFactory`
- Transaction manager bean: `@Primary centralTransactionManager`

All central repositories must extend `JpaRepository<Entity, Long>` and be managed by `centralTransactionManager`. Entity fields must strictly match the database types established in Story 1.2 (`V1__init_central_schema.sql`).

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - Story 1.1 completed (datasource configuration).
  - Story 1.2 completed (central schema Flyway migration script available).
- **Dependencies**:
  - Depends on Story 1.2.
  - Required by Story 1.6 (Multitenancy Bridge) and Story 1.7 (Integration Tests).

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. Creation of 8 central enums in package `com.company.GymVerse.models.central.enums`.
2. Creation of 6 central JPA entities in package `com.company.GymVerse.models.central`:
   - `UserAccount`
   - `GymTenant`
   - `UserTenantRole`
   - `SaasSubscriptionPlan`
   - `GymSubscription`
   - `AdminAuditLog`
3. Creation of 6 central Spring Data JPA repositories in package `com.company.GymVerse.repositories.central`:
   - `UserAccountRepository`
   - `GymTenantRepository`
   - `UserTenantRoleRepository`
   - `SaasSubscriptionPlanRepository`
   - `GymSubscriptionRepository`
   - `AdminAuditLogRepository`
4. Strict mapping of monetary values using `BigDecimal` with `@Column(precision = 10, scale = 2, nullable = false)`.
5. Enumerated column mapping using `@Enumerated(EnumType.STRING)`.
6. UTC timestamps mapped using `java.time.Instant` or `java.time.LocalDate` for calendar dates.

### Out of Scope
- Creating tenant-local entities or repositories (Story 1.5).
- Modifying `CentralJpaConfig.java` or any other protected multitenancy class.
- Implementing authentication service logic, token issuance, or REST controllers.
- Implementing cross-database JPA associations (Tenant entities must never have `@ManyToOne` or `@OneToMany` targeting Central entities).

---

## Detailed Acceptance Criteria

### AC-1.3.1: Central Enum Definitions
- **Given** package `com.company.GymVerse.models.central.enums`,
- **When** compiled,
- **Then** the following enums exist with exact constants:
  - `UserAccountStatus`: `PENDING`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`
  - `GymStatus`: `ACTIVE`, `SUSPENDED`, `INACTIVE`
  - `TenantRole`: `TENANT_ADMIN`, `MEMBER`, `TRAINER`, `STAFF`
  - `AdminAuthority`: `PRIMARY`, `DELEGATED`, `NONE`
  - `TenantMembershipStatus`: `ACTIVE`, `PENDING`, `SUSPENDED`, `REVOKED`
  - `BillingCycle`: `MONTHLY`, `QUARTERLY`, `ANNUAL`
  - `GymSubscriptionStatus`: `TRIAL`, `ACTIVE`, `EXPIRED`, `CANCELLED`
  - `AdminAuditEvent`: `APPOINT_DELEGATED_ADMIN`, `REVOKE_DELEGATED_ADMIN`, `TRANSFER_PRIMARY_AUTHORITY`

### AC-1.3.2: `UserAccount` Entity Mapping
- **Given** table `user_accounts`,
- **When** mapped by `UserAccount`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@Column(unique = true, nullable = false) private String email;`
  - `@Column(nullable = false, length = 100) private String firstName;`
  - `@Column(nullable = false, length = 100) private String lastName;`
  - `@Column(nullable = false) private String password;`
  - `@Column(length = 30) private String phoneNumber;`
  - `@Column(nullable = false) private boolean emailVerified;`
  - `@Column(nullable = false) private boolean isPlatformAdmin;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private UserAccountStatus status;`
  - `@Column(nullable = false, updatable = false) private Instant createdAt;`
  - `@Column(nullable = false) private Instant updatedAt;`
  - `@PrePersist` and `@PreUpdate` lifecycle callbacks managing `createdAt` and `updatedAt`.

### AC-1.3.3: `GymTenant` Entity Mapping
- **Given** table `gym_tenants`,
- **When** mapped by `GymTenant`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@Column(nullable = false, length = 200) private String gymName;`
  - `@Column(nullable = false) private String contactEmail;`
  - `@Column(length = 30) private String phoneNumber;`
  - `@Column(nullable = false) private String address;`
  - `@Column(nullable = false, length = 100) private String city;`
  - `@Column(length = 500) private String logoUrl;`
  - `@Column(unique = true, nullable = false, length = 50) private String gymCode;`
  - `@Column(unique = true, nullable = false, length = 100) private String dbName;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private GymStatus status;`
  - `@Column(nullable = false, updatable = false) private Instant registeredAt;`
  - `private Instant suspendedAt;`
  - `@Column(columnDefinition = "TEXT") private String suspensionReason;`

### AC-1.3.4: `UserTenantRole` Entity Mapping
- **Given** table `user_tenant_roles`,
- **When** mapped by `UserTenantRole`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_account_id", nullable = false) private UserAccount userAccount;`
  - `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "gym_tenant_id", nullable = false) private GymTenant gymTenant;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TenantRole role;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AdminAuthority adminAuthority;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TenantMembershipStatus status;`
  - `@Column(nullable = false, updatable = false) private Instant joinedAt;`
  - `@Column(nullable = false) private Instant updatedAt;`

### AC-1.3.5: `SaasSubscriptionPlan` & `GymSubscription` Entity Mapping
- **Given** tables `saas_subscription_plans` and `gym_subscriptions`,
- **When** mapped,
- **Then**:
  - `SaasSubscriptionPlan` maps `price` as `BigDecimal` with `@Column(precision = 10, scale = 2, nullable = false)`, `billingCycle` as `@Enumerated(EnumType.STRING) BillingCycle`, `maxMembers`, `maxTrainers`, boolean feature flags, and `active`.
  - `GymSubscription` maps `gymTenant` (`@ManyToOne` LAZY), `plan` (`@ManyToOne` LAZY), `pricePaid` as `BigDecimal` with `@Column(precision = 10, scale = 2, nullable = false)`, `billingCycle` (`@Enumerated`), `startDate` (`LocalDate`), `endDate` (`LocalDate`), `status` (`@Enumerated(EnumType.STRING) GymSubscriptionStatus`), `subscribedAt` (`Instant`), and `transactionReference` (`String`).

### AC-1.3.6: `AdminAuditLog` Entity Mapping
- **Given** table `admin_audit_logs`,
- **When** mapped by `AdminAuditLog`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "gym_tenant_id", nullable = false) private GymTenant gymTenant;`
  - `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_user_account_id", nullable = false) private UserAccount actorUserAccount;`
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "target_user_account_id") private UserAccount targetUserAccount;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private AdminAuditEvent action;`
  - `@Column(columnDefinition = "TEXT") private String metadata;`
  - `@Column(nullable = false, updatable = false) private Instant timestamp;`

### AC-1.3.7: Central Repository Query Methods
- **Given** package `com.company.GymVerse.repositories.central`,
- **When** repositories are scanned by `@EnableJpaRepositories` in `CentralJpaConfig`,
- **Then** the repositories expose the following query contracts:
  - `UserAccountRepository`:
    - `Optional<UserAccount> findByEmail(String email);`
    - `boolean existsByEmail(String email);`
  - `GymTenantRepository`:
    - `Optional<GymTenant> findByGymCode(String gymCode);`
    - `Optional<GymTenant> findByDbName(String dbName);`
    - `boolean existsByGymCode(String gymCode);`
    - `boolean existsByDbName(String dbName);`
  - `UserTenantRoleRepository`:
    - `List<UserTenantRole> findByUserAccountId(Long userAccountId);`
    - `List<UserTenantRole> findByGymTenantId(Long gymTenantId);`
    - `Optional<UserTenantRole> findByUserAccountIdAndGymTenantId(Long userAccountId, Long gymTenantId);`
    - `Optional<UserTenantRole> findByUserAccountIdAndGymTenantIdAndRole(Long userAccountId, Long gymTenantId, TenantRole role);`
    - `boolean existsByUserAccountIdAndGymTenantIdAndRole(Long userAccountId, Long gymTenantId, TenantRole role);`
  - `SaasSubscriptionPlanRepository`:
    - `List<SaasSubscriptionPlan> findByActiveTrue();`
  - `GymSubscriptionRepository`:
    - `Optional<GymSubscription> findTopByGymTenantIdAndStatusOrderByEndDateDesc(Long gymTenantId, GymSubscriptionStatus status);`
    - `List<GymSubscription> findByGymTenantIdOrderBySubscribedAtDesc(Long gymTenantId);`
  - `AdminAuditLogRepository`:
    - `List<AdminAuditLog> findByGymTenantIdOrderByTimestampDesc(Long gymTenantId);`

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/java/com/company/GymVerse/models/central/enums/*.java` | Create | 8 enum classes defining central lifecycle, status, authority, and audit constants. |
| `src/main/java/com/company/GymVerse/models/central/*.java` | Create | 6 JPA entity classes mapped to central tables with `BigDecimal` monetary fields. |
| `src/main/java/com/company/GymVerse/repositories/central/*.java` | Create | 6 Spring Data JPA repositories with required query methods. |

---

## Technical Implementation Guidance

1. **Lombok & JPA Best Practices**:
   - Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` on entities.
   - Avoid `@Data` or `@EqualsAndHashCode` on entities containing `@ManyToOne` relationships to avoid lazy loading recursion or hash code instability across persistence states.
   - For `equals` and `hashCode`, implement based strictly on the entity ID or business key.
2. **Transaction Demarcation**:
   - Central repositories are bound to `centralTransactionManager`.
   - Write operations using central repositories should be annotated with `@Transactional("centralTransactionManager")` (or `@Transactional` since `centralTransactionManager` is `@Primary`).
3. **Monetary Precision Standard**:
   ```java
   @Column(name = "price", precision = 10, scale = 2, nullable = false)
   private BigDecimal price;
   ```

---

## Security & Multitenancy Considerations

1. **Isolation of Credentials**: `UserAccount` holds the BCrypt hashed password. Tenant models must never touch or duplicate this field.
2. **Authority Integrity**: Primary administrator authority is represented by `AdminAuthority.PRIMARY`. Tenant administration services will enforce that each gym tenant possesses at most one active `PRIMARY` administrator.
3. **No Cross-Database Foreign Keys**: Entities in `com.company.GymVerse.models.central` have zero references to classes in `com.company.GymVerse.models.tenant`.

---

## Verification & Testing Plan

### Compilation Check
```powershell
./mvnw clean test-compile
```

### Integration Test Execution (Once Story 1.7 is in place)
```powershell
./mvnw test -Dtest=CentralPersistenceIntegrationTest -Dspring.profiles.active=test
```

---

## Definition of Done (DoD)

- [x] All 8 enums exist in `com.company.GymVerse.models.central.enums`.
- [x] All 6 entities exist in `com.company.GymVerse.models.central` and match `V1__init_central_schema.sql` column definitions.
- [x] Monetary fields are strictly typed as `BigDecimal` with precision 10 and scale 2.
- [x] All 6 repositories exist in `com.company.GymVerse.repositories.central` with required query methods.
- [x] Central JPA configuration scans and loads entities and repositories without error.
- [x] Zero modifications to the 8 protected multitenancy classes.
