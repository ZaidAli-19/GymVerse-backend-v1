---
id: "story-5.3"
epic_id: "epic-phase-5-tenant-admin"
title: "Join Requests Review & Approval Workflow"
status: "ready-for-review"
type: "gateway-operation"
created_at: "2026-11-25"
dependencies:
  - "story-5.1"
  - "story-5.2"
files_to_modify:
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/enums/TenantAdminRequestType.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/TenantAdminRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/TenantAdminResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidations.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/validations/TenantAdminValidationsImpl.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapper.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/mapper/TenantAdminMapperImpl.java"
  - "src/main/java/com/company/GymVerse/repositories/tenant/MemberRepository.java"
files_to_create:
  - "src/main/java/com/company/GymVerse/tenantAdmin/specifications/members/JoinRequestSpecification.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/members/FetchJoinRequestsRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/members/ApproveJoinRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/request/members/RejectJoinRequest.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/members/JoinRequestItemResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/response/members/ApproveJoinRequestResponse.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/FetchJoinRequestsService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/ApproveJoinRequestService.java"
  - "src/main/java/com/company/GymVerse/tenantAdmin/service/members/RejectJoinRequestService.java"
  - "src/test/java/com/company/GymVerse/tenantAdmin/service/members/JoinRequestAdminServiceTest.java"
---

# Story 5.3: Join Requests Review & Approval Workflow

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. **Dual-DB Coordination**: Approving a join request updates the **Tenant DB** (`join_requests`, `users`, `members`, `member_memberships`, `revenue`) AND creates the **Central DB** association (`user_tenant_roles` with `role = MEMBER`, `status = ACTIVE`). Never mix entity managers in a single `@Transactional`; coordinate explicitly via `TransactionTemplate` / `TenantRoutingBridge` or dedicated Central helper with safe compensation if Central insert fails.
> 4. **Subfolder Classification**: Place all classes in `service/members/`, `request/members/`, `response/members/`, `specifications/members/`.

---

## 1. User Story

**As a** Tenant Administrator reviewing incoming member applications on the Members page's Join Requests tab,  
**I want** to search and filter join requests, reject invalid requests, or approve a request by selecting a membership plan, marking payment as `PAID` or `PARTIALLY_PAID` with the entered amount,  
**So that** the applicant immediately becomes an active gym member with recorded revenue and central tenant-switch access.

---

## 2. Technical Context & Operations

### 2.1 `FETCH_JOIN_REQUESTS`
- Request (`FetchJoinRequestsRequest { status ("ALL" | "PENDING" | "APPROVED" | "REJECTED"), search, page, size, sortBy, sortDirection }`).
- Specification (`JoinRequestSpecification.withFilters(status, search)`):
  - `status`: optional (skip if null or `"ALL"`).
  - `search`: case-insensitive `LIKE` across `firstName`, `lastName`, `email`, `phoneNumber`.
- Returns `PagedResponse<JoinRequestItemResponse>`.

### 2.2 `APPROVE_JOIN_REQUEST`
- Request (`ApproveJoinRequest`):
  - `joinRequestId` (required `Long`)
  - `planId` (required `Long`)
  - `paymentStatus` (required `PaymentStatus`: `PAID` or `PARTIALLY_PAID`)
  - `paidAmount` (required `BigDecimal >= 0`)
  - `paymentMode` (required `PaymentMode`: `CASH` or `ONLINE`)
  - `transactionReference` (optional `String`)
  - `adminNotes` (optional `String`)
  - `trainerId` (optional `Long` — tenant `users.id` of trainer if assigned)
  - `independent` (optional `Boolean`, default `false`)
