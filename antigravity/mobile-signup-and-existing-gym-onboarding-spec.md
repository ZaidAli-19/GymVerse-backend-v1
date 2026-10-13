# Architectural Task Report: Mobile Signup and Existing-Gym Onboarding Specification

**Date:** October 13, 2026  
**Status:** COMPLETE & INTEGRATED  
**Author:** Winston, System Architect 🏗️  
**Target Document:** `docs/architecture.md` (Revision v2.2)  
**Project:** `GymVerse-Backend-v2`  

---

## 1. Executive Summary & Objective

The engineering requirements for GymVerse were updated with explicit specifications governing **Mobile Signup**, **Existing-Gym Onboarding**, and **Client Boundary Enforcement**.

The objective of this architectural update was to:
1. Integrate the mobile application's dual **Sign Up** and **Login** flows into the target architecture without modifying unrelated system decisions.
2. Formalize the invariant that central registration creates a central identity (`user_accounts`) **only**, strictly prohibiting automatic tenant creation, activation, tenant admin appointment, or immediate gym access.
3. Detail the existing-gym onboarding workflows (invitations and join requests), ensuring gym access requires explicit administrative authorization/approval and mandatory token exchange.
4. Codify the physical and functional boundary separating the **Desktop Client (`.exe`)** (gym registration, activation, and tenant provisioning) from the **Mobile Application** (signup, login, gym discovery, invitations, join requests, operational features).
5. Ensure the backend enforces all permissions, role boundaries, and provisioning guards **server-side**, completely independent of the client interface or client platform headers.
6. Reaffirm that the mobile application does not offer tenant activation or in-app purchases.

---

## 2. Technical Clarifications & Architectural Decisions

### 2.1 Mobile Signup Flow & Identity-Only Invariant
- **Flow Definition:**
  1. A new user initiates signup via email (`POST [USER_SIGNUP_INITIATE_ENDPOINT]`).
  2. The backend generates and validates an OTP via Redis and asynchronous Kafka email delivery (`POST [USER_SIGNUP_VERIFY_ENDPOINT]`).
  3. The user completes registration with password and profile attributes (`POST [USER_SIGNUP_COMPLETE_ENDPOINT]`), creating exactly one record in `user_accounts`.
  4. The backend returns a Central Access Token (`CENTRAL_USER`) and Central Refresh Token.
  5. The user can subsequently log in and access gyms for which they possess an authorized, active relationship in `user_tenant_roles`.
- **Identity-Only Invariant:** Central signup grants **zero implicit tenant access**. A newly registered account is strictly a root platform identity. It does not auto-provision physical tenant databases, does not assign the user as a `TENANT_ADMIN`, and does not connect the user to any gym until an explicit onboarding workflow occurs.

### 2.2 Existing-Gym Onboarding & Controlled Access
- **Pathways:**
  - **Self-Join Discovery:** User discovers an active gym via its unique `gymCode` and submits a `join_requests` entry (status `PENDING`). An active Tenant Administrator (Primary or Delegated) must review and approve the request before local profile creation and central `user_tenant_roles` assignment.
  - **Invitation Acceptance:** A Tenant Administrator issues an invitation token. The user accepts via `ACCEPT_INVITATION_ENDPOINT` with their central token, linking the account.
- **Mandatory Central-to-Tenant Token Exchange:**
  - Possession of a `CENTRAL_USER` token is insufficient to invoke any tenant-operational endpoint (e.g. check-in, workouts, coaching, desk overrides).
  - The user must call `POST [EXCHANGE_TOKEN_ENDPOINT]` with `targetGymId`.
  - The server verifies that the caller has an `ACTIVE` role in `user_tenant_roles`, that the gym tenant is `ACTIVE`, and that the SaaS subscription is in `ACTIVE` or `TRIAL` state.
  - Only then does the backend issue a signed `TENANT` access token containing `tenantId`, local profile `uid`, tenant `role`, and `adminAuthority`.

