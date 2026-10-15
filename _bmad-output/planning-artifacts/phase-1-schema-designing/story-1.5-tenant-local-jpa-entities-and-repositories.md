---
id: "story-1.5"
epic_id: "epic-1"
title: "Tenant-Local JPA Entities and Repositories"
status: "implemented"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies:
  - "story-1.4"
files_to_modify: []
files_to_create:
  - "src/main/java/com/company/GymVerse/models/tenant/TenantUser.java"
  - "src/main/java/com/company/GymVerse/models/tenant/GymConfig.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Member.java"
  - "src/main/java/com/company/GymVerse/models/tenant/MembershipPlan.java"
  - "src/main/java/com/company/GymVerse/models/tenant/MemberMembership.java"
  - "src/main/java/com/company/GymVerse/models/tenant/MembershipRequest.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Attendance.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Revenue.java"
  - "src/main/java/com/company/GymVerse/models/tenant/Expense.java"
  - "src/main/java/com/company/GymVerse/models/tenant/JoinRequest.java"
  - "src/main/java/com/company/GymVerse/models/tenant/GymInvitation.java"
  - "src/main/java/com/company/GymVerse/models/tenant/FriendRequest.java"
  - "src/main/java/com/company/GymVerse/models/tenant/CustomFriend.java"
  - "src/main/java/com/company/GymVerse/models/tenant/FriendStreak.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/TenantUserRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/GymConfigRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MembershipPlanRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberMembershipRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MembershipRequestRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/AttendanceRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/RevenueRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/ExpenseRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/JoinRequestRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/GymInvitationRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/FriendRequestRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/CustomFriendRepository.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/FriendStreakRepository.java"
---

# Story 1.5: Tenant-Local JPA Entities and Repositories

## User Story

**As a** backend engineer,  
**I want** strongly typed JPA entities and Spring Data repositories for the tenant operational domain,  
**So that** gym-specific business modules can perform type-safe operations against members, plans, attendance, financials, and invitations within dynamically routed tenant databases under the management of `tenantEntityManagerFactory` and `tenantTransactionManager`.

---

## Business & Technical Context

In the GymVerse modular monolith, operational features for each individual gym (e.g., check-in processing, membership sales, expense logging, invitation management) execute exclusively within the tenant's isolated database.

