package com.company.GymVerse.multitanancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

/**
 * Service responsible for executing Flyway database migrations
 * for tenant-specific databases.
 *
 * <p>
 * In a database-per-tenant architecture, every tenant database
 * starts as an empty database. Before the application can use it,
 * all required tables, indexes, constraints, and seed data must
 * be created.
 * </p>
 *
 * <p>
 * This service applies Flyway migrations located in:
 * </p>
 *
 * <pre>
 * src/main/resources/db/migration/tenant
 * </pre>
 *
 * <p>
 * Each tenant database maintains its own
 * {@code flyway_schema_history} table to track which migrations
 * have already been applied.
 * </p>
 *
 * <p>
 * Typical onboarding flow:
 * </p>
 *
 * <pre>
 * Create Database
 *        ↓
 * Run Flyway Migration
 *        ↓
 * Database Ready
 *        ↓
 * Tenant Can Start Using System
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantFlywayService {

    /**
     * Cache that provides tenant-specific datasource instances.
     *
     * <p>
     * Used to obtain a datasource connected to the target
     * tenant database before running Flyway migrations.
     * </p>
     */
    private final TenantDataSourceCache dataSourceCache;

    /**
     * Executes Flyway migrations for a tenant database.
     *
     * <p>
     * If the database is new:
     * <ul>
     *     <li>All migration scripts are executed.</li>
     *     <li>Required tables are created.</li>
     *     <li>The Flyway history table is initialized.</li>
     * </ul>
     * </p>
     *
     * <p>
     * If the database already exists:
     * <ul>
     *     <li>Flyway checks migration history.</li>
     *     <li>Only new migrations are executed.</li>
     * </ul>
     * </p>
     *
     * @param tenantDbName tenant database name
     */
    public void migrate(String tenantDbName) {

        log.info("[TenantFlyway] Migrating '{}'", tenantDbName);

        // Configure Flyway for the target tenant database.
        Flyway flyway = Flyway.configure()

                // Datasource connected to the tenant database.
                .dataSource(
                        dataSourceCache.getOrCreate(tenantDbName)
                )

                // Folder containing tenant migration scripts.
                .locations("classpath:db/migration/tenant")

                // Table used by Flyway to track migration history.
                .table("flyway_schema_history")

                // Verify migration integrity before executing.
                // Prevents accidental schema drift.
                .validateOnMigrate(true)

                // Build Flyway instance.
                .load();

        // Execute pending migrations.
        flyway.migrate();

        log.info("[TenantFlyway] Done '{}'", tenantDbName);
    }
}