package com.company.GymVerse.user.factory;

import com.company.GymVerse.user.request.enums.UserRequestType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory router component resolving and dispatching {@link UserOperation} beans
 * based on {@link UserRequestType}.
 */
@Component
public class UserFactory {

    private final Map<UserRequestType, UserOperation> operations;

    public UserFactory(final List<UserOperation> operationList) {
        Map<UserRequestType, UserOperation> map = new EnumMap<>(UserRequestType.class);
        if (operationList != null) {
            for (UserOperation op : operationList) {
                if (op != null && op.getRequestType() != null) {
                    map.put(op.getRequestType(), op);
                }
            }
        }
        this.operations = Collections.unmodifiableMap(map);
    }

    /**
     * Resolves the concrete operation bean corresponding to the requested type.
     *
     * @param requestType the operation discriminator
     * @return the matching {@link UserOperation} bean, or null if unregistered
     */
    public UserOperation getOperation(final UserRequestType requestType) {
        return operations.get(requestType);
    }
}