In [`TenantJpaConfig.java`](file:///D:/projects/GymVerse-Backend-v2/src/main/java/com/company/GymVerse/multitanancy/TenantJpaConfig.java):
- Base package scanned for entities: `com.company.GymVerse.models.tenant`
- Base package scanned for repositories: `com.company.GymVerse.repositories.tenant`
- Binds `GymVerseMultiTenantConnectionProvider` and `GymVerseCurrentTenantIdentifierResolver`
- Entity manager factory bean: `tenantEntityManagerFactory`
- Transaction manager bean: `tenantTransactionManager`

All tenant repositories must extend `JpaRepository<Entity, Long>` and be managed by `tenantTransactionManager`.

### Core Architectural Invariants
1. **Zero Passwords in `TenantUser`**: The tenant user entity maps to the local `users` table and contains scalar `centralUserId`, `email`, `firstName`, `lastName`, `phoneNumber`, `role`, and `active`. It contains **no password field**.
2. **Hashed Invitation Token Lookup**: `GymInvitation` contains `invitationTokenHash` (`VARCHAR(64)`), and `GymInvitationRepository` provides lookup via `findByInvitationTokenHash(String invitationTokenHash)`.
3. **Monetary Precision**: All monetary quantities in `MembershipPlan`, `MemberMembership`, `Revenue`, and `Expense` must be represented as `BigDecimal` with `@Column(precision = 10, scale = 2)`.
4. **Decoupled Cross-Database Persistence**: Tenant entities must never contain `@ManyToOne` or `@OneToMany` relationships targeting central entities. The only cross-database link is scalar `Long centralUserId`.

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - Story 1.1 completed (profiles configured).
  - Story 1.4 completed (tenant migration script `V1__init_tenant_schema.sql` available).
- **Dependencies**:
  - Depends on Story 1.4.
  - Required by Story 1.6 (Multitenancy Bridge) and Story 1.7 (Tenant Integration Tests).

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. Creation of 14 tenant JPA entities in package `com.company.GymVerse.models.tenant`:
   - `TenantUser` (maps table `users`)
   - `GymConfig` (maps table `gym_config`)
   - `Member` (maps table `members`)
   - `MembershipPlan` (maps table `membership_plans`)
   - `MemberMembership` (maps table `member_memberships`)
   - `MembershipRequest` (maps table `membership_requests`)
   - `Attendance` (maps table `attendance`)
   - `Revenue` (maps table `revenue`)
   - `Expense` (maps table `expenses`)
   - `JoinRequest` (maps table `join_requests`)
   - `GymInvitation` (maps table `gym_invitations`)
   - `FriendRequest` (maps table `friend_requests`)
   - `CustomFriend` (maps table `custom_friends`)
   - `FriendStreak` (maps table `friend_streaks`)
2. Creation of 14 tenant Spring Data repositories in package `com.company.GymVerse.repositories.tenant`.
3. Application of `BigDecimal` for monetary fields and Java 8+ time types (`Instant`, `LocalDate`, `LocalTime`).
4. Proper mapping of local `@ManyToOne` / `@OneToOne` relationships with lazy loading.

### Out of Scope
- Modifying `TenantJpaConfig.java` or any other protected multitenancy class.
- Implementing tenant business controllers or service business logic.
- Cross-database entity associations.

---

## Detailed Acceptance Criteria

### AC-1.5.1: `TenantUser` Entity Mapping (Zero-Password Invariant)
- **Given** table `users`,
- **When** mapped by `TenantUser`,
- **Then** it declares:
  - `@Table(name = "users")`
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@Column(name = "central_user_id", unique = true, nullable = false) private Long centralUserId;`
  - `@Column(name = "first_name", nullable = false, length = 100) private String firstName;`
  - `@Column(name = "last_name", nullable = false, length = 100) private String lastName;`
  - `@Column(unique = true, nullable = false) private String email;`
  - `@Column(length = 30) private String phoneNumber;`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TenantRole role;`
  - `@Column(nullable = false) private boolean active;`
  - `@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;`
  - **Does not declare any password field or password hash property**.

### AC-1.5.2: `GymConfig` Entity Mapping
- **Given** table `gym_config`,
- **When** mapped by `GymConfig`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@Column(precision = 10, scale = 8, nullable = false) private Double latitude;`
  - `@Column(precision = 11, scale = 8, nullable = false) private Double longitude;`
  - `@Column(nullable = false) private Double radius;`
  - `@Column(nullable = false, length = 50) private String timeZone;`
  - `@Column(nullable = false) private LocalTime openingTime;`
  - `@Column(nullable = false) private LocalTime closingTime;`
  - `private LocalTime autoCheckoutTime;`

### AC-1.5.3: `Member` Entity Mapping
- **Given** table `members`,
- **When** mapped by `Member`,
- **Then** it declares:
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
  - `@OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true, nullable = false) private TenantUser user;`
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trainer_id") private TenantUser trainer;`
  - `@Column(nullable = false) private boolean independent;`
  - `@Column(precision = 5, scale = 2) private Double height;`
  - `@Column(precision = 5, scale = 2) private Double weight;`
  - `private String goal;`
  - `@Column(nullable = false) private LocalDate joinDate;`
  - `@Column(unique = true, length = 50) private String gymId;`
  - `@Column(length = 50) private String type;`
  - `private LocalDate trialEndDate;`
  - `@Column(length = 100) private String specialization;`

