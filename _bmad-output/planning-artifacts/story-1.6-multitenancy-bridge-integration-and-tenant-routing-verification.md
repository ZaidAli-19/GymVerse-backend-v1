---
id: "story-1.6"
epic_id: "epic-1"
title: "Multitenancy Bridge Integration & Tenant Routing Verification"
status: "ready-for-dev"
type: "architecture-persistence"
created_at: "2026-10-13"
dependencies:
  - "story-1.3"
  - "story-1.5"
files_to_modify: []
files_to_create:
  - "src/main/java/com/company/GymVerse/multitanancy/TenantRoutingBridge.java"
---

# Story 1.6: Multitenancy Bridge Integration & Tenant Routing Verification

## User Story

**As a** backend engineer,  
**I want** an application-to-database routing bridge component that strictly respects Spring JPA transaction initiation boundaries and the existing multitenancy subsystem lifecycle,  
**So that** tenant-scoped database operations reliably bind to the intended tenant schema, execute isolated operations under `tenantTransactionManager`, and guarantee `TenantContext` cleanup in `finally` blocks without modifying any of the eight protected multitenancy classes.

---

## Business & Technical Context

The multitenancy subsystem in `GymVerse-Backend-v2` is governed by eight verified, protected classes in `com.company.GymVerse.multitanancy`:
1. `CentralJpaConfig` (`@Primary centralEntityManagerFactory`, `@Primary centralTransactionManager`)
2. `TenantJpaConfig` (`tenantEntityManagerFactory`, `tenantTransactionManager`)
3. `GymVerseCurrentTenantIdentifierResolver` (reads `TenantContext.getCurrentTenant()`, falls back to `"central"`)
4. `GymVerseMultiTenantConnectionProvider` (routes to `centralDataSource` or dynamic tenant pool via `TenantDataSourceCache`)
5. `TenantDataSourceCache` (manages HikariCP pools keyed by tenant database name)
6. `TenantDatabaseService` (`CREATE DATABASE` / `DROP DATABASE` via central datasource)
7. `TenantFlywayService` (programmatic migration engine for tenant databases)
8. `TenantContext` (ThreadLocal storage for active tenant database name)

### Critical Architectural Timing Rule & Severe Pitfall
In Spring Boot JPA multi-tenancy, when a method annotated with `@Transactional("tenantTransactionManager")` is invoked:
1. Spring's `JpaTransactionManager` begins the transaction **before** entering the method body.
2. Hibernate immediately queries `CurrentTenantIdentifierResolver.resolveCurrentTenantIdentifier()` to select the schema.
3. Hibernate requests a connection from `MultiTenantConnectionProvider.getConnection(tenantIdentifier)`.

> [!CAUTION]
> **Severe Pitfall — Transaction Initiation Timing**:  
> Calling `TenantContext.setCurrentTenant(tenantDbName)` *inside* a method annotated with `@Transactional("tenantTransactionManager")` is **ineffective**. The transaction manager will have already queried Hibernate before the method body executed, causing it to resolve `"central"` and bind the central datasource instead of the tenant pool.
>
> **Mandatory Invariant**: `TenantContext.setCurrentTenant(tenantDbName)` must **always** be executed **before** entering the tenant transaction boundary.

### Complete Tenant Transaction Lifecycle
```
[Caller / Business Service / Bridge]
   │
   ├─► 1. Establish Tenant Context: TenantContext.setCurrentTenant(tenantDbName)
   │
   ├─► 2. Enter Tenant Transaction Boundary: @Transactional("tenantTransactionManager")
   │      │
   │      ├─► Hibernate calls GymVerseCurrentTenantIdentifierResolver
   │      │     └─► resolves tenantDbName from TenantContext
   │      │
   │      ├─► Hibernate calls GymVerseMultiTenantConnectionProvider.getConnection(tenantDbName)
   │      │     └─► retrieves/creates pool via TenantDataSourceCache.getOrCreate(tenantDbName)
   │      │
   │      ├─► Execute Tenant JPA Repository Queries (e.g. MemberRepository, AttendanceRepository)
   │      │
   │      └─► Transaction Commits or Rolls Back; Connection released to Hikari pool
   │
   └─► 3. Cleanup Tenant Context (MANDATORY IN FINALLY BLOCK): TenantContext.clear()
```

---

## Prerequisites & Dependencies

- **Prerequisites**:
  - Story 1.1 completed (profiles configured).
  - Story 1.3 completed (Central entities and repositories available).
  - Story 1.5 completed (Tenant entities and repositories available).
- **Dependencies**:
  - Depends on Story 1.3 and Story 1.5.
  - Required by Story 1.7 (Automated Integration Test Suite).

---

## Scope & Out-of-Scope Boundaries

### In Scope
1. Creation of `TenantRoutingBridge.java` in `com.company.GymVerse.multitanancy` (or `com.company.GymVerse.multitanancy.bridge`).
2. Programmatic transaction execution helpers:
   - `void runInTenant(String tenantDbName, Runnable action)`
   - `<T> T callInTenant(String tenantDbName, Supplier<T> action)`
   - `void runInTenantTx(String tenantDbName, Consumer<TenantRoutingBridge> action)`
3. Guaranteeing that:
   - `TenantContext.setCurrentTenant(tenantDbName)` is invoked before the transaction begins.
   - Operations execute under `tenantTransactionManager`.
   - `TenantContext.clear()` is unconditionally executed in a `finally` block to prevent ThreadLocal contamination.
