package com.company.GymVerse.auth.factory;

import com.company.GymVerse.auth.request.enums.AuthRequestType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory dispatching incoming Auth requests to registered {@link AuthOperation} handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFactory {

    private final List<AuthOperation> operations;
    private final Map<AuthRequestType, AuthOperation> operationMap = new HashMap<>();

    @PostConstruct
    public void init() {
        operations.forEach(op -> {
            operationMap.put(op.getRequestType(), op);
            log.info("Registered Auth operation: [{}] -> {}", op.getRequestType(), op.getClass().getSimpleName());
        });
    }

    public AuthOperation getOperation(AuthRequestType requestType) {
        return operationMap.get(requestType);
    }
}
