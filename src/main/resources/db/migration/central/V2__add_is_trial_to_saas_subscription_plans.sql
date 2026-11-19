-- -----------------------------------------------------------------------------
-- Migration: V2__add_is_trial_to_saas_subscription_plans.sql
-- Purpose: Add is_trial flag to saas_subscription_plans to designate trial tiers
-- -----------------------------------------------------------------------------
ALTER TABLE saas_subscription_plans
    ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_ssp_trial_active ON saas_subscription_plans (is_trial, active);
