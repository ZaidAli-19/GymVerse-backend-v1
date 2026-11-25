# Story 5.3 Task Log: Join Requests Review & Approval Workflow

## 1. Context & Operational Goals
- **Objective:** Implement the Join Requests tab operations inside `/api/tenant-admin` (`FETCH_JOIN_REQUESTS`, `APPROVE_JOIN_REQUEST`, `REJECT_JOIN_REQUEST`) organized under `service/members/`, `request/members/`, `response/members/`, and `specifications/members/`.
- **Architectural Invariants:**
  - Single unified `@PostMapping("/api/tenant-admin")` controller gateway via `TenantAdminRequestType`.
  - Clean dual-DB coordination between Tenant DB (`join_requests`, `users`, `members`, `member_memberships`, `revenue`) and Central DB (`user_tenant_roles`, `gym_subscriptions`, `user_accounts`) using explicit `TransactionTemplate` boundaries and tenant compensation rollback on central failure.
  - All status/type/payment fields strictly use `@Enumerated(EnumType.STRING)` enums (`JoinRequestStatus`, `PaymentStatus`, `PaymentMode`, `RevenueType`, `MemberType`).
  - All 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% untouched; zero autonomous `git commit` or `git push`.

## 2. Changes Implemented
1. **Request & Response DTOs (`request/members/`, `response/members/`)**:
   - Requests: `FetchJoinRequestsRequest`, `ApproveJoinRequest`, `RejectJoinRequest`.
   - Responses: `JoinRequestItemResponse`, `ApproveJoinRequestResponse`.
   - Updated `TenantAdminRequestType`, `TenantAdminRequest`, and `TenantAdminResponse`.
2. **Specifications, Helpers, Validations & Mapper**:
   - `JoinRequestSpecification`: Dynamic JPA criteria filtering by `JoinRequestStatus` and case-insensitive partial search across `firstName`, `lastName`, `email`, and `phoneNumber`.
   - `JoinRequestHelper`: Cross-DB central validation (`GymSubscription` status & `maxMembers` quota, `UserAccount`, duplicate active role guard), `persistCentralMemberRole(...)` (`MEMBER`, `ACTIVE`), and unique `MEM-XXXXXXXX` `gymId` generation (`MemberRepository.existsByGymId`).
   - `TenantAdminValidationsImpl`: Added `validateFetchJoinRequests`, `parseJoinRequestStatusFilter`, `validateApproveJoinRequest`, `validateJoinRequestPayment` (`PAID` vs `PARTIALLY_PAID` amount invariants), and `validateRejectJoinRequest`.
   - `TenantAdminMapper` / `TenantAdminMapperImpl`: Handwritten conversions for join request pages, items, and approval summaries.
3. **Services (`service/members/`)**:
   - `FetchJoinRequestsService` (`FETCH_JOIN_REQUESTS`)
   - `ApproveJoinRequestService` (`APPROVE_JOIN_REQUEST`)
   - `RejectJoinRequestService` (`REJECT_JOIN_REQUEST`)
4. **Tests**:
   - `JoinRequestAdminServiceTest` (5 unit tests covering `FETCH_JOIN_REQUESTS`, `APPROVE_JOIN_REQUEST` for `PAID` & `PARTIALLY_PAID`, `max_members` quota & payment guards, central failure compensation rollback, and `REJECT_JOIN_REQUEST`).
   - `JoinRequestSpecificationIntegrationTest` (MySQL 9.1 integration test verifying `JoinRequestSpecification` filtering and `REJECT_JOIN_REQUEST`).

## 3. Verification & Compliance
- **Test Execution (`BUILD SUCCESS`):** 19/19 tests passed (0 failures, 0 errors) across `JoinRequestAdminServiceTest` (5), `JoinRequestSpecificationIntegrationTest` (1), `PlansServiceTest` (3), and `TenantSubscriptionIntegrationTest` (10).
- **Protected Core Invariant:** `com.company.GymVerse.multitanancy` remains 100% untouched.
- **Git Safety:** Zero commits or pushes executed.
