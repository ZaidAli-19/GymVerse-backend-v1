# Architecture Lockdown: Unified Module Factory & Single Controller Gateway Standard

## Objective
Permanently standardize the engineering guidelines across `PROMPT.md` and `RULES.md` to guarantee that all AI agents and engineers strictly produce the exact same module-level architecture, modeled after ConnectSouq's `franchiseOperator` reference standard.

---

## Changes Made

### 1. `RULES.md` Update
- **Unified Single-Endpoint Controller Gateway (Mandatory & Absolute)**:
  - Formally codified that every feature module controller exposes **strictly ONE single command gateway endpoint**: `@PostMapping("/<module-route>")` (e.g. `@PostMapping("/platform-admin")`).
  - Required that all operations—both write commands (`CREATE`, `UPDATE`, `DELETE`) and read queries (`FETCH_ALL`, `FETCH_BY_ID`, `SEARCH`)—must route through this single endpoint using the `<Module>RequestType` discriminator enum.
  - Strictly forbade additional HTTP endpoints (`GET`, `PUT`, `DELETE`) or sub-paths (`POST /plans`, etc.).
- **Module-Level Factory Standard**:
  - One factory per module (`<Module>Factory`).
  - One non-generic operation interface (`<Module>FactoryOperation`).
  - Shared request/response DTOs (`<Module>Request`, `<Module>Response`) with nested DTOs.
  - Ban on operation-specific factories, generic `<T, R>` registries, and unchecked casts.

### 2. `PROMPT.md` Update
- **Sole & Mandatory Architecture in Section 3**:
  - Eliminated conflicting residual paradigms (former Paradigm B: Resource-Oriented RESTful Controllers, and former Paradigm C: CQRS / MediatR).
  - Explicitly established the Unified Module-Level Factory Pattern as the sole, non-negotiable architecture standard across the backend.
- **Single Controller Gateway Specification in Section 3.1.E**:
  - Documented that each feature module controller has exactly one `@PostMapping` endpoint.
- **Factory Query & Read Dispatching Specification in Section 3.1.F**:
  - Clarified how read operations (fetching lists, finding entities by ID, dashboards) are implemented as first-class operations implementing `<Module>FactoryOperation` and invoked via `POST` with query discriminator enums (`FETCH_ALL_SAAS_PLANS`, etc.), directly mirroring ConnectSouq's `FetchFranchiseCommissionEntriesService` and `FetchPendingApplicationsService`.
- **Prohibited Patterns in Section 3.2**:
  - Documented that multi-endpoint RESTful controllers and generic CQRS mediator frameworks are strictly prohibited.
- **Section 9.1 & Golden Rule 3 in Section 11**:
  - Updated controller examples and Golden Rule 3 to explicitly reinforce the single gateway endpoint and unified factory dispatching for both writes and reads.

---

## Verification
- Verified build and test suite execution via `mvn test`.
- 23/23 tests pass cleanly.