### AC-1.5.4: `MembershipPlan` & `MemberMembership` Entity Mapping
- **Given** tables `membership_plans` and `member_memberships`,
- **When** mapped,
- **Then**:
  - `MembershipPlan` maps `basePrice`, `discountValue`, `finalPrice` as `BigDecimal` with `@Column(precision = 10, scale = 2)`, `durationDays` as `Integer`, and `active` as `boolean`.
  - `MemberMembership` maps `member` (`@ManyToOne` LAZY), `plan` (`@ManyToOne` LAZY), `priceAtPurchase`, `discountValue`, `finalPrice` as `BigDecimal` with `@Column(precision = 10, scale = 2)`, `startDate`, `endDate`, and `active`.

### AC-1.5.5: `Attendance`, `Revenue`, & `Expense` Entity Mapping
- **Given** operational logging tables,
- **When** mapped,
- **Then**:
  - `Attendance`: `member` (`@ManyToOne` LAZY), `date` (`LocalDate`), `checkInTime` (`Instant`), `checkOutTime` (`Instant`), `autoCheckedOut` (`boolean`), `status` (`String`).
  - `Revenue`: `memberMembership` (`@OneToOne` LAZY), `amount` (`BigDecimal(10,2)` NOT NULL), `paymentMode` (`String`), `transactionReference` (`String`), `receivedAt` (`Instant`), `recordedByAdmin` (`@ManyToOne` LAZY `TenantUser`).
  - `Expense`: `title` (`String`), `description` (`String`), `amount` (`BigDecimal(10,2)` NOT NULL), `category` (`String`), `expenseDate` (`LocalDate`), `recordedByAdmin` (`@ManyToOne` LAZY `TenantUser`).

### AC-1.5.6: `JoinRequest` & `GymInvitation` Entity Mapping (Hashed Token Invariant)
- **Given** onboarding tables,
- **When** mapped,
- **Then**:
  - `JoinRequest`: scalar `Long centralUserId`, `email`, `firstName`, `lastName`, `phoneNumber`, `status`, `requestNotes`, `adminNotes`, `createdAt`, `reviewedAt`, `reviewedByAdmin` (`@ManyToOne` LAZY `TenantUser`).
  - `GymInvitation`:
    - `email` (`String`), `role` (`TenantRole`),
    - **`@Column(name = "invitation_token_hash", unique = true, nullable = false, length = 64) private String invitationTokenHash;`**
    - `status` (`String`), `expiresAt` (`Instant`), `createdAt` (`Instant`), `acceptedAt` (`Instant`), `createdByAdmin` (`@ManyToOne` LAZY `TenantUser`).

### AC-1.5.7: Social Entities (`FriendRequest`, `CustomFriend`, `FriendStreak`)
- **Given** social tables,
- **When** mapped,
- **Then**:
  - `FriendRequest`: `senderMember` (`@ManyToOne`), `receiverMember` (`@ManyToOne`), `status` (`FriendRequestStatus` enum: `PENDING`, `ACCEPTED`, `REJECTED`, `REMOVED`).
  - `CustomFriend`: `userMember` (`@ManyToOne`), `friendMember` (`@ManyToOne`), `customName` (`String`), `createdAt` (`Instant`).
  - `FriendStreak`: `member1` (`@ManyToOne`), `member2` (`@ManyToOne`), `currentStreak`, `longestStreak`, `lastStreakDate`, `togetherCheckIns`, `togetherCheckOuts`, `lastCheckoutTogetherDate`.

