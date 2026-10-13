package com.company.GymVerse.models.central.enums;

/**
 * Administrative authority level for tenant administrators.
 * Enforces the primary administrator model where only one active PRIMARY admin exists per tenant.
 */
public enum AdminAuthority {
    PRIMARY,
    DELEGATED,
    NONE
}
