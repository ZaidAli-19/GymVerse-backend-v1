# Project & Assistant Execution Rules

## 1. Git Commit & Push Policy (STRICT & ABSOLUTE)
- **NEVER commit code on your own**: Under NO circumstances should any `git commit` command be executed autonomously.
- **NEVER push code on your own**: Under NO circumstances should any `git push` command be executed autonomously.
- **Explicit User Request Only**: Git commits and git pushes must ONLY be executed when the user explicitly commands it in chat (e.g., "commit this", "push the changes").
- **Local Working Copy Only**: When code changes are made, keep them unstaged or staged in the local working directory for review. Run verification/tests, report the results, and wait for user instructions.

## 2. Project Boundaries & Phasing
- Treat external reference repositories (such as `cs-events`) as strictly **READ-ONLY**.
- Follow the incremental brownfield migration plan established in `docs/geo-migration/`.
- Do not modify existing domain entities (`User`, `Franchise`, `BusinessPartnerApplication`, `Event`) until their designated story (Story S6).
- Preserve all existing public contracts and Kafka message formats (`MeetingAttendeeJoinedEvent` must carry string names).

## 3. Verification Standards
- Always ensure changes compile cleanly (`mvn compile`).
- Add and run unit tests for newly implemented functionality before declaring a task ready for review.

## 4. Architecture, Coding Style & Engineering Habits (from PROMPT.md)
- **Master Engineering Guide**: Adhere strictly to all directives and the 21 Golden Rules in `PROMPT.md`.
- **Modular Feature Architecture**:
  - Encapsulate feature components in `<feature_module>/{controller, factory, service, request, response, validations, mapper}`.
  - Keep cross-cutting concerns centralized in `model/`, `repositories/`, `security/`, `config/`, `exceptions/`, `util/`.
- **Factory / Dispatcher Pattern**:
  - Respect the unified factory dispatching pattern. Do not add ad-hoc controller endpoints where factories/operations exist.
- **Dependency Injection**:
  - Use constructor injection via `@RequiredArgsConstructor` with `private final` fields. No field injection (`@Autowired`).
- **Object Mapping & Validations**:
  - Use handwritten mappers (`XxxMapper` interface + `XxxMapperImpl`).
  - Keep domain validations and authorization checks separated in dedicated validation components (`XxxValidations` + `XxxValidationsImpl`).
- **Data Types & Defensive Style**:
  - `Long` / `UUID` for IDs, `Instant` for UTC timestamps, `BigDecimal` for currency/quantities.
  - Guard clauses and early returns; throw specific domain exceptions (`BadRequestException`, `ResourceNotFoundException`).
- **Task Logging in `/antigravity/`**:
  - Maintain a dedicated task log file in `<project_root>/antigravity/<task-descriptive-name>.md` recording all changes, technical decisions, rationale, and verification.
- **API Payloads Documentation**:
  - Whenever an API is added or modified, update `/docs/payloads.md` (or `docs/payloads/manual-test.md`) with the 13 required fields and copy-pasteable curl commands.

## 5. Agent Context & Session Continuity Maintenance (`antigravity/agentContext.md`) (MANDATORY)
- **Mandatory Context Updates After Every Task Execution**: Immediately upon completing any engineering task, story, subtask, or bugfix, the agent MUST update `antigravity/agentContext.md`.
- **Maintain Chat & Session Continuity ("Where We Left Off")**:
  - Keep the active state section in `antigravity/agentContext.md` updated with where the conversation and implementation left off last time.
  - Record active branch, current Git HEAD, completed tasks, tasks currently in progress, pending next steps, and any open decisions or user instructions.
  - In normal conversational interactions, ensure any significant contextual changes or milestone agreements are reflected in `agentContext.md` so future sessions resume seamlessly without context loss.
- **Audit & Story Synchronization**:
  - Keep the executed task inventory in `agentContext.md` strictly synchronized with `/docs` specifications and `/antigravity` execution logs.
