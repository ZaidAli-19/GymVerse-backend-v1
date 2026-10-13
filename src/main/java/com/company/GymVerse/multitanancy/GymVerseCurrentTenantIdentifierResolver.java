package com.company.GymVerse.multitanancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the current tenant identifier for Hibernate.
 *
 * <p>
 * Hibernate calls this resolver whenever it needs to determine which
 * tenant (database/schema) should be used for the current operation.
 * </p>
 *
 * <p>
 * The tenant value is retrieved from {@link TenantContext}, which stores
 * the tenant identifier in a thread-local variable for the duration of
 * a request.
 * </p>
 *
 * <p>
 * If no tenant is available, the application falls back to the
 * {@value #CENTRAL_TENANT} tenant. This is typically the shared or
 * master database that contains global application data.
 * </p>
 *
 * <p>
 * This class also registers itself with Hibernate through
 * {@link HibernatePropertiesCustomizer}, allowing Spring Boot to
 * automatically configure the tenant resolver.
 * </p>
 */
@Component
@Slf4j
public class GymVerseCurrentTenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<String>,
        HibernatePropertiesCustomizer {

    /**
     * Default tenant used when no tenant has been selected.
     *
     * <p>
     * Typically represents the central/master database that stores
     * shared application data such as tenant registrations,
     * subscriptions, configuration, etc.
     * </p>
     */
    public static final String CENTRAL_TENANT = "central";

    /**
     * Determines the tenant identifier that Hibernate should use
     * for the current database operation.
     *
     * <p>
     * Hibernate invokes this method whenever it needs to obtain
     * a connection for executing queries or transactions.
     * </p>
     *
     * @return the current tenant identifier, or
     *         {@value #CENTRAL_TENANT} if none is set
     */
    @Override
    public String resolveCurrentTenantIdentifier() {

        // Retrieve the tenant stored in the current thread context.
        String tenant = TenantContext.getCurrentTenant();

        // If a valid tenant exists, use it.
        // Otherwise, fall back to the central tenant.
        return (tenant != null && !tenant.isBlank())
                ? tenant
                : CENTRAL_TENANT;
    }

    /**
     * Indicates whether Hibernate should validate existing sessions
     * when the tenant identifier changes.
     *
     * <p>
     * Returning {@code false} means Hibernate will not verify that
     * the currently active session belongs to the same tenant.
     * </p>
     *
     * <p>
     * This is a common setting in database-per-tenant architectures
     * where tenant context is established before any session is created.
     * </p>
     *
     * @return {@code false}
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * Registers this tenant resolver with Hibernate.
     *
     * <p>
     * Spring Boot automatically invokes this method during application
     * startup. The resolver is then added to Hibernate's configuration
     * so Hibernate knows how to determine the current tenant.
     * </p>
     *
     * @param hibernateProperties Hibernate configuration properties
     */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {

        // Startup log to verify that the resolver has been registered.
        log.info("[Multitenancy] Registering TenantIdentifierResolver");

        // Tell Hibernate to use this class whenever it needs
        // the current tenant identifier.
        hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                this
        );
    }
}