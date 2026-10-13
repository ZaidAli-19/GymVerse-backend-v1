package com.company.GymVerse.multitanancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bridge component managing tenant context lifecycle, programmatic transaction demarcation,
 * and guaranteed ThreadLocal cleanup.
 *
 * <p>
 * Enforces the critical multitenancy architectural invariant:
 * {@link TenantContext#setCurrentTenant(String)} MUST be called prior to entering the
 * {@code tenantTransactionManager} transaction boundary, so Hibernate properly resolves
 * the tenant identifier when obtaining a connection from {@link GymVerseMultiTenantConnectionProvider}.
 * </p>
 *
 * <p>
 * Unconditionally executes {@link TenantContext#clear()} in a {@code finally} block to prevent
 * ThreadLocal leakage across thread pools.
 * </p>
 */
@Slf4j
@Component
public class TenantRoutingBridge {

    private final PlatformTransactionManager tenantTransactionManager;

    public TenantRoutingBridge(
            @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTransactionManager
    ) {
        this.tenantTransactionManager = tenantTransactionManager;
    }

    /**
     * Executes a callback function with {@link TransactionStatus} inside an active tenant context
     * and tenant transaction managed by {@code tenantTransactionManager}.
     *
     * @param tenantDbName the target tenant database name (e.g. "gym_db_1")
     * @param action       the transactional callback to execute
     * @param <T>          the return type
     * @return the result of the action
     */
    public <T> T callInTenantTx(String tenantDbName, TransactionCallback<T> action) {
        if (tenantDbName == null || tenantDbName.isBlank()) {
            throw new IllegalArgumentException("Tenant database name must not be null or blank");
        }
        Objects.requireNonNull(action, "Action must not be null");

        log.debug("Establishing tenant context for database: {}", tenantDbName);
        TenantContext.setCurrentTenant(tenantDbName);
        try {
            TransactionTemplate txTemplate = new TransactionTemplate(tenantTransactionManager);
            return txTemplate.execute(action);
        } finally {
            TenantContext.clear();
            log.debug("Cleared tenant context for database: {}", tenantDbName);
        }
    }

    /**
     * Executes a supplier function within an active tenant context and a tenant transaction.
     *
     * @param tenantDbName the target tenant database name
     * @param action       the supplier action to execute
     * @param <T>          the return type
     * @return the result of the action
     */
    public <T> T callInTenant(String tenantDbName, Supplier<T> action) {
        Objects.requireNonNull(action, "Action must not be null");
        return callInTenantTx(tenantDbName, status -> action.get());
    }

    /**
     * Executes a runnable action within an active tenant context and a tenant transaction.
     *
     * @param tenantDbName the target tenant database name
     * @param action       the runnable action to execute
     */
    public void runInTenant(String tenantDbName, Runnable action) {
        Objects.requireNonNull(action, "Action must not be null");
        callInTenantTx(tenantDbName, status -> {
            action.run();
            return null;
        });
    }

    /**
     * Executes a consumer action with {@link TransactionStatus} within an active tenant context
     * and a tenant transaction.
     *
     * @param tenantDbName the target tenant database name
     * @param action       the consumer action to execute
     */
    public void runInTenantTx(String tenantDbName, Consumer<TransactionStatus> action) {
        Objects.requireNonNull(action, "Action must not be null");
        callInTenantTx(tenantDbName, status -> {
            action.accept(status);
            return null;
        });
    }

    /**
     * Executes a consumer action with this bridge within an active tenant context and a tenant transaction.
     *
     * @param tenantDbName the target tenant database name
     * @param action       the consumer action to execute
     */
    public void runInTenant(String tenantDbName, Consumer<TenantRoutingBridge> action) {
        Objects.requireNonNull(action, "Action must not be null");
        runInTenant(tenantDbName, () -> action.accept(this));
    }
}
