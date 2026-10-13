package com.company.GymVerse.multitanancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service responsible for managing tenant databases.
 *
 * <p>
 * In a database-per-tenant architecture, every tenant receives
 * its own dedicated database. This service provides functionality
 * to create and remove those databases dynamically.
 * </p>
 *
 * <p>
 * The service executes administrative SQL statements against the
 * central MySQL server using the central datasource.
 * </p>
 *
 * <p>
 * Typical usage:
 * <ul>
 *     <li>Create a new tenant database during onboarding.</li>
 *     <li>Delete a tenant database when a tenant is removed.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDatabaseService {

    /**
     * Central datasource used for administrative database operations.
     *
     * <p>
     * This datasource connects to MySQL with privileges sufficient
     * to create and drop databases.
     * </p>
     */
    private final DataSource centralDataSource;

    /**
     * Creates a new tenant database if it does not already exist.
     *
     * <p>
     * The database is created using:
     * <ul>
     *     <li>UTF-8 MB4 character set</li>
     *     <li>Unicode collation</li>
     * </ul>
     * This ensures proper support for international characters and emojis.
     * </p>
     *
     * @param dbName name of the tenant database
     * @throws IllegalArgumentException if the database name is invalid
     * @throws RuntimeException if database creation fails
     */
    public void createDatabase(String dbName) {

        // Validate database name before using it in SQL.
        validate(dbName);

        String sql = String.format(
                "CREATE DATABASE IF NOT EXISTS `%s` " +
                        "CHARACTER SET utf8mb4 " +
                        "COLLATE utf8mb4_unicode_ci",
                dbName
        );

        execute(sql, dbName, "create");
    }

    /**
     * Drops a tenant database if it exists.
     *
     * <p>
     * This operation permanently removes all tables and data
     * contained within the database.
     * </p>
     *
     * @param dbName name of the tenant database
     * @throws IllegalArgumentException if the database name is invalid
     * @throws RuntimeException if database deletion fails
     */
    public void dropDatabase(String dbName) {

        // Validate database name before using it in SQL.
        validate(dbName);

        execute(
                "DROP DATABASE IF EXISTS `" + dbName + "`",
                dbName,
                "drop"
        );
    }

    /**
     * Executes a database administration command.
     *
     * <p>
     * A connection is obtained from the central datasource,
     * the SQL statement is executed, and resources are
     * automatically closed via try-with-resources.
     * </p>
     *
     * @param sql SQL statement to execute
     * @param dbName target database name
     * @param action operation name (create/drop)
     * @throws RuntimeException if execution fails
     */
    private void execute(
            String sql,
            String dbName,
            String action) {

        try (
                Connection conn = centralDataSource.getConnection();
                Statement stmt = conn.createStatement()
        ) {

            // Execute database creation/deletion statement.
            stmt.executeUpdate(sql);

            log.info("[TenantDB] {} '{}'", action, dbName);

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to " + action + " database: " + dbName,
                    e
            );
        }
    }

    /**
     * Validates a database name before it is used in SQL.
     *
     * <p>
     * Only alphanumeric characters and underscores are allowed.
     * This prevents SQL injection and ensures valid MySQL
     * database naming conventions.
     * </p>
     *
     * <p>
     * Examples:
     * <pre>
     * gym_abc     ✓ valid
     * tenant123   ✓ valid
     * gym-test    ✗ invalid
     * gym db      ✗ invalid
     * gym;DROP    ✗ invalid
     * </pre>
     * </p>
     *
     * @param dbName database name to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validate(String dbName) {

        if (!dbName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Invalid database name: " + dbName
            );
        }
    }
}