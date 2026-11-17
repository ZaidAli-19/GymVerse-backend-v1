---
id: "story-3.2"
epic_id: "epic-phase-3-security-auth"
title: "Modernized JJWT Token Engine (JwtUtil & JwtFilter with MASTER_ACCOUNT Token Typing)"
status: "planning"
type: "jwt-token-engine"
created_at: "2026-11-17"
dependencies: ["story-3.1"]
files_to_modify:
  - "src/main/java/com/company/GymVerse/config/SecurityConfig.java"
  - "src/main/resources/application.yaml"
files_to_create:
  - "src/main/java/com/company/GymVerse/security/jwt/JwtUtil.java"
  - "src/main/java/com/company/GymVerse/security/jwt/JwtFilter.java"
  - "src/test/java/com/company/GymVerse/security/jwt/JwtUtilTest.java"
  - "src/test/java/com/company/GymVerse/security/jwt/JwtFilterIntegrationTest.java"
---

# Story 3.2: Modernized JJWT Token Engine (`JwtUtil` & `JwtFilter` with `MASTER_ACCOUNT` Token Typing)

## ⚠️ MANDATORY DIRECTIVES FOR IMPLEMENTING AGENTS
> 1. Strictly adhere to [`RULES.md`](file:///D:/projects/GymVerse-Backend-v2/RULES.md) and [`PROMPT.md`](file:///D:/projects/GymVerse-Backend-v2/PROMPT.md).
> 2. **NEVER** run `git commit` or `git push` autonomously.
> 3. Use JJWT 0.12.5 / 0.12.6 APIs (`Jwts.builder()`, `Jwts.parser().verifyWith(signingKey).build()`, `Keys.hmacShaKeyFor(...)`).
> 4. Token type for the 1st master layer must be **`MASTER_ACCOUNT`** (do NOT use `PLATFORM`).
> 5. Guard API routes: **Refresh tokens must NEVER be permitted to access protected API endpoints**.
> 6. Maintain task logging in `antigravity/story-3.2-modernized-jjwt-token-engine.md`.

---

## 1. User Story

**As an** API consumer and client application,  
**I want** a secure, standardized JWT token engine that generates and verifies `MASTER_ACCOUNT` Access and Refresh tokens with tamper-proof signatures,  
**So that** authenticated requests are securely authorized via Spring Security context while refresh tokens cannot be abused as API bearer tokens.

---

## 2. Technical Context & Requirements

1. **JwtUtil (`com.company.GymVerse.security.jwt.JwtUtil`)**:
   - Injects `@Value("${jwt.secret}")`, `@Value("${jwt.access-token-expiration}")`, `@Value("${jwt.refresh-token-expiration}")`.
   - Initializes `SecretKey` in `@PostConstruct` using `Decoders.BASE64URL.decode(secret)`.
   - **Token Generation Methods**:
     - `generateMasterAccessToken(UserAccount account)`:
       - Subject: `String.valueOf(account.getId())`.
       - Claims:
         - `tokenType`: `"MASTER_ACCOUNT"`
         - `type`: `"ACCESS"`
         - `role`: `account.isPlatformAdmin() ? "PLATFORM_ADMIN" : "USER_ACCOUNT"`
         - `email`: `account.getEmail()`
       - Expiration: `currentTime + accessTokenExpiration` (default 15 mins).
       - Signature: `signWith(signingKey, Jwts.SIG.HS256)`.
     - `generateMasterRefreshToken(UserAccount account)`:
       - Subject: `String.valueOf(account.getId())`.
       - Claims:
         - `tokenType`: `"MASTER_ACCOUNT"`
         - `type`: `"REFRESH"`
         - `role`: `account.isPlatformAdmin() ? "PLATFORM_ADMIN" : "USER_ACCOUNT"`
         - `email`: `account.getEmail()`
       - Expiration: `currentTime + refreshTokenExpiration` (default 30 days).
   - **Extraction & Verification Methods**:
     - `extractSubject(String token)`
     - `extractEmail(String token)`
     - `extractRole(String token)`
     - `extractTokenType(String token)` (`MASTER_ACCOUNT`)
     - `extractType(String token)` (`ACCESS` or `REFRESH`)
     - `isTokenExpired(String token)`
     - `isTokenValid(String token, String expectedSubject)`

2. **JwtFilter (`com.company.GymVerse.security.jwt.JwtFilter`)**:
   - Extends `OncePerRequestFilter`.
   - Extracts `Authorization` header. If missing or doesn't start with `Bearer `, passes down filter chain.
   - Extracts token payload and inspects `type`:
     - If `extractType(token).equals("REFRESH")`: throws `InsufficientAuthenticationException("Refresh token cannot be used to access APIs")`.
   - Validates `tokenType`: must be `"MASTER_ACCOUNT"` (or valid tenant token in future phases).
   - Extracts subject (`userId`), role, and builds `UsernamePasswordAuthenticationToken` with authority `ROLE_<role>`.
   - Sets authentication details and registers in `SecurityContextHolder.getContext().setAuthentication(authToken)`.
   - Properly catches `JwtException` and throws `BadCredentialsException("Invalid or malformed JWT token", e)`.

3. **SecurityConfig Integration**:
   - Register `JwtFilter` before `UsernamePasswordAuthenticationFilter.class` in `SecurityConfig.java`.

---

## 3. Verification & Acceptance Criteria

1. **Unit Tests (`JwtUtilTest.java`)**:
   - Verify Access and Refresh tokens contain required claims (`MASTER_ACCOUNT`, `ACCESS`/`REFRESH`, role, email).
   - Verify expired tokens are flagged correctly by `isTokenExpired`.
   - Verify modified/tampered tokens throw signature validation exceptions.
2. **Integration Tests (`JwtFilterIntegrationTest.java`)**:
   - Verify valid `ACCESS` token permits access to protected endpoints and sets authentication with `ROLE_PLATFORM_ADMIN` or `ROLE_USER_ACCOUNT`.
   - Verify sending a `REFRESH` token to a protected endpoint returns HTTP 401 Unauthorized (`InsufficientAuthenticationException`).
   - Verify sending an invalid or malformed bearer token returns HTTP 401 Unauthorized.