4. Preserving all eight protected multitenancy classes byte-for-byte.

### Out of Scope
- Modifying any of the 8 existing multitenancy classes.
- Creating HTTP interceptors, filters, or security JWT extractors (deferred to Phase 2).
- Implementing business service logic or REST endpoints.

---

## Detailed Acceptance Criteria

### AC-1.6.1: Immutable Multitenancy Baseline
- **Given** the 8 protected multitenancy classes in `com.company.GymVerse.multitanancy`,
- **When** Story 1.6 is implemented,
- **Then** all 8 classes remain 100% byte-for-byte identical to their original repository state.

### AC-1.6.2: Tenant Context Establishment Timing
- **Given** `TenantRoutingBridge`,
- **When** executing a tenant-scoped operation via `runInTenant(tenantDbName, action)` or `callInTenant(tenantDbName, action)`,
- **Then** `TenantContext.setCurrentTenant(tenantDbName)` is set **prior** to acquiring the transaction or invoking Hibernate session logic,
- **And** `GymVerseCurrentTenantIdentifierResolver` resolves `tenantDbName`.

### AC-1.6.3: Tenant Transaction Execution Boundary
- **Given** `TenantRoutingBridge`,
- **When** transactional tenant actions are performed,
- **Then** the transaction boundary is managed by `PlatformTransactionManager tenantTransactionManager`,
- **And** connection acquisition is performed by `GymVerseMultiTenantConnectionProvider` calling `TenantDataSourceCache.getOrCreate(tenantDbName)`.

### AC-1.6.4: Guaranteed ThreadLocal Cleanup
- **Given** `TenantRoutingBridge`,
- **When** the tenant action completes successfully OR throws an unchecked runtime exception,
- **Then** `TenantContext.clear()` is guaranteed to execute in a `finally` block,
- **And** subsequent executions on the same thread see `TenantContext.getCurrentTenant() == null` (falling back to `"central"`).

### AC-1.6.5: Central Fallback Verification
- **Given** an operation executing under `centralTransactionManager` without an explicit tenant context,
- **When** `GymVerseCurrentTenantIdentifierResolver.resolveCurrentTenantIdentifier()` is invoked,
- **Then** it returns `"central"`,
- **And** `GymVerseMultiTenantConnectionProvider` supplies a connection from `centralDataSource`.

---

## Affected Files

| File Path | Action | Description |
| :--- | :--- | :--- |
| `src/main/java/com/company/GymVerse/multitanancy/TenantRoutingBridge.java` | Create | Infrastructure component managing tenant context lifecycle, programmatic transaction demarcation, and cleanup. |

---

## Technical Implementation Guidance

### Design Pattern for `TenantRoutingBridge`
```java
package com.company.GymVerse.multitanancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Slf4j
@Component
public class TenantRoutingBridge {

    private final PlatformTransactionManager tenantTransactionManager;

    public TenantRoutingBridge(
            @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTransactionManager
    ) {
        this.tenantTransactionManager = tenantTransactionManager;
    }

    /**
     * Executes a supplier function within an active tenant context and a tenant transaction.
     */
    public <T> T callInTenant(String tenantDbName, Supplier<T> action) {
        if (tenantDbName == null || tenantDbName.isBlank()) {
            throw new IllegalArgumentException("Tenant database name must not be null or blank");
        }

        TenantContext.setCurrentTenant(tenantDbName);
        try {
            TransactionTemplate txTemplate = new TransactionTemplate(tenantTransactionManager);
            return txTemplate.execute(status -> action.get());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Executes a runnable action within an active tenant context and a tenant transaction.
     */
    public void runInTenant(String tenantDbName, Runnable action) {
        callInTenant(tenantDbName, () -> {
            action.run();
            return null;
        });
    }
}
```

---

## Security & Multitenancy Considerations

1. **ThreadLocal Leak Prevention**: In thread-pooled web servers (Tomcat/HikariCP), failing to clear `ThreadLocal` variables leaks tenant identity into subsequent unrelated user requests. Enforcing `finally { TenantContext.clear(); }` guarantees zero cross-tenant contamination.
2. **Preventing Accidental Central Writes**: If tenant context were not established before transaction initiation, Hibernate would fall back to `"central"`, potentially attempting to execute tenant queries on the central database or failing with missing table errors. The bridge prevents this failure mode completely.

---

## Verification & Testing Plan

### Compilation Check
```powershell
./mvnw clean test-compile
```

### Integration Test Execution (Once Story 1.7 is in place)
```powershell
./mvnw test -Dtest=TenantIsolationIntegrationTest -Dspring.profiles.active=test
```

---

## Definition of Done (DoD)

- [ ] `TenantRoutingBridge.java` is implemented in `com.company.GymVerse.multitanancy`.
- [ ] Injects `tenantTransactionManager` with `@Qualifier("tenantTransactionManager")`.
- [ ] Establishes `TenantContext.setCurrentTenant(tenantDbName)` before transaction boundary.
- [ ] Enforces unconditional `TenantContext.clear()` in `finally` blocks.
- [ ] All 8 existing multitenancy classes remain 100% byte-for-byte unchanged.
- [ ] `./mvnw clean test-compile` succeeds.
