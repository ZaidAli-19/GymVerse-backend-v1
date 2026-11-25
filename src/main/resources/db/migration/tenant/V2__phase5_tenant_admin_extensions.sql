-- =============================================================================
-- GymVerse Backend v2 - Tenant Database Migration
-- Version: V2
-- Description: Phase 5 Tenant Admin Schema Extensions (plan_tiers, gym_facilities,
--              membership_plan_facilities, trainers, payment & revenue tracking)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Table: plan_tiers
-- Purpose: Persisted default (ELITE, LEGENDARY) and admin-created custom tiers
--          with deterministic frontend theme keys.
-- -----------------------------------------------------------------------------
CREATE TABLE plan_tiers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    theme_key VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_pt_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO plan_tiers (name, theme_key, is_default, created_at) VALUES
    ('ELITE', 'GOLDEN', TRUE, UTC_TIMESTAMP(6)),
    ('LEGENDARY', 'PURPLE', TRUE, UTC_TIMESTAMP(6));

-- -----------------------------------------------------------------------------
-- 2. Table: gym_facilities
-- Purpose: Reusable gym facilities saved by tenant admins for plans & filtering.
-- -----------------------------------------------------------------------------
CREATE TABLE gym_facilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_gf_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: membership_plans (Alter)
-- Purpose: Link plans via plan_tier_id FK and add is_trial flag.
-- -----------------------------------------------------------------------------
ALTER TABLE membership_plans
    DROP COLUMN tier,
    DROP COLUMN facility,
    ADD COLUMN plan_tier_id BIGINT NULL AFTER description,
    ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE AFTER duration_days,
    ADD CONSTRAINT fk_mp_plan_tier FOREIGN KEY (plan_tier_id) REFERENCES plan_tiers (id) ON DELETE RESTRICT;

-- -----------------------------------------------------------------------------
-- 4. Table: membership_plan_facilities
-- Purpose: Many-to-many link between membership_plans and gym_facilities.
-- -----------------------------------------------------------------------------
CREATE TABLE membership_plan_facilities (
    plan_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    PRIMARY KEY (plan_id, facility_id),
    CONSTRAINT fk_mpf_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_mpf_facility FOREIGN KEY (facility_id) REFERENCES gym_facilities (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: member_memberships (Alter)
-- Purpose: Support PAID / PARTIALLY_PAID tracking, renewals, and trial flag.
-- -----------------------------------------------------------------------------
ALTER TABLE member_memberships
    ADD COLUMN paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER final_price,
    ADD COLUMN payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID' AFTER paid_amount,
    ADD COLUMN is_renewal BOOLEAN NOT NULL DEFAULT FALSE AFTER active,
    ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE AFTER is_renewal;

-- -----------------------------------------------------------------------------
-- 6. Table: revenue (Alter)
-- Purpose: Allow multiple/partial payments, nullable membership link for OTHER
--          revenue, revenue categorization (NEW_MEMBERSHIP, RENEWAL, OTHER),
--          and optional admin notes.
-- -----------------------------------------------------------------------------
ALTER TABLE revenue
    DROP FOREIGN KEY fk_rev_membership,
    DROP INDEX uq_rev_membership;

ALTER TABLE revenue
    MODIFY COLUMN member_membership_id BIGINT NULL,
    ADD INDEX idx_rev_membership (member_membership_id),
    ADD CONSTRAINT fk_rev_membership FOREIGN KEY (member_membership_id) REFERENCES member_memberships (id) ON DELETE RESTRICT,
    ADD COLUMN revenue_type VARCHAR(30) NOT NULL DEFAULT 'NEW_MEMBERSHIP' AFTER payment_mode,
    ADD COLUMN notes TEXT NULL AFTER transaction_reference;

-- -----------------------------------------------------------------------------
-- 7. Table: trainers
-- Purpose: 1:1 trainer operational profile linked to tenant users.
-- -----------------------------------------------------------------------------
CREATE TABLE trainers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    specialization VARCHAR(150) NULL,
    experience_years INT NULL,
    bio TEXT NULL,
    joined_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_tr_user_id UNIQUE (user_id),
    CONSTRAINT fk_tr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Table: expenses (Index)
-- Purpose: Optimize Finance weekly/monthly/yearly category breakdown queries.
-- -----------------------------------------------------------------------------
ALTER TABLE expenses
    ADD INDEX idx_exp_category_date (category, expense_date);
