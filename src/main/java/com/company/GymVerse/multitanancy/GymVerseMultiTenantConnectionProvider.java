package com.company.GymVerse.multitanancy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Hibernate Multi-Tenant Connection Provider.
 *
 * <p>
 * This class is responsible for supplying database connections
 * to Hibernate based on the currently active tenant.
 * </p>
 *
 * <p>
 * In a database-per-tenant architecture:
 * <ul>
 *     <li>Each tenant has its own database.</li>
 *     <li>Hibernate asks this provider for a connection whenever it
 *     needs to execute a query.</li>
 *     <li>The provider determines which datasource should be used.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The central database is used for shared application data such as:
 * <ul>
 *     <li>Tenant registrations</li>
 *     <li>Subscriptions</li>
 *     <li>Global configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * Tenant-specific databases are loaded dynamically through
 * {@link TenantDataSourceCache}.
 * </p>
 *
 * <p>
 * This class also registers itself with Hibernate during application
 * startup via {@link HibernatePropertiesCustomizer}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GymVerseMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String>,
        HibernatePropertiesCustomizer {

    /**
     * Cache responsible for managing tenant-specific datasource pools.
     */
    private final TenantDataSourceCache dataSourceCache;

    /**
     * Central/master datasource configured by Spring Boot.
     *
     * <p>
     * This datasource points to the application's primary database.
     * </p>
     */
    private final DataSource centralDataSource;

    /**
     * Initializes the datasource cache after bean creation.
     *
     * <p>
     * The central datasource is seeded into the cache so that
     * Hibernate can immediately resolve connections for the
     * default tenant.
     * </p>
     */
    @PostConstruct
    public void init() {

        dataSourceCache.seedCentral(
                GymVerseCurrentTenantIdentifierResolver.CENTRAL_TENANT,
                centralDataSource
        );
    }

    /**
     * Returns a generic connection when Hibernate does not yet know
     * which tenant should be used.
     *
     * <p>
     * Hibernate calls this method during startup and metadata
     * initialization.
     * </p>
     *
     * @return connection from the central datasource
     * @throws SQLException if connection creation fails
     */
    @Override
    public Connection getAnyConnection() throws SQLException {

        return centralDataSource.getConnection();
    }

    /**
     * Releases a connection obtained from {@link #getAnyConnection()}.
     *
     * @param connection connection to close
     * @throws SQLException if closing fails
     */
    @Override
    public void releaseAnyConnection(Connection connection)
            throws SQLException {

        connection.close();
    }

    /**
     * Returns a database connection for the specified tenant.
     *
     * <p>
     * Hibernate invokes this method whenever it needs a connection
     * for executing tenant-specific operations.
     * </p>
     *
     * <p>
     * Logic:
     * <ul>
     *     <li>If tenant = "central", use the central datasource.</li>
     *     <li>Otherwise load or create the tenant datasource.</li>
     * </ul>
     * </p>
     *
     * @param tenantIdentifier tenant database name
     * @return tenant-specific database connection
     * @throws SQLException if connection creation fails
     */
    @Override
    public Connection getConnection(String tenantIdentifier)
            throws SQLException {

        // Use central datasource for shared/global operations.
        if (GymVerseCurrentTenantIdentifierResolver
                .CENTRAL_TENANT
                .equals(tenantIdentifier)) {

            return centralDataSource.getConnection();
        }

        // Load tenant datasource from cache or create it if needed.
        return dataSourceCache
                .getOrCreate(tenantIdentifier)
                .getConnection();
    }

    /**
     * Releases a tenant connection after Hibernate has finished using it.
     *
     * @param tenantIdentifier tenant database name
     * @param connection connection to release
     * @throws SQLException if closing fails
     */
    @Override
    public void releaseConnection(
            String tenantIdentifier,
            Connection connection)
            throws SQLException {

        connection.close();
    }

    /**
     * Indicates whether Hibernate can aggressively release connections.
     *
     * <p>
     * Returning {@code false} ensures connections remain available
     * throughout the transaction lifecycle.
     * </p>
     *
     * @return false
     */
    @Override
    public boolean supportsAggressiveRelease() {

        return false;
    }

    /**
     * Indicates whether this provider can be unwrapped to another type.
     *
     * <p>
     * Not needed in this implementation.
     * </p>
     *
     * @param unwrapType requested type
     * @return false
     */
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {

        return false;
    }

    /**
     * Attempts to unwrap this provider into another type.
     *
     * <p>
     * Unsupported because this provider does not expose
     * any underlying implementation.
     * </p>
     *
     * @param unwrapType target type
     * @param <T> generic type
     * @return never returns
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public <T> T unwrap(Class<T> unwrapType) {

        throw new UnsupportedOperationException();
    }

    /**
     * Registers this connection provider with Hibernate.
     *
     * <p>
     * Spring Boot automatically invokes this method during startup.
     * Hibernate then uses this provider whenever it needs
     * database connections.
     * </p>
     *
     * @param hibernateProperties Hibernate configuration properties
     */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {

        log.info("[Multitenancy] Registering MultiTenantConnectionProvider");

        hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                this
        );
    }
}