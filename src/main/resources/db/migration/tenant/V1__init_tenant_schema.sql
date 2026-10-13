-- =============================================================================
-- GymVerse Backend v2 - Tenant Database Migration
-- Version: V1
-- Description: Initialize Tenant Schema (users, gym_config, members,
--              membership_plans, member_memberships, membership_requests,
--              attendance, revenue, expenses, join_requests, gym_invitations,
--              friend_requests, custom_friends, friend_streaks)
-- Security: ZERO passwords stored. SHA-256 hashed tokens for invitations.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Table: users
-- Purpose: Operational profile of users within this gym tenant.
-- Invariant: Zero password or credential fields. Central identity referenced
--            via scalar central_user_id.
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    central_user_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_u_central_user_id UNIQUE (central_user_id),
    CONSTRAINT uq_u_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Table: gym_config
-- Purpose: Geofencing center, radius, operating hours, and timezone.
-- -----------------------------------------------------------------------------
CREATE TABLE gym_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    radius DOUBLE NOT NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    auto_checkout_time TIME NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: members
-- Purpose: Member profile linked 1:1 with users, tracking fitness stats & badges.
-- -----------------------------------------------------------------------------
CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    trainer_id BIGINT NULL,
    independent BOOLEAN NOT NULL DEFAULT FALSE,
    height DECIMAL(5,2) NULL,
    weight DECIMAL(5,2) NULL,
    goal VARCHAR(255) NULL,
    join_date DATE NOT NULL,
    gym_id VARCHAR(50) NULL,
    type VARCHAR(50) NULL,
    trial_end_date DATE NULL,
    specialization VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_m_user_id UNIQUE (user_id),
    CONSTRAINT uq_m_gym_id UNIQUE (gym_id),
    CONSTRAINT fk_m_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_m_trainer FOREIGN KEY (trainer_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Table: membership_plans
-- Purpose: Pricing packages and tier durations offered by this gym.
-- -----------------------------------------------------------------------------
CREATE TABLE membership_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    tier VARCHAR(50) NULL,
    facility VARCHAR(100) NULL,
    base_price DECIMAL(10,2) NOT NULL,
    discount_type VARCHAR(30) NULL,
    discount_value DECIMAL(10,2) NULL,
    final_price DECIMAL(10,2) NOT NULL,
    duration_days INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: member_memberships
-- Purpose: Active and historical membership subscriptions purchased by members.
-- -----------------------------------------------------------------------------
CREATE TABLE member_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    price_at_purchase DECIMAL(10,2) NOT NULL,
    discount_type VARCHAR(30) NULL,
    discount_value DECIMAL(10,2) NULL,
    final_price DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_mm_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_mm_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE RESTRICT,
    INDEX idx_mm_member_active (member_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Table: membership_requests
-- Purpose: Requests submitted by members to change or purchase membership plans.
-- -----------------------------------------------------------------------------
CREATE TABLE membership_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_date DATETIME(6) NOT NULL,
    member_notes TEXT NULL,
    admin_notes TEXT NULL,
    reviewed_at DATETIME(6) NULL,
    reviewed_by_admin_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Table: attendance
-- Purpose: Attendance logs tracking member check-ins and check-outs via geofencing.
-- -----------------------------------------------------------------------------
CREATE TABLE attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in_time DATETIME(6) NOT NULL,
    check_out_time DATETIME(6) NULL,
    auto_checked_out BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_att_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    INDEX idx_att_member_date (member_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Table: revenue
-- Purpose: Payment intake records linked 1:1 to purchased memberships.
-- -----------------------------------------------------------------------------
CREATE TABLE revenue (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_membership_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100) NULL,
    received_at DATETIME(6) NOT NULL,
    recorded_by_admin_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_rev_membership UNIQUE (member_membership_id),
    CONSTRAINT fk_rev_membership FOREIGN KEY (member_membership_id) REFERENCES member_memberships (id) ON DELETE RESTRICT,
    CONSTRAINT fk_rev_admin FOREIGN KEY (recorded_by_admin_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. Table: expenses
-- Purpose: Operating expenditures recorded by gym administrators.
-- -----------------------------------------------------------------------------
CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    expense_date DATE NOT NULL,
    recorded_by_admin_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_exp_admin FOREIGN KEY (recorded_by_admin_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_exp_date (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 10. Table: join_requests
-- Purpose: Applications submitted by mobile/web users seeking to join this gym.
-- -----------------------------------------------------------------------------
CREATE TABLE join_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    central_user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,
    phone_number VARCHAR(30) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_notes TEXT NULL,
    admin_notes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6) NULL,
    reviewed_by_admin_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_jr_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_jr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 11. Table: gym_invitations
-- Purpose: Direct invitations issued by admins for staff, trainers, or members.
-- Invariant: Plaintext tokens are NEVER stored; stores SHA-256 hash.
-- -----------------------------------------------------------------------------
CREATE TABLE gym_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    invitation_token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    created_by_admin_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_gi_token_hash UNIQUE (invitation_token_hash),
    CONSTRAINT fk_gi_admin FOREIGN KEY (created_by_admin_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_gi_email_status (email, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 12. Table: friend_requests
-- Purpose: Social connections between members within this gym.
-- -----------------------------------------------------------------------------
CREATE TABLE friend_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_member_id BIGINT NOT NULL,
    receiver_member_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT fk_fr_sender FOREIGN KEY (sender_member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_fr_receiver FOREIGN KEY (receiver_member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT uq_fr_sender_receiver UNIQUE (sender_member_id, receiver_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 13. Table: custom_friends
-- Purpose: Confirmed friend pairs for workout tracking and attendance sharing.
-- -----------------------------------------------------------------------------
CREATE TABLE custom_friends (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_member_id BIGINT NOT NULL,
    friend_member_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cf_user FOREIGN KEY (user_member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_cf_friend FOREIGN KEY (friend_member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT uq_cf_pair UNIQUE (user_member_id, friend_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 14. Table: friend_streaks
-- Purpose: Gym attendance streaks and joint checkout counts between friends.
-- -----------------------------------------------------------------------------
CREATE TABLE friend_streaks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member1_id BIGINT NOT NULL,
    member2_id BIGINT NOT NULL,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_streak_date DATE NULL,
    together_check_ins INT NOT NULL DEFAULT 0,
    together_check_outs INT NOT NULL DEFAULT 0,
    last_checkout_together_date DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fs_m1 FOREIGN KEY (member1_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_fs_m2 FOREIGN KEY (member2_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT uq_fs_members UNIQUE (member1_id, member2_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
