# Epic 4 Context: Phase 4: Tenant Onboarding, Dynamic Database Provisioning, Scoped RBAC & Subscription Lifecycle

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Phase 4 establishes the multi-tenant operational bridge connecting central master user identity with physically isolated tenant databases. It enables master users to register gym franchises (dynamically provisioning dedicated MySQL databases and Flyway schemas), browse their multi-tenant directory, switch seamlessly into tenant operations via token exchange, and manage tenant SaaS trial subscriptions with scoped role-based access control.

## Stories

- Story 4.1: SaaS Trial Engine Configuration & Properties
- Story 4.2: Tenant-Scoped JWT Engine & Security Context Resolution
- Story 4.3: Dynamic Tenant Provisioning & Schema Migration Service
- Story 4.4R: Central User Module Gateway Architecture & Onboarding Refactoring
- Story 4.5: User Tenants Directory & Token Exchange Gateway
- Story 4.6: Subscription Insights & Entitlements Management

## Requirements & Constraints

- **Master-Scoped vs. Tenant-Scoped Invariant**: Operations executed before entering an operational gym context (gym registration, user tenants directory) belong to the Central User module (`POST /api/user`) requiring `MASTER_ACCOUNT` token. Token exchange belongs to Auth (`POST /api/auth`). In-gym operational workflows belong to Tenant Admin (`POST /api/tenant-admin`) requiring `TENANT_ACCOUNT` token and `ROLE_TENANT_ADMIN`.
- **Zero Password Invariant**: Tenant database `users` table contains zero credentials; it references the central master identity via `central_user_id`.
- **Zero-Lookup Dynamic Tenant Routing**: Tenant JWTs embed `tenantDbName`, enabling `JwtFilter` to bind `TenantContext` without central database lookups on operational requests.
- **Multi-Gym Role Mapping**: Master users can hold distinct roles (`TENANT_ADMIN`, `MEMBER`, `TRAINER`, `STAFF`) across multiple gym tenants via `user_tenant_roles`.
- **Clean Unified POST Gateways**: Exactly one unified POST gateway per module (`/api/user`, `/api/auth`, `/api/tenant-admin`).
- **Compensation Rollback**: Dynamic provisioning must automatically clean up and drop physical databases on schema or seed failure.
- **Client Neutrality**: Backend exposes uniform API contracts; platform-specific presentation is delegated to frontend UI.

## Technical Decisions

- **Core Multi-Tenancy Classes**: All 8 classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
- **Factory Pattern**: Module-level factories dispatching to concrete operation beans implementing module operation interfaces.
- **Handwritten Mappers & Validations**: Explicit mappers and validations without reflection or MapStruct.
- **Atomic Token Issuance**: Provisioning and token exchange generate fresh access/refresh token pairs with exact tenant claims.

## Cross-Story Dependencies

- Story 4.5 depends on Story 4.2 (tenant-scoped JWT engine) and Story 4.4R (central user module and tenant role mappings).
- Story 4.6 depends on Story 4.2 (tenant security context) and Story 4.5 (tenant token exchange).