### AC-1.5.8: Tenant Repository Query Methods
- **Given** package `com.company.GymVerse.repositories.tenant`,
- **When** scanned by `TenantJpaConfig`,
- **Then** repositories expose the following contracts:
  - `TenantUserRepository`:
    - `Optional<TenantUser> findByCentralUserId(Long centralUserId);`
    - `Optional<TenantUser> findByEmail(String email);`
    - `boolean existsByCentralUserId(Long centralUserId);`
    - `boolean existsByEmail(String email);`
  - `GymConfigRepository`:
    - `Optional<GymConfig> findTopByOrderByIdAsc();`
  - `MemberRepository`:
    - `Optional<Member> findByUserId(Long userId);`
    - `List<Member> findByTrainerId(Long trainerId);`
    - `Optional<Member> findByGymId(String gymId);`
  - `MembershipPlanRepository`:
    - `List<MembershipPlan> findByActiveTrue();`
  - `MemberMembershipRepository`:
    - `List<MemberMembership> findByMemberIdAndActiveTrue(Long memberId);`
    - `List<MemberMembership> findByMemberIdOrderByStartDateDesc(Long memberId);`
  - `MembershipRequestRepository`:
    - `List<MembershipRequest> findByUserId(Long userId);`
    - `List<MembershipRequest> findByStatus(String status);`
  - `AttendanceRepository`:
    - `Optional<Attendance> findByMemberIdAndDate(Long memberId, LocalDate date);`
    - `List<Attendance> findByDate(LocalDate date);`
  - `RevenueRepository`:
    - `List<Revenue> findByReceivedAtBetween(Instant start, Instant end);`
  - `ExpenseRepository`:
    - `List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);`
  - `JoinRequestRepository`:
    - `List<JoinRequest> findByCentralUserId(Long centralUserId);`
    - `List<JoinRequest> findByStatus(String status);`
  - `GymInvitationRepository`:
    - **`Optional<GymInvitation> findByInvitationTokenHash(String invitationTokenHash);`**
    - `List<GymInvitation> findByEmailAndStatus(String email, String status);`
    - `boolean existsByInvitationTokenHash(String invitationTokenHash);`
  - `FriendRequestRepository`:
    - `Optional<FriendRequest> findBySenderMemberIdAndReceiverMemberId(Long senderId, Long receiverId);`
  - `CustomFriendRepository`:
    - `List<CustomFriend> findByUserMemberId(Long userMemberId);`
  - `FriendStreakRepository`:
    - `Optional<FriendStreak> findByMember1IdAndMember2Id(Long m1, Long m2);`

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/java/com/company/GymVerse/models/tenant/*.java` | Create | 14 JPA entity classes representing operational tenant tables. |
| `src/main/java/com/company/GymVerse/repositories/tenant/*.java` | Create | 14 Spring Data JPA repositories with query contracts. |

---

## Technical Implementation Guidance

1. **Transaction Demarcation Requirement**:
   - Tenant repository methods MUST be invoked within a transaction managed by `tenantTransactionManager`.
   - Annotation: `@Transactional("tenantTransactionManager")`.
2. **Context Setup Timing**:
   - `TenantContext.setCurrentTenant(tenantDbName)` must execute **prior** to entering `@Transactional("tenantTransactionManager")`.
3. **No Cross-Database Foreign Keys**:
   - `TenantUser.centralUserId` is a scalar `Long`.
   - `JoinRequest.centralUserId` is a scalar `Long`.
   - Do NOT use `@ManyToOne private UserAccount userAccount` inside tenant entities.

---

## Security & Multitenancy Considerations

1. **Zero Credential Exposure**: `TenantUser` does not declare a password field. Passwords never exist in tenant memory or tenant databases.
2. **Invitation Security**: `GymInvitationRepository` searches by `invitationTokenHash`. The application never stores or queries raw tokens in the database.
3. **Dynamic Pool Routing**: All queries are automatically dispatched to the active tenant's HikariCP pool via `GymVerseMultiTenantConnectionProvider`.

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

- [x] All 14 tenant entities exist in `com.company.GymVerse.models.tenant`.
- [x] `TenantUser` contains scalar `centralUserId` and **zero password fields**.
- [x] `GymInvitation` contains `invitationTokenHash` (`VARCHAR(64)`).
- [x] Monetary fields use `BigDecimal` with `@Column(precision = 10, scale = 2)`.
- [x] All 14 repositories exist in `com.company.GymVerse.repositories.tenant`.
- [x] `GymInvitationRepository` provides `findByInvitationTokenHash(String tokenHash)`.
- [x] Scanned by `TenantJpaConfig` without mapping errors.
- [x] Zero modifications to the 8 protected multitenancy classes.
