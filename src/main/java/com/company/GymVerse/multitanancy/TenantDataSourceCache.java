package com.company.GymVerse.multitanancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages and caches tenant-specific DataSource instances.
 *
 * <p>
 * In a database-per-tenant architecture, each tenant has its own database.
 * Creating a new connection pool for every request would be expensive, so
 * this class maintains a cache of {@link HikariDataSource} instances.
 * </p>
 *
 * <p>
 * When a tenant is accessed for the first time:
 * <ul>
 *     <li>A new HikariCP connection pool is created.</li>
 *     <li>The pool is stored in memory.</li>
 *     <li>Subsequent requests reuse the existing pool.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example:
 * <pre>
 * Tenant: gym_abc  -> HikariPool-gym_abc
 * Tenant: gym_xyz  -> HikariPool-gym_xyz
 * Tenant: central  -> HikariPool-central
 * </pre>
 * </p>
 *
 * <p>
 * This approach improves performance by avoiding repeated datasource
 * creation and allowing each tenant to have its own managed connection pool.
 * </p>
 */
@Slf4j
@Component
public class TenantDataSourceCache {

    /**
     * Database username shared by all tenant databases.
     */
    @Value("${spring.datasource.username}")
    private String dbUsername;

    /**
     * Database password shared by all tenant databases.
     */
    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * MySQL host where tenant databases are located.
     *
     * Defaults to localhost if not configured.
     */
    @Value("${gymverse.datasource.host:localhost}")
    private String dbHost;

    /**
     * MySQL port where tenant databases are exposed.
     *
     * Defaults to 3306 if not configured.
     */
    @Value("${gymverse.datasource.port:3306}")
    private String dbPort;

    /**
     * In-memory cache of tenant datasource pools.
     *
     * Key   = Tenant database name
     * Value = HikariCP datasource for that database
     *
     * ConcurrentHashMap ensures thread-safe access when multiple
     * requests attempt to load tenants simultaneously.
     */
    private final ConcurrentHashMap<String, HikariDataSource> cache =
            new ConcurrentHashMap<>();

    /**
     * Retrieves an existing datasource from the cache or creates
     * a new one if it does not already exist.
     *
     * <p>
     * computeIfAbsent() guarantees that datasource creation occurs
     * only once per tenant, even under concurrent access.
     * </p>
     *
     * @param dbName tenant database name
     * @return tenant datasource
     */
    public DataSource getOrCreate(String dbName) {

        // If datasource exists, return it.
        // Otherwise, create, cache, and return it.
        return cache.computeIfAbsent(dbName, this::buildDataSource);
    }

    /**
     * Pre-loads the central datasource into the cache.
     *
     * <p>
     * During application startup, the primary datasource
     * (usually the central/master database) can be registered
     * so that Hibernate can immediately use it.
     * </p>
     *
     * @param key tenant identifier (typically "central")
     * @param centralDs central datasource bean
     */
    public void seedCentral(String key, DataSource centralDs) {

        // Store the central datasource only if it isn't already present.
        cache.putIfAbsent(key, (HikariDataSource) centralDs);
    }

    /**
     * Removes a tenant datasource from the cache and closes
     * its connection pool.
     *
     * <p>
     * Useful when:
     * <ul>
     *     <li>A tenant is deleted.</li>
     *     <li>A datasource configuration changes.</li>
     *     <li>You want to force datasource recreation.</li>
     * </ul>
     * </p>
     *
     * @param dbName tenant database name
     */
    public void evict(String dbName) {

        // Remove datasource from cache.
        HikariDataSource ds = cache.remove(dbName);

        // Gracefully shut down the pool if it exists.
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    /**
     * Creates a new HikariCP datasource for a tenant database.
     *
     * <p>
     * This method is invoked automatically when a datasource
     * is requested for a tenant that is not yet cached.
     * </p>
     *
     * @param dbName tenant database name
     * @return configured Hikari datasource
     */
    private HikariDataSource buildDataSource(String dbName) {

        log.info("[TenantCache] Creating pool for '{}'", dbName);

        HikariConfig config = new HikariConfig();

        // Build JDBC URL dynamically using tenant database name.
        config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                dbHost,
                dbPort,
                dbName
        ));

        // Database credentials.
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);

        // MySQL JDBC driver.
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Maximum number of connections allowed in the pool.
        config.setMaximumPoolSize(5);

        // Minimum number of idle connections maintained.
        config.setMinimumIdle(1);

        // Maximum time to wait for a connection.
        config.setConnectionTimeout(20_000);

        // Time before idle connections are removed.
        config.setIdleTimeout(300_000);

        // Maximum lifetime of a connection before renewal.
        config.setMaxLifetime(1_200_000);

        // Pool name visible in logs and monitoring tools.
        config.setPoolName("HikariPool-" + dbName);

        // Simple query used to validate connections.
        config.setConnectionTestQuery("SELECT 1");

        // Create and return datasource.
        return new HikariDataSource(config);
    }
}