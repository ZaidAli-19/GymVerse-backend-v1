# Phase 3: Core Security & Master DB Authentication — Dependency Matrix & Implementation Ordering

This document establishes the dependency relationships, critical path, risk analysis, and recommended execution sequence for **Phase 3: Core Security Baseline & Master DB Platform Authentication Engine** of `GymVerse-Backend-v2`.

---

## 1. Story Dependency Matrix

| Story ID | Story Title | Immediate Dependencies | Cross-Cutting Dependencies | Blocks / Enables |
| :--- | :--- | :--- | :--- | :--- |
| **Story 3.1** | **Security Baseline & UserDetailsService** | Phase 1 (`UserAccountRepository`) | `SecurityCryptoConfig`, `CentralJpaConfig`, `user_accounts` table | Stories 3.2, 3.5, 3.6 |
| **Story 3.2** | **Modernized JJWT Token Engine** | Story 3.1 | `SecurityConfig`, JJWT 0.12.x, `UserAccount` | Stories 3.5, 3.6, 3.8; All future secured API endpoints |
| **Story 3.3** | **Kafka Asynchronous Email Pipeline & JavaMailSender** | None (Independent infra) | `spring-kafka`, `spring-boot-starter-mail`, `application.yaml` | Story 3.4 (`OtpService` email dispatch) |
| **Story 3.4** | **Enterprise Redis OtpService with Rate Limiting** | Story 3.3 | `RedisConfig`, `KafkaTemplate`, `@ConfigurationProperties` | Stories 3.5, 3.6, 3.7 |
| **Story 3.5** | **Master User Signup Lifecycle** | Stories 3.1, 3.2, 3.4 | `UserAccountRepository`, `PasswordEncoder`, `AuthValidations` | Stories 3.6, 3.8; User population for the entire platform |
| **Story 3.6** | **Dual-Mode Master Authentication Engine** | Stories 3.1, 3.2, 3.4, 3.5 | `AuthenticationManager`, `JwtUtil`, `OtpService` | Stories 3.8; Platform-wide login access |
| **Story 3.7** | **Password Recovery Lifecycle** | Stories 3.1, 3.4 | `UserAccountRepository`, `OtpService`, `PasswordEncoder` | Story 3.8; Self-service account recovery |
| **Story 3.8** | **Unified Security Gateway Integration & API Docs** | Stories 3.1 through 3.7 | `AuthController`, `AuthFactory`, `GlobalExceptionHandler`, `docs/payloads.md` | Phase 3 completion gate; Enables Phase 4 (Tenant Onboarding) |

---

## 2. Dependency Graph & Critical Path

```mermaid
graph TD
    subgraph "Phase 1 Baseline (Complete)"
        P1["UserAccountRepository & Central DB"]
    end

    subgraph "Phase 3: Infrastructure Foundation"
        S31["Story 3.1: Security Baseline & UserDetailsService"]
        S32["Story 3.2: Modernized JJWT Token Engine"]
        S33["Story 3.3: Kafka Email Pipeline & MailSender"]
        S34["Story 3.4: Enterprise Redis OtpService"]
    end

    subgraph "Phase 3: Core Identity & Lifecycle Services"
        S35["Story 3.5: Master User Signup Lifecycle"]
        S36["Story 3.6: Dual-Mode Master Authentication"]
        S37["Story 3.7: Password Recovery Lifecycle"]
    end

    subgraph "Phase 3: Gateway Integration & Delivery"
        S38["Story 3.8: Unified Gateway (/api/auth) & API Docs"]
    end

    P1 --> S31
    S31 --> S32
    S33 --> S34
    S31 --> S35
    S32 --> S35
    S34 --> S35
    S35 --> S36
    S32 --> S36
    S34 --> S36
    S34 --> S37
    S31 --> S37
    S35 --> S38
    S36 --> S38
    S37 --> S38

    classDef primary fill:#2563eb,stroke:#1d4ed8,stroke-width:2px,color:#fff;
    classDef foundation fill:#059669,stroke:#047857,stroke-width:2px,color:#fff;
    classDef gateway fill:#7c3aed,stroke:#6d28d9,stroke-width:2px,color:#fff;
    class S31,S32,S33,S34 foundation;
    class S35,S36,S37 primary;
    class S38 gateway;
```

---

## 3. Implementation Sequence & Order of Execution

To guarantee zero compile breaks and progressive verification, execution must strictly follow this linear progression:

### Stage 1: Security & Messaging Infrastructure (Stories 3.1 – 3.4)
1. **Story 3.1**: Establish `SecurityConfig`, `CustomUserDetailsService`, and `AuthenticationManager`.
2. **Story 3.2**: Implement JJWT 0.12.x `JwtUtil` (`MASTER_ACCOUNT` Access & Refresh tokens) and `JwtFilter`.
3. **Story 3.3**: Configure Spring Boot Kafka auto-configuration, `OtpEmailEvent`, `OtpEmailListener`, and `EmailServiceImpl`.
4. **Story 3.4**: Build `OtpService` + `OtpServiceImpl` with Redis cooldown rate limiting (60s), attempt throttling (5 attempts), and state verification flags.

### Stage 2: Business Logic & Lifecycle Operations (Stories 3.5 – 3.7)
5. **Story 3.5**: Implement the 3-step Signup flow (`SIGNUP_INITIATE`, `SIGNUP_VERIFY`, `SIGNUP_COMPLETE`).
6. **Story 3.6**: Implement Dual Login (`LOGIN_PASSWORD`, `LOGIN_OTP_INITIATE`, `LOGIN_OTP_VERIFY`) and `REFRESH_TOKEN`.
7. **Story 3.7**: Implement Password Recovery (`FORGOT_PASSWORD_INITIATE`, `FORGOT_PASSWORD_VERIFY`, `FORGOT_PASSWORD_RESET`).

### Stage 3: Unified Gateway Integration & Documentation (Story 3.8)
8. **Story 3.8**: Assemble the single unified gateway controller `@PostMapping("/api/auth")` with `AuthFactory`, configure `GlobalExceptionHandler` security mappings, publish full contracts to `docs/payloads.md`, and execute the entire test regression suite.
