# Universal AI Coding & Engineering Guide (`PROMPT.md`)

> **Role & Purpose:** This document is the master engineering directive for AI coding assistants and software engineers working across any software repository. It establishes universal rules of engagement, architectural patterns, code quality standards, layer boundaries, and API documentation requirements to ensure all AI-generated code is robust, clean, maintainable, and indistinguishable from expert human engineering.

---

## 1. Prime Directives for AI Coding Agents

Every AI assistant operating in this codebase must strictly adhere to the following core tenets:

1. **Analyze Before Writing ("When in Rome, Do as the Romans Do")**:
   - Never generate code based on generic assumptions.
   - First inspect the directory structure, build files, and representative source files in the target module.
   - Mirror existing naming standards, indentation, file organization, error handling, and component structure.

2. **Strictly Follow the Modular Folder Structure**:
   - All code must strictly reside in its designated layer and module package as defined in Section 2.
   - Never dump files into the root package or mix controllers, services, and DTOs in a single directory.

3. **Respect Established Architectural Paradigms**:
   - If the codebase uses a specific architectural style (e.g., Factory-based routing, CQRS, Hexagonal/Ports-and-Adapters, or traditional layered MVC), work strictly within that pattern.
   - Never introduce competing patterns (e.g., do not add ad-hoc controller endpoints if the project uses a centralized dispatcher/factory).

4. **Minimal, Surgical Changes**:
   - Keep diffs minimal and focused exclusively on the user's objective.
   - Do not perform unsolicited refactoring, reformat unrelated files, or change coding style in legacy files.
   - Preserve existing comments, docstrings, and license headers.

5. **Zero-Trust Security & Secrets Policy**:
   - Never hardcode passwords, API keys, private tokens, webhook secrets, or credentials in source code or documentation.
   - Always reference external configuration, environment variables, or secure secret vaults.

6. **Definition of Done Requires Verification**:
   - Never claim code compiles, runs, or passes tests unless you have actively executed the appropriate build or test command and verified exit code 0.

7. **Mandatory Task Logging in `/antigravity/`**:
   - For every task assigned, always create a new dedicated markdown file inside `<project_root>/antigravity/` with a descriptive, task-justifying name (e.g., `<task-descriptive-name>.md` such as `environment-profiles-and-pipeline-setup.md`).
   - Never use generic file names like `agent-work.md`.
   - Log everything done, all technical decisions, and the reasons behind them in simple, human-understandable English.

---

## 2. Standardized Directory Structure & Layer Responsibilities

### 2.1 Prescribed Modular Directory Layout

