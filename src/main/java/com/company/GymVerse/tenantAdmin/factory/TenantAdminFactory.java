package com.company.GymVerse.tenantAdmin.factory;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory dispatching incoming TenantAdminRequestType operations to their corresponding handler beans.
 */
@Component
public class TenantAdminFactory {

    private final Map<TenantAdminRequestType, TenantAdminOperation> operations = new EnumMap<>(TenantAdminRequestType.class);

    public TenantAdminFactory(final List<TenantAdminOperation> operationList) {
        for (TenantAdminOperation op : operationList) {
            operations.put(op.getRequestType(), op);
        }
    }

    /**
     * Resolves the handler bean associated with the requested operation type.
     *
     * @param requestType operation discriminator
     * @return matching {@link TenantAdminOperation}
     * @throws BadRequestException if the operation type is not registered
     */
    public TenantAdminOperation getOperation(final TenantAdminRequestType requestType) {
        return Optional.ofNullable(operations.get(requestType))
                .orElseThrow(() -> new BadRequestException("Unsupported TenantAdminRequestType: " + requestType));
    }
}
