-- =============================================================================
-- GymVerse Backend v2 - Central Database Migration
-- Version: V1
-- Description: Initialize Central Schema (user_accounts, gym_tenants,
--              user_tenant_roles, saas_subscription_plans, gym_subscriptions,
--              admin_audit_logs)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Table: user_accounts
-- Purpose: Unified central identity repository across the entire platform.
-- -----------------------------------------------------------------------------
CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_ua_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Table: gym_tenants
-- Purpose: Tenant registry mapping each gym to its discovery code and DB schema.
-- -----------------------------------------------------------------------------
CREATE TABLE gym_tenants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gym_name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500) NULL,
    gym_code VARCHAR(50) NOT NULL,
    db_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    registered_at DATETIME(6) NOT NULL,
    suspended_at DATETIME(6) NULL,
    suspension_reason TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_gt_gym_code UNIQUE (gym_code),
    CONSTRAINT uq_gt_db_name UNIQUE (db_name),
    INDEX idx_gt_gym_code (gym_code),
    INDEX idx_gt_db_name (db_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: user_tenant_roles
-- Purpose: Maps user accounts to gyms with specific roles and admin authority.
-- -----------------------------------------------------------------------------
CREATE TABLE user_tenant_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    gym_tenant_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    admin_authority VARCHAR(30) NOT NULL DEFAULT 'NONE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_utr_user_account FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_utr_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE,
    CONSTRAINT uq_utr_user_gym_role UNIQUE (user_account_id, gym_tenant_id, role),
    INDEX idx_utr_user_status (user_account_id, status),
    INDEX idx_utr_gym_role (gym_tenant_id, role, admin_authority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Table: saas_subscription_plans
-- Purpose: Platform SaaS tier pricing and feature entitlements for gyms.
-- -----------------------------------------------------------------------------
CREATE TABLE saas_subscription_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    billing_cycle VARCHAR(30) NOT NULL,
    max_members INT NOT NULL,
    max_trainers INT NOT NULL,
    analytics_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    multi_trainer_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    custom_branding_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: gym_subscriptions
-- Purpose: Active and historical SaaS subscriptions of gyms.
-- -----------------------------------------------------------------------------
CREATE TABLE gym_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gym_tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    price_paid DECIMAL(10,2) NOT NULL,
    billing_cycle VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    subscribed_at DATETIME(6) NOT NULL,
    transaction_reference VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_gs_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_gs_plan FOREIGN KEY (plan_id) REFERENCES saas_subscription_plans (id) ON DELETE RESTRICT,
    INDEX idx_gs_tenant_status (gym_tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Table: admin_audit_logs
-- Purpose: Immutable record of administrative delegation, transfer, and revocation.
-- -----------------------------------------------------------------------------
CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gym_tenant_id BIGINT NOT NULL,
    actor_user_account_id BIGINT NOT NULL,
    target_user_account_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    metadata TEXT NULL,
    timestamp DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_aal_gym_tenant FOREIGN KEY (gym_tenant_id) REFERENCES gym_tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_aal_actor_user FOREIGN KEY (actor_user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_aal_target_user FOREIGN KEY (target_user_account_id) REFERENCES user_accounts (id) ON DELETE SET NULL,
    INDEX idx_aal_tenant_timestamp (gym_tenant_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