All features and components in the repository must be organized strictly according to the **Modular / Feature-Packaged Architecture** (modeled after ConnectSouq's enterprise layout). High-level business domains are encapsulated into cohesive feature modules, while core data models, repositories, security, and infrastructure concerns remain centralized:

```
<project_root>/
├── antigravity/                          # Task-specific work logs and engineering records
│   └── <task-descriptive-name>.md        # Record of tasks, decisions, rationale & verification
├── docs/                                 # Project & API documentation
│   ├── payloads/
│   │   └── manual-test.md (or payloads.md)# API contract reference, payloads, curl commands
│   ├── Architecture.md                   # System architecture, topology & technical flows
│   ├── project-context.md                # Domain context, stakeholder roles & specs
│   └── onboarding-scenarios.md           # Onboarding workflows & business rules
│
├── src/main/java/<company_package>/      # Application source root (or src/ for TS/Python/Go)
│   │
│   ├── <feature_module>/                 # FEATURE / DOMAIN MODULE (e.g., memberOperations, invitations, auth)
│   │   ├── controller/                   # Module entry point (Unified Dispatcher / REST Controller)
│   │   ├── factory/                      # Factory router component & Operation interface contract
│   │   ├── service/                      # Single-responsibility operation services & domain handlers
│   │   ├── request/                      # Request DTOs, command models & module-specific enums
│   │   ├── response/                     # Response DTOs & view models
│   │   ├── validations/                  # Domain validations, permission checks & invariant guards
│   │   └── mapper/                       # Entity <-> DTO mappers (MapStruct or custom)
│   │
│   ├── model/ (or entities/)             # CENTRALIZED DOMAIN ENTITIES (JPA entities, table schemas)
│   ├── repositories/                     # CENTRALIZED DATA ACCESS (Spring Data JPA repositories, locks)
│   ├── security/                         # SECURITY & AUTH (JWT utils, filters, OAuth handlers, token parsing)
│   ├── config/                           # APPLICATION CONFIGURATION (SecurityConfig, RedisConfig, KafkaConfig)
│   ├── exceptions/                       # EXCEPTION HIERARCHY (Domain exceptions, GlobalExceptionHandler)
│   ├── util/                             # SHARED UTILITIES (Cross-cutting enums, date helpers, pagination)
│   ├── kafka/ (or messaging/events/)     # ASYNCHRONOUS MESSAGING (Event models, producers, topic listeners)
│   └── schedulers/ (or cron/jobs/)       # BACKGROUND SCHEDULERS (Cron jobs, expiry monitors, sweepers)
│
├── src/main/resources/                   # Application configurations, migrations, static assets
│   ├── application.yaml                  # Environment configuration & external service profiles
│   └── db/                               # Database migration scripts (Flyway/Liquibase/SQL scripts)
│
├── src/test/java/<company_package>/      # Test suite mirroring main source structure
├── pom.xml (or package.json / go.mod)   # Build & dependency management descriptor
└── PROMPT.md                             # Universal AI Engineering Guide (This document)
```

### 2.2 Component Placement & Classification Guide

When introducing any new capability, class, or artifact, place it strictly according to this classification:

| Component Type | Prescribed Directory / Package | Description & Rules |
| :--- | :--- | :--- |
| **Controller** | `<feature_module>/controller/` | Entry point for HTTP requests. Never query repositories or perform business logic directly here. |
| **Factory Router** | `<feature_module>/factory/` | Centralized router resolving specific operation handlers from request discriminator enums. |
| **Operation Interface** | `<feature_module>/factory/` | Defines the contract (e.g. `execute(Request) -> Response`, `getRequestType()`) for operation beans. |
| **Operation Service** | `<feature_module>/service/` | Single-responsibility service executing one specific domain action/command. Annotated with `@Service`. |
| **Domain / Helper Service** | `<feature_module>/service/` | Multi-method domain services handling domain-specific business operations. |
| **Request DTO** | `<feature_module>/request/` | Input transfer object containing validation annotations (`@NotNull`, `@Size`) and request enums. |
| **Response DTO** | `<feature_module>/response/` | Output transfer object returned to client. Structured, null-safe, and immutable. |
| **Validation Component** | `<feature_module>/validations/` | Domain invariant checks, state prerequisites, and caller permission guards (`@Component`). |
| **Mapper** | `<feature_module>/mapper/` | Pure transformation between JPA domain entities and Request/Response DTOs. |
| **Domain Entity** | `model/` (or `entities/`) | Centralized JPA entities representing database tables, relationships, and lifecycle hooks. |
| **Repository** | `repositories/` | Centralized data access interfaces extending `JpaRepository` with custom queries and locking methods. |
| **Security Filters & JWT** | `security/` | Authentication filters, token validators, HttpOnly cookie helpers, and user context resolvers. |
| **Configuration Beans** | `config/` | Spring `@Configuration` beans: CORS, Redis cache manager, Kafka topics, ThreadPools, Security. |
| **Custom Exceptions** | `exceptions/` | Domain-specific runtime exceptions (`BadRequestException`, `ResourceNotFoundException`). |
| **Global Exception Handler** | `exceptions/` | `@RestControllerAdvice` mapping domain exceptions to standardized `ErrorResponse` schemas. |
| **Shared Enums & Utils** | `util/` | Domain-wide status enums, hierarchy builders, date utilities, and calculation helpers. |
| **Events & Messaging** | `kafka/` (or `messaging/`) | Event payload records, Kafka templates, publishers, and topic `@KafkaListener` consumers. |
| **Scheduled Tasks** | `schedulers/` | Periodic `@Scheduled` cron jobs (token expiry, intention timeouts, subscription synchronization). |
| **API Documentation** | `docs/payloads/` or `docs/` | Contract documentation with cURL commands, payloads, and response examples (`manual-test.md` / `payloads.md`). |

### 2.3 Feature Organization Rules
1. **Feature Encapsulation (Package-by-Feature)**:
   - High-level functional modules (e.g. `memberOperations`, `invitations`, `chats`, `businessPartner`) are self-contained packages.
   - Do not create a monolithic top-level `controllers/` folder containing all controllers for the entire application.
2. **Centralized Foundation (Package-by-Layer for Cross-Cutting Concerns)**:
   - Shared data models (`model/`), repositories (`repositories/`), security (`security/`), and configuration (`config/`) remain centralized.
   - This prevents duplicate entity definitions and circular package dependencies across interrelated modules.
3. **No Cross-Module Implementation Leakage**:
   - A feature module must never directly instantiate or depend on internal private helper classes of another feature module.
   - Cross-domain interactions must occur through published domain services, shared interfaces, or asynchronous domain events.

### 2.4 Universal Layer Responsibilities & Architectural Boundaries

In any enterprise or production codebase, code must be divided into clean, decoupled layers with strict one-way dependency flow:

```
[ Ingress / Client Request ]
             │
             ▼
┌───────────────────────────────────────────────┐
│ 1. INGRESS & SECURITY LAYER                   │  Authentication filters, CORS, rate limiters, token parsers
└────────────────────┬──────────────────────────┘
                     │ Validated SecurityContext
                     ▼
┌───────────────────────────────────────────────┐
│ 2. CONTROLLER / API GATEWAY LAYER             │  HTTP serialization, route mapping, header validation
└────────────────────┬──────────────────────────┘
                     │ Request DTO
                     ▼
┌───────────────────────────────────────────────┐
│ 3. ROUTING / DISPATCHER LAYER (If Applicable) │  Factory router, Command bus, Strategy resolver
└────────────────────┬──────────────────────────┘
                     │ Dispatches to specific handler
                     ▼
┌───────────────────────────────────────────────┐
│ 4. SERVICE / BUSINESS LOGIC LAYER             │  Domain operations, transaction boundaries, orchestration
│    ├── Validation Component                   │  Domain invariants, permissions, cross-entity checks
│    ├── Persistence / Repository Component     │  Database queries, locking, CRUD operations
│    ├── Object Mapping Component               │  Entity <-> Response DTO transformation
│    └── Asynchronous Event / Messaging Bus     │  Event publication (Kafka, RabbitMQ, Redis, WebSockets)
└────────────────────┬──────────────────────────┘
                     │ Response DTO
                     ▼
[ Egress / Client Response ]
```

### Layer Rules Matrix

| Layer | Primary Responsibilities | Allowed Dependencies | Forbidden Actions |
| :--- | :--- | :--- | :--- |
| **Ingress & Security** | Authenticate identity, validate token/session, set context | Framework security, token utilities | Executing business logic, updating database |
| **Controller** | Route incoming requests, parse payloads, return HTTP status | Router / Factory or Service interfaces | Database queries, business logic, transactions |
| **Router / Factory** | Resolve handler based on request type or command | Registered operation services | Data manipulation, direct persistence |
| **Service (Logic)** | Orchestrate business workflows, enforce domain rules | Repositories, Validations, Mappers, Event bus | Direct HTTP request/response dependencies |
| **Validation** | Verify caller permissions, ownership, state prerequisites | Repositories, security context | Mutating database entities |
| **Repository** | Data access, queries, concurrency locks, persistence | Data models / Entities, ORM framework | Business logic, direct controller interaction |
| **Model / Entity** | Represent domain data, table schema, entity relationships | Language & ORM annotations | Calling services or repositories |
| **Mapper** | Convert between storage entities and external DTOs | Entity classes, DTO classes | Business logic, database queries |

---

## 3. Controller & Routing Architecture: The Unified Module Factory Pattern

### 3.1 Sole & Mandatory Architecture: The Unified Module Factory Standard
This codebase strictly standardizes on the **Unified Module-Level Factory Pattern** (modeled after ConnectSouq's `franchiseOperator` enterprise standard). Traditional multi-endpoint RESTful resource controllers (separate `@GetMapping`, `@PutMapping`, `@DeleteMapping`, or multiple `@PostMapping` endpoints) and generic CQRS mediator frameworks are **strictly prohibited** for business modules.

Every business module follows these non-negotiable rules:

#### A. One Factory per Module
- Each business module (e.g. `platformAdmin`, `franchiseOperator`) has a single, module-level factory component: `<Module>Factory` (e.g. `PlatformAdminFactory`, `FranchiseOperatorFactory`).
- The factory maintains an internal dispatch map mapping the module's request type enum (`<Module>RequestType`) to operation handler beans (`<Module>FactoryOperation`).
- Registered cleanly via `@PostConstruct init()`:
  ```java
  @Component
  @RequiredArgsConstructor
  public class PlatformAdminFactory {
      private final List<PlatformAdminFactoryOperation> operations;
      private final Map<PlatformAdminRequestType, PlatformAdminFactoryOperation> operationMap = new HashMap<>();

      @PostConstruct
      void init() {
          operations.forEach(op -> operationMap.put(op.getRequestType(), op));
      }

      public PlatformAdminFactoryOperation getOperation(PlatformAdminRequestType requestType) {
          return operationMap.get(requestType);
      }
  }
  ```

#### B. Module Factory Operation Contract
- The module defines a dedicated operation interface: `<Module>FactoryOperation` (e.g. `PlatformAdminFactoryOperation`, `FranchiseOperatorFactoryOperation`).
- Strongly typed to the module's shared request DTO and shared response DTO (no generic `<T, R>` type parameters, no raw object casts):
  ```java
  public interface PlatformAdminFactoryOperation {
      PlatformAdminRequestType getRequestType();
      PlatformAdminResponse execute(PlatformAdminRequest request);
  }
  ```

#### C. Shared Request & Response DTOs
- **One Shared Request DTO per Module**: `<Module>Request` (e.g. `PlatformAdminRequest`, `FranchiseOperatorRequest`).
  - Contains the operation discriminator enum (`<Module>RequestType`).
  - Encapsulates operation attributes directly or via nested DTOs when complex payloads require distinct structures.
- **One Shared Response DTO per Module**: `<Module>Response` (e.g. `PlatformAdminResponse`, `FranchiseOperatorResponse`).
  - Structured, null-safe, annotated with `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Getter`, `@Setter`, and `@JsonInclude(JsonInclude.Include.NON_NULL)`.
  - Contains standard response fields such as `message`, entity IDs, and nested DTOs representing specific domain results.
- **Nested DTOs for Complex Structures**:
  - When specific operations involve complex, hierarchical, or distinct data structures, define dedicated nested DTOs within or alongside the shared request/response (e.g. `SaasPlanResponse`, `SpeakerInput`, `AgendaInput`).
  - Re-use the shared module DTO across services rather than generating one-off request/response DTO pairs for every trivial action.

#### D. Dedicated Operation Services
- Each business operation resides in its own single-responsibility service bean implementing `<Module>FactoryOperation`.
- Responsible for transaction boundaries (`@Transactional`), business logic, domain validation delegation (`<Module>Validations`), entity persistence, and mapping.

#### E. Controller Ingress (Strictly ONE Gateway Endpoint per Module)
- Every feature module controller exposes **STRICTLY ONE unified command gateway endpoint**: `@PostMapping("/<module-route>")` (e.g. `@PostMapping("/platform-admin")` or `@PostMapping("/franchise-operator")`).
- **All operations without exception**—both write commands (`CREATE_...`, `UPDATE_...`, `DELETE_...`) and read queries (`FETCH_ALL_...`, `FETCH_BY_ID_...`, `SEARCH_...`)—must be dispatched exclusively through this single `POST` endpoint using the `<Module>RequestType` discriminator enum.
- **NEVER** introduce additional endpoints (e.g., no `@GetMapping`, `@PutMapping`, `@DeleteMapping`, or extra `@PostMapping` sub-paths like `/plans` or `/plans/{id}`). All module interaction occurs through the single unified gateway endpoint.
- Controllers inject only the single module factory and dispatch incoming requests:
  ```java
  @RestController
  @RequiredArgsConstructor
  public class PlatformAdminController {
      private final PlatformAdminFactory factory;

      @PostMapping("/platform-admin")
      public PlatformAdminResponse execute(@RequestBody PlatformAdminRequest request) {
          PlatformAdminFactoryOperation operation = factory.getOperation(request.getPlatformAdminRequestType());
          if (operation == null) {
              throw new BadRequestException("Unsupported platform admin request type: " + request.getPlatformAdminRequestType());
          }
          return operation.execute(request);
      }
  }
  ```

#### F. Handling Queries / Read Operations in the Unified Factory
- Read and query operations (e.g., fetching lists, retrieving entities by ID, loading dashboards) are first-class operations implementing `<Module>FactoryOperation`.
- The client sends a `POST` request to the module gateway endpoint with the query discriminator (e.g. `PlatformAdminRequestType.FETCH_ALL_SAAS_PLANS`, `FETCH_SAAS_PLAN_BY_ID`, `FETCH_TENANT_BY_ID`) and any query criteria (such as `id` or pagination parameters) in the shared `<Module>Request`.
- The corresponding query service (`FetchAllSaasPlansService`, `FetchSaasPlanByIdService`, directly mirroring ConnectSouq's `FetchFranchiseCommissionEntriesService`, `FetchPendingApplicationsService`, and `FetchFranchiseOperatorDashboardService`) executes the query, validates permissions via `<Module>Validations`, maps entities to nested response DTOs or lists, and returns the shared `<Module>Response`.
- Do not create separate HTTP `GET` endpoints for queries. All queries flow through the unified module gateway.

### 3.2 Strictly Prohibited Alternative Paradigms
- **Traditional Multi-Endpoint REST Controllers (Prohibited)**: Do NOT create resource-oriented controllers with multiple HTTP verbs (`GET /items`, `POST /items`, `GET /items/{id}`, `PUT /items/{id}`, `DELETE /items/{id}`).
- **Generic CQRS / MediatR Frameworks (Prohibited)**: Do NOT introduce external command buses or CQRS mediator abstractions.
- **Operation-Specific Factories / Handler Registries (Prohibited)**: Do NOT create separate factories per operation (e.g. `CreatePlanFactory`) or generic handler registries with `<T, R>` type parameters and unchecked type casts.

---

## 4. Request & Response Conventions

### 1. Request DTO Standards
- **Naming**: `<Operation>Request` or `<Module>Request`.
- **Field Conventions**: Strict camelCase matching API serialization rules.
- **Identifier Types**: Use 64-bit integers (`Long` / `int64`) or UUID strings. Avoid primitive integers where `null` distinguishes absence from zero.
- **Monetary & Decimal Precision**: Always use arbitrary-precision decimals (`BigDecimal` / `Decimal` / string representations) for financial amounts and quantities. Never use floating-point types (`float`, `double`).
- **Dates & Times**: Always use UTC ISO-8601 timestamps (`Instant` / `2026-10-12T16:30:00Z`).
- **Validation**: Combine framework validation annotations (`@NotNull`, `@Valid`, `@Size`, etc.) with explicit domain invariant checks in validation components.

### 2. Response DTO Standards
- **Naming**: `<Operation>Response` or `<Module>Response`.
- **Structure**: Include a human-readable confirmation message, resulting entity IDs, and domain payload objects.
- **Null Safety**: Omit null fields or use empty collections (`[]`) rather than null arrays for list queries.

### 3. Uniform Error Response Schema
Every API must return a predictable, standardized error structure when operations fail:

```json
{
  "timestamp": "2026-10-12T16:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed explanation of the validation failure or invariant violation",
  "path": "/api/v1/resource"
}
```

### 4. HTTP Status Code Conventions
- `200 OK`: Successful command or query execution returning a payload.
- `201 Created`: Successful resource creation (when following RESTful standards).
- `204 No Content`: Successful operation returning no body.
- `400 Bad Request`: Validation failure, missing required field, or illegal business state.
- `401 Unauthorized`: Authentication missing, expired, or invalid token.
- `403 Forbidden`: Authenticated user lacks sufficient permissions or role for this action.
- `404 Not Found`: Target resource does not exist.
- `409 Conflict`: Unique constraint violation, duplicate key, or concurrent state collision.
- `500 Internal Server Error`: Unhandled runtime exceptions (must be caught and logged).

### 5. Pagination & Sorting Conventions
- Request parameters: `page` (0-indexed default), `size` (default 10 or 20), `sortBy`, `sortDirection` (`asc`/`desc`).
- Response wrapper:
  ```json
  {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 45,
    "totalPages": 5,
    "last": false
  }
  ```

---

## 5. Persistence, Concurrency & Transactions

1. **Transaction Boundaries**:
   - Apply transactional boundaries (`@Transactional` / transaction blocks) at the Service / Use Case layer, never on controller methods.
   - For read-only operations, specify read-only transaction modes where supported to optimize database performance.

2. **Concurrency & Race Condition Prevention**:
   - **Pessimistic Locking**: When modifying shared counters, financial balances, seats, or state transitions subject to high concurrency, use pessimistic row locks (`SELECT ... FOR UPDATE` / `@Lock(LockModeType.PESSIMISTIC_WRITE)`).
   - **Optimistic Locking**: Use version fields (`@Version`) for distributed resources where collisions are infrequent.
   - **Atomic In-Database Mutations**: Prefer conditional atomic update queries (e.g. `UPDATE resource SET owner = :user WHERE id = :id AND owner IS NULL`) that check affected row counts to guarantee single-winner execution.

3. **Data Integrity & Schema Constraints**:
   - Rely on database-level unique constraints and indexes to backstop application validations.
   - Foreign keys must be indexed to prevent table scans during relationship lookups.

---

## 6. Security & Context Resolution

1. **Never Trust Client-Supplied Caller IDs**:
   - For authenticated operations, extract the current user ID strictly from the authenticated security context / session token, never from an untrusted JSON body or query parameter.

2. **Stateless Authentication Flow**:
   - Extract tokens from standard `Authorization: Bearer <TOKEN>` headers or secure HttpOnly cookies.
   - Ensure refresh tokens cannot be used to invoke standard business APIs.

3. **Role-Based & Tier-Based Access Control**:
   - Enforce role gates (`ADMIN`, `OPERATOR`, `MEMBER`, etc.) at the security filter or service layer.
   - Implement feature and tier gating in dedicated helper classes to avoid polluting core business logic.

---

## 7. Asynchronous Messaging, Events & Integrations

1. **Idempotency in Webhooks & Consumers**:
   - Message consumers and webhook handlers must be strictly idempotent. Verify incoming message IDs or transaction references before executing state changes to handle duplicate deliveries gracefully.

2. **Webhook Verification**:
   - Always verify cryptographic signatures (e.g. `Stripe-Signature`, HMAC headers) using the raw request body before parsing JSON.

3. **Asynchronous Non-Blocking Event Dispatch**:
   - Publish domain events (Kafka, RabbitMQ, SQS, Redis) after successful database transactions.
   - Attach non-blocking error callbacks to log failures without bringing down the primary transaction if the event is non-critical.

---

## 8. Code Style & Engineering Habits

1. **Clean Object Construction**:
   - Favor Constructor Injection for all dependencies. Make dependency fields `private final` (or `readonly`).
   - Use Builder patterns or designated factory methods for complex entity and DTO construction.

2. **Defensive Coding & Early Returns**:
   - Place guard clauses at the beginning of methods to validate preconditions immediately.
   - Avoid deep arrow-shaped nesting (`if { if { if { ... } } }`).

3. **Clear Error Propagation**:
   - Throw specialized domain exceptions rather than returning error flags or generic exceptions.
   - Allow centralized exception handlers to map domain exceptions to appropriate HTTP responses.

4. **Contextual Logging**:
   - Log business milestones at `INFO` level with relevant identifiers (e.g., `User 101 performed Action on Entity 55`).
   - Log unhandled errors at `ERROR` level with the full stack trace and request path.
   - Never log passwords, tokens, full credit card numbers, or personally identifiable information (PII).

---

## 9. Concrete Implementation Examples

### 9.1 Module Controller Example

```java
// Controller: Strictly ONE single unified gateway endpoint per module dispatching all commands & queries via module factory
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class OrderController {

    private final OrderFactory factory;

    @PostMapping
    public OrderResponse execute(@RequestBody OrderRequest request) {
        log.info("Processing order operation: {}", request.getOrderRequestType());
        OrderFactoryOperation operation = factory.getOperation(request.getOrderRequestType());
        if (operation == null) {
            throw new BadRequestException("Unsupported order request type: " + request.getOrderRequestType());
        }
        return operation.execute(request);
    }
}
```

### 9.2 Module Factory Example

```java
// Factory: Single module factory dispatching to operation handler beans
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFactory {

    private final List<OrderFactoryOperation> operations;
    private final Map<OrderRequestType, OrderFactoryOperation> operationMap = new HashMap<>();

    @PostConstruct
    void init() {
        operations.forEach(op -> operationMap.put(op.getRequestType(), op));
    }

    public OrderFactoryOperation getOperation(OrderRequestType requestType) {
        return operationMap.get(requestType);
    }
}
```

### 9.3 Operation Service Implementation Example

```java
// Service: Executes a single business operation implementing the module factory operation contract
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderService implements OrderFactoryOperation {

    private final OrderRepository orderRepository;
    private final OrderValidations validations;
    private final OrderMapper mapper;

    @Override
    public OrderRequestType getRequestType() {
        return OrderRequestType.CREATE_ORDER;
    }

    @Override
    @Transactional
    public OrderResponse execute(OrderRequest request) {
        // 1. Resolve caller from secure context
        User caller = validations.getAuthenticatedUser();

        // 2. Validate domain invariants
        validations.validateCanCreateOrder(caller, request);

        // 3. Build and persist entity
        Order order = Order.builder()
                .customer(caller)
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency())
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        orderRepository.save(order);

        log.info("User {} created Order {}", caller.getId(), order.getId());

        // 4. Return shared module response with nested detail if needed
        return OrderResponse.builder()
                .message("Order created successfully")
                .orderId(order.getId())
                .order(mapper.toDetailResponse(order))
                .build();
    }
}
```

---

## 10. Mandatory API Documentation in `/docs/payloads.md` (or `docs/payloads/manual-test.md`)

Whenever a new service that exposes an API, a new API endpoint, or an externally accessible operation is created or modified during development:

**You must update the API documentation in:**  
`/docs/payloads.md` (or the project's designated payloads file, e.g. `docs/payloads/manual-test.md`)

This file serves as the centralized, definitive contract reference for all API consumers, QA engineers, and automated tests.

### 10.1 Required 13 Fields for Every API Entry

For every documented endpoint, include the following 13 items wherever applicable:

1. **Feature / Module**: The domain or module name.
2. **HTTP Method**: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`.
3. **Endpoint URL**: Complete route including base path (e.g. `http://localhost:8080/api/orders`).
4. **Purpose**: Plain-language description of what the endpoint accomplishes.
5. **Authentication & Authorization**: Required security level, Bearer tokens, roles, or public access.
6. **Request Headers**: Expected headers (e.g. `Content-Type: application/json`, `Authorization: Bearer <TOKEN>`).
7. **Path Parameters**: Names, data types, and constraints for path variables.
8. **Query Parameters**: Names, defaults, and usage for query parameters.
9. **Request Payload**: Accurate JSON schema with sample data, field names, and validation rules.
10. **Success Response**: HTTP status code (`200 OK`, `201 Created`) and complete JSON response body.
11. **Error Responses**: Anticipated error status codes (`400`, `401`, `403`, `404`, `409`) with sample error bodies.
12. **Implementation References**: Names of the Controller, Factory, Service, and DTO classes.
13. **Business Rules**: Constraints, state transitions, side-effects, or concurrency locks triggered.

### 10.2 Maintenance Rules for Payloads Documentation
- **Preserve Existing Documentation**: Never overwrite or delete unrelated endpoint sections. Append or update only the target operation.
- **Copy-Pasteable cURL Commands**: Every endpoint must include a complete `curl` command using standard headers and base URL placeholders.
- **Realistic Sample Data**: Use clear, representative business data. Never include live credentials, API keys, or private tokens.
- **Explicit Bodyless Declarations**: If an endpoint takes no request body (such as a GET query), explicitly document `"Request Body: None"`.

---

## 11. The 21 Golden Rules for AI Code Generation

Every AI assistant working in this repository must comply with these 21 mandatory rules:

1. **Read `PROMPT.md` First**: Treat this document as the supreme coding specification for the project.
2. **Strictly Follow the Prescribed Modular Folder Structure**: Place new components precisely in `<feature_module>/{controller, factory, service, request, response, validations, mapper}` and centralized layers (`model/`, `repositories/`, `security/`, `config/`, `exceptions/`, `util/`). Never dump classes into an ad-hoc or global folder.
3. **Adhere to the Module-Level Factory Architecture & Single Controller Gateway (MANDATORY & ABSOLUTE)**: Follow the ConnectSouq `franchiseOperator` reference standard. Each business module has one factory (`<Module>Factory`), a dedicated operation interface (`<Module>FactoryOperation`), and shared request/response DTOs (`<Module>Request`, `<Module>Response`). The module controller must contain **strictly ONE unified gateway endpoint**: `@PostMapping("/<module-route>")`. All module actions—both write commands and read queries (e.g. `FETCH_ALL`, `FETCH_BY_ID`)—must be dispatched exclusively through this single `POST` endpoint using the `<Module>RequestType` discriminator enum. Strictly no separate RESTful endpoints, no HTTP GET endpoints, no operation-specific factories, and no generic `<T, R>` handler registries.
4. **Inspect Sibling Code**: Always inspect the nearest existing implementation before creating a new class or service.
5. **Shared Module Request/Response DTOs & Nested DTOs**: Use the module's shared `<Module>Request` and `<Module>Response` across operations. Use strongly typed nested DTOs (e.g. `SaasPlanResponse`, `SpeakerInput`) when complex distinct payloads require them. Never use generic payload wrappers (`Payload<T>`) or generic operation handlers with unchecked casts.
6. **Strict Constructor Injection**: Inject dependencies via constructors (`@RequiredArgsConstructor` / `readonly`). Never use field injection.
7. **Keep Dependencies `private final`**: Ensure all injected service and repository fields are immutable.
8. **Pair Interfaces with Implementations**: Follow the project's interface-to-implementation conventions for operations, mappers, and validations.
9. **Enforce Concurrency & Locking**: Guard critical shared state and seat allocations with pessimistic row locks or atomic database updates.
10. **Never Trust Client-Sent Caller IDs**: Always extract the acting user from the authenticated security context.
11. **Validate Preconditions Early**: Place guard clauses and validations at the top of services and throw specific domain exceptions.
12. **Maintain Consistent Entity Models**: Use 64-bit integer or UUID keys, UTC timestamps (`Instant`), and arbitrary-precision decimals (`BigDecimal`) for currency.
13. **Do Not Introduce Competing Libraries**: Never import a new JSON serializer, mapping framework, or utility library if one already exists in the project.
14. **Centralize Database Access**: Place queries and persistence logic strictly in designated repository interfaces.
15. **Keep Webhooks & Event Consumers Idempotent**: Guard external integrations and message consumers against duplicate execution.
16. **Avoid Unrelated Refactoring**: Focus exclusively on the requested task. Do not reformat or clean up unrelated code.
17. **Synchronize API Documentation**: Treat updating `/docs/payloads.md` (or `docs/payloads/manual-test.md`) as a mandatory part of the Definition of Done for any API change.
18. **Write Tests Following Existing Style**: Add unit or integration tests that match the project's existing testing framework and assertion style.
19. **Verify Before Declaring Complete**: Never claim a task is complete without verifying that code compiles and satisfies the user's requirements.
20. **Zero Secrets in Code or Logs**: Never expose API keys, database credentials, or auth tokens in source files, examples, or logs.
21. **Log Every Task with a Descriptive Name in `/antigravity/`**: For each assigned task, always create a new markdown file inside `/antigravity/` named after the specific work performed (e.g., `<task-descriptive-name>.md`, never generic like `agent-work.md`). Record all changes, technical rationale, and verification steps in clear, human-understandable English.

---

## 12. Project Profile & Auto-Discovery Blueprint

When applying this prompt to a new or existing repository, the AI assistant should auto-detect and populate (or respect) the following project profile:

```yaml
project_profile:
  project_name: "<Project Name>"
  primary_language: "<Java 21 / TypeScript / Python 3.12 / Go / C#>"
  framework: "<Spring Boot / NestJS / FastAPI / Gin / ASP.NET Core>"
  architecture_pattern: "<Factory-Based Routing / Layered REST / CQRS / Clean Architecture>"
  directory_structure: "<Modular Feature-Packaged (docs/, <module>/{controller,factory,service,request,response,validations,mapper}, model/, repositories/, security/)>"
  server:
    port: "<8080 / 3000 / 8000>"
    context_path: "<e.g., /api/v1 or /cs-network or />"
  persistence:
    database: "<PostgreSQL / MySQL / MongoDB>"
    orm_data_layer: "<Spring Data JPA / Prisma / TypeORM / SQLAlchemy / GORM / EF Core>"
  messaging:
    event_bus: "<Kafka / RabbitMQ / Redis Streams / SQS / None>"
    realtime: "<WebSocket STOMP / Socket.io / Server-Sent Events / None>"
  caching_session:
    store: "<Redis / Memcached / In-Memory / None>"
  documentation:
    api_payloads_file: "docs/payloads/manual-test.md (or /docs/payloads.md)"
```