### 2.3 Client Boundary & Server-Side Security Independence
- **Desktop Client (`.exe`):**
  - Exclusively designated for facility onboarding: Gym Registration, Tenant Activation, SaaS subscription selection/billing, and physical MySQL database provisioning (`CREATE DATABASE` and Flyway migrations).
- **Mobile Application:**
  - Dedicated to mobility and floor engagement: Central Signup, Central Login, Gym Discovery (by code), Join Request submission, Invitation acceptance, Member GPS check-in/out, social workouts, streaks, trainer coaching dashboards, and staff check-in overrides.
  - **Zero In-App Purchases:** Tenant activation and SaaS billing are omitted from the mobile application. This avoids third-party mobile store digital payment fees and operational friction.
- **Client-Agnostic Backend Enforcement:**
  - The Spring Boot backend exposes a unified, zero-trust security architecture.
  - Endpoints verify caller tokens and database-backed authorities server-side.
  - Client platform headers (e.g., `X-Client-Platform`) cannot be forged to bypass tenant provisioning or role checks.

---

## 3. Scope of Modifications to `docs/architecture.md`

All updates have been integrated directly into `docs/architecture.md` (Revision v2.2):

| Section | Nature of Architectural Update |
| :--- | :--- |
| **Header & Status** | Bumped specification status to Revision v2.2 (Mobile Signup, Client Boundaries & Existing-Gym Onboarding). |
| **Section 1.1 (Invariants)** | Added Invariant 4 (Central Identity Only on Signup), Invariant 7 (Client Boundary & Operational Segmentation), Invariant 8 (Server-Side Client-Agnostic Permission Enforcement), and Invariant 9 (Controlled Existing-Gym Onboarding). |
| **Section 2 (System Topology)** | Updated Mermaid topology diagram with explicit `DesktopClient` and `MobileClient` nodes entering the security filter chain. Added Section 2.1 detailing client operational boundaries. |
| **Section 3.1 & 3.4 (Identity Model)** | Added Section 3.1.1 (Mobile Signup & Central Identity Creation Flow), Section 3.1.2 (Existing-Gym Onboarding & Access Prerequisites), and Section 3.4 (Client Functional Boundaries & Backend Governance Matrix). |
| **Section 5.1 & 5.4 (Token Architecture)** | Clarified token scopes for Central vs Tenant tokens; added Section 5.4 outlining server-side authority verification independent of client headers. |
| **Section 6.1 (Workflows: Signup)** | Annotated sequence diagram for Mobile/Central registration with step-by-step invariant documentation. |
| **Section 6.2 (Workflows: Gym Registration)** | Formally bounded gym registration and database provisioning to the Desktop Client (`.exe`). |
| **Section 6.5 (Workflows: Onboarding)** | Added new end-to-end Mermaid sequence diagram depicting Join Requests, Invitation Acceptance, and Mandatory Token Exchange. |
| **Section 7.1 (Permission Matrix)** | Added client capability and platform permission matrix comparing Desktop `.exe`, Mobile, and server enforcement rules. |
| **Section 9 (ADRs)** | Added **ADR-6: Mobile Signup, Existing-Gym Onboarding, and Client Boundary Enforcement**. |
| **Section 11 (Phases)** | Updated Phase 2, Phase 3, and Phase 5 descriptions to incorporate mobile signup, desktop boundary, and existing-gym onboarding gates. |

---

## 4. Verification & Integrity Checklist

- [x] **Zero Application Code Modified:** No Java code, controller beans, or SQL files were generated or modified.
- [x] **Unrelated Decisions Preserved:** Modular monolith structure, database-per-tenant isolation (`gym_db_<id>`), central `user_accounts` identity, primary/delegated admin hierarchy, and two-tier JWT token exchange were preserved verbatim.
- [x] **Documentation Consistency:** All cross-references, mermaid diagrams, and table contracts in `docs/architecture.md` remain valid and mutually consistent.
- [x] **PROMPT.md Compliance:** Documented task rationale and technical decisions under `antigravity/` per Rule 7 and Rule 21.
