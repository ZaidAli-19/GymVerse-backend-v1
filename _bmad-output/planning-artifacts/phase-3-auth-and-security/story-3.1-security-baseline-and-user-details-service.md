---
id: "story-3.1"
epic_id: "epic-phase-3-security-auth"
title: "Security Baseline Configuration, Password Hashing & CustomUserDetailsService"
status: "implemented"
type: "security-foundation"
created_at: "2026-11-17"
dependencies: []
files_to_modify:
  - "src/main/java/com/company/GymVerse/config/SecurityConfig.java"
  - "src/main/resources/application.yaml"
  - "src/test/resources/application-test.yaml"
files_to_create:
  - "src/main/java/com/company/GymVerse/security/jwt/CustomUserDetailsService.java"
  - "src/test/java/com/company/GymVerse/security/CustomUserDetailsServiceTest.java"
  - "src/test/java/com/company/GymVerse/security/SecurityBaselineIntegrationTest.java"
---

# Story 3.1: Security Baseline Configuration, Password Hashing & CustomUserDetailsService

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Preserve all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` byte-for-byte.
> 4. Use constructor injection with `@RequiredArgsConstructor` and `private final` fields. No `@Autowired`.
> 5. Central database entities and repositories must be used under `centralTransactionManager`.
> 6. Maintain task logging in `antigravity/story-3.1-security-baseline-and-user-details-service.md`.

---

## 1. User Story

**As a** platform security architect,  
**I want** to establish the core Spring Security filter chain with stateless session management, a centralized `CustomUserDetailsService` querying `UserAccountRepository`, and an `AuthenticationManager` bean,  
**So that** user credentials can be authenticated securely against the central database with BCrypt password hashing while keeping auth endpoints accessible.

---

## 2. Technical Context & Requirements

1. **SecurityFilterChain Configuration (`SecurityConfig.java`)**:
   - Update `src/main/java/com/company/GymVerse/config/SecurityConfig.java`.
   - Disable CSRF (`AbstractHttpConfigurer::disable`).
   - Configure session creation policy to `SessionCreationPolicy.STATELESS`.
   - Permit unauthenticated access to `/api/auth` (and `/api/auth/**`).
   - Secure all other application endpoints with `.anyRequest().authenticated()`.
   - Disable standard form login and HTTP basic authentication.
   - Register `AuthenticationManager` bean via `AuthenticationConfiguration.getAuthenticationManager()`.

2. **CustomUserDetailsService (`com.company.GymVerse.security.jwt.CustomUserDetailsService`)**:
   - Implements Spring Security's `UserDetailsService`.
   - Injects `UserAccountRepository`.
   - In `loadUserByUsername(String email)`:
     - Queries `userAccountRepository.findByEmail(email)`.
     - Throws `UsernameNotFoundException("User not found with email: " + email)` if absent.
     - Validates account status: if `status != UserAccountStatus.ACTIVE` (e.g. `SUSPENDED` or `DEACTIVATED`), sets user account flags (`disabled = true` or `accountLocked = true`).
     - Maps platform authorities:
       - If `account.isPlatformAdmin() == true`: assigns `ROLE_PLATFORM_ADMIN`.
       - Else: assigns `ROLE_USER_ACCOUNT`.
     - Builds and returns `org.springframework.security.core.userdetails.User` with hashed password and granted authorities.

3. **Password Cryptography**:
   - `SecurityCryptoConfig.java` already defines `BCryptPasswordEncoder` bean; ensure it integrates smoothly into authentication checks.

---

## 3. Verification & Acceptance Criteria

1. **Unit Test (`CustomUserDetailsServiceTest.java`)**:
   - Verifies `loadUserByUsername` correctly returns `UserDetails` with email, hashed password, and proper role.
   - Verifies `UsernameNotFoundException` is thrown when email does not exist.
   - Verifies account status flags (`accountLocked`, `disabled`) reflect `UserAccountStatus`.
2. **Integration Test (`SecurityBaselineIntegrationTest.java`)**:
   - Verifies `/api/auth` is accessible anonymously (HTTP 200/400 instead of 401/403).
   - Verifies protected endpoints (such as `/api/platform-admin`) return HTTP 401 Unauthorized when requested without authentication.
   - Verifies Spring context loads cleanly without circular bean dependencies.