- Orchestration & Validations:
  1. Validate caller is `ROLE_TENANT_ADMIN` (`TenantAdminContextHelper`).
  2. **Central Checks** (via Central DB read):
     - Verify `GymSubscription` is active/trial and current tenant `members` count `< saasPlan.getMaxMembers()` (throw `ForbiddenException` if plan member limit reached).
     - Verify `UserAccount` exists in Central DB for `joinRequest.getCentralUserId()` and does not already hold an `ACTIVE` role for this `gymTenantId`.
  3. **Tenant Validations**:
     - `JoinRequest` exists and `status` equals `"PENDING"` (throw `BadRequestException` otherwise).
     - `MembershipPlan` exists and `active == true`.
     - Validate payment math:
       - `paidAmount >= 0` and `paidAmount <= plan.getFinalPrice()`.
       - If `paymentStatus == PAID`, `paidAmount` must equal `plan.getFinalPrice()` (for non-zero paid plans; for free/trial plans `paidAmount == 0` is allowed).
       - If `paymentStatus == PARTIALLY_PAID`, `paidAmount > 0` and `paidAmount < plan.getFinalPrice()`.
     - Verify `TenantUser` does not already exist for `centralUserId` or `email`.
  4. **Tenant Persistence (in `tenantTransactionManager`)**:
     - Resolve reviewing admin (`TenantUser`) via `TenantAdminContextHelper`.
     - Update `JoinRequest`: `status = "APPROVED"`, `adminNotes`, `reviewedAt = Instant.now()`, `reviewedByAdmin`.
     - Create `TenantUser` (`centralUserId`, `firstName`, `lastName`, `email`, `phoneNumber`, `role = TenantRole.MEMBER`, `active = true`).
     - Create `Member` (`user`, `trainer` if provided, `independent`, `joinDate = LocalDate.now()`, generated unique `gymId`, `type = plan.isTrial() ? "TRIAL" : "REGULAR"`, `trialEndDate = plan.isTrial() ? today.plusDays(plan.getDurationDays()) : null`).
     - Create `MemberMembership` (`member`, `plan`, `priceAtPurchase = plan.getBasePrice()`, `discountType`, `discountValue`, `finalPrice = plan.getFinalPrice()`, `paidAmount`, `paymentStatus.name()`, `isRenewal = false`, `isTrial = plan.isTrial()`, `startDate = today`, `endDate = today.plusDays(plan.getDurationDays())`, `active = true`).
     - If `paidAmount > 0`, create `Revenue` (`memberMembership`, `amount = paidAmount`, `paymentMode.name()`, `revenueType = "NEW_MEMBERSHIP"`, `transactionReference`, `receivedAt = Instant.now()`, `recordedByAdmin`, `notes = adminNotes`).
  5. **Central Persistence (in `centralTransactionManager`)**:
     - Persist `UserTenantRole` (`userAccount`, `gymTenant`, `role = TenantRole.MEMBER`, `adminAuthority = AdminAuthority.NONE`, `status = TenantMembershipStatus.ACTIVE`).
  6. Return `ApproveJoinRequestResponse` (`joinRequestId`, `memberId`, `membershipId`, `gymId`, `paymentStatus`, `paidAmount`, `finalPrice`, `remainingDue`, `startDate`, `endDate`, `status = "APPROVED"`).

### 2.3 `REJECT_JOIN_REQUEST`
- Request (`RejectJoinRequest { joinRequestId, adminNotes }`).
- Validates `JoinRequest` is `"PENDING"`.
- Updates `status = "REJECTED"`, `adminNotes`, `reviewedAt = Instant.now()`, `reviewedByAdmin`.

---

## 3. Acceptance Criteria

- [ ] `FETCH_JOIN_REQUESTS` supports pagination, sorting, status filtering, and case-insensitive search via `JoinRequestSpecification`.
- [ ] `APPROVE_JOIN_REQUEST` enforces `PAID` vs `PARTIALLY_PAID` amount invariants and `max_members` SaaS quota.
- [ ] Approving atomically creates `TenantUser`, `Member`, `MemberMembership`, `Revenue` (when `paidAmount > 0`), and Central `UserTenantRole(MEMBER, ACTIVE)`.
- [ ] `REJECT_JOIN_REQUEST` transitions `PENDING` -> `REJECTED` and prevents re-reviewing already processed requests.
- [ ] Unit tests cover happy paths (`PAID`, `PARTIALLY_PAID`, `REJECT`), quota exceeded, invalid amount, and non-pending request guards.
