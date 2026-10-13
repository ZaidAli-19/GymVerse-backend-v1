package com.company.GymVerse.multitanancy;

/**
 * Utility class for storing and retrieving the current tenant context
 * during the lifecycle of a request.
 *
 * <p>
 * This class uses {@link InheritableThreadLocal} so that child threads
 * automatically inherit the tenant information from the parent thread.
 * This is useful in a multi-tenant application where database routing
 * depends on the currently selected tenant.
 * </p>
 *
 * <p>
 * Typical flow:
 * <ul>
 *     <li>Set the tenant when a request arrives.</li>
 *     <li>Use the tenant information during processing.</li>
 *     <li>Clear the tenant after the request is completed.</li>
 * </ul>
 * </p>
 */
public final class TenantContext {

    /**
     * Stores the current tenant identifier (for example, a database name)
     * for the current thread.
     *
     * <p>
     * InheritableThreadLocal allows child threads created by the current
     * thread to inherit the same tenant value automatically.
     * </p>
     */
    private static final InheritableThreadLocal<String> CURRENT_TENANT =
            new InheritableThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>
     * This class only contains static utility methods and should never
     * be instantiated.
     * </p>
     */
    private TenantContext() {
    }

    /**
     * Sets the current tenant for the executing thread.
     *
     * @param tenantDbName the tenant identifier or database name
     *                     associated with the current request
     */
    public static void setCurrentTenant(String tenantDbName) {
        CURRENT_TENANT.set(tenantDbName);
    }

    /**
     * Retrieves the tenant associated with the current thread.
     *
     * @return the current tenant identifier, or {@code null}
     *         if no tenant has been set
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Removes the tenant information from the current thread.
     *
     * <p>
     * This should be called after request processing is complete to
     * prevent tenant data from leaking into subsequent requests when
     * thread pools are reused.
     * </p>
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}