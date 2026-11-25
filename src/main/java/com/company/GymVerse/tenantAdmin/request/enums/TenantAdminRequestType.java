package com.company.GymVerse.tenantAdmin.request.enums;

/**
 * Discriminator enum defining operations supported by the unified Tenant Admin gateway.
 */
public enum TenantAdminRequestType {
    FETCH_TENANT_SUBSCRIPTION,
    FETCH_PLANS_SUMMARY,
    FETCH_PLAN_FILTER_OPTIONS,
    FETCH_PLANS,
    CREATE_PLAN,
    UPDATE_PLAN_AVAILABILITY
}
