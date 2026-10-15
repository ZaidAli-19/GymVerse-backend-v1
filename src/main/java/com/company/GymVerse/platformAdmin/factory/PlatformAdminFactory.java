package com.company.GymVerse.platformAdmin.factory;

import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminFactory {

    private final List<PlatformAdminFactoryOperation> operations;
    private final Map<PlatformAdminRequestType, PlatformAdminFactoryOperation> operationMap = new HashMap<>();

    @PostConstruct
    void init() {
        operations.forEach(op -> {
            operationMap.put(op.getRequestType(), op);
            log.info("Registered PlatformAdmin operation: {} -> {}", op.getRequestType(), op.getClass().getSimpleName());
        });
    }

    public PlatformAdminFactoryOperation getOperation(PlatformAdminRequestType requestType) {
        return operationMap.get(requestType);
    }
}
