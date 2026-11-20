package com.company.GymVerse.user.request.enums;

/**
 * Discriminator enum defining all central master user operation types
 * routed through the unified UserController gateway.
 */
public enum UserRequestType {

    /**
     * Operation to register a new gym franchise/center and provision its isolated tenant database.
     */
    REGISTER_GYM,

    /**
     * Operation to fetch all gym tenants where the authenticated master user has a role association.
     */
    FETCH_MY_TENANTS,

    /**
     * Operation to discover and retrieve gym profile metadata by its gym code.
     */
    FETCH_GYM_BY_CODE,

    /**
     * Operation for a central user to submit a membership join request to a gym tenant.
     */
    SUBMIT_JOIN_REQUEST
}
