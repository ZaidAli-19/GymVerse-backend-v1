package com.company.GymVerse.user.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation of domain invariant and constraint validations for tenant provisioning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningValidationsImpl implements TenantProvisioningValidations {

    private final GymTenantRepository gymTenantRepository;

    @Override
    public void validateProvisioningCommand(TenantProvisioningCommand command) {
        if (command == null) {
            throw new BadRequestException("Tenant provisioning command must not be null");
        }
        if (command.getUserAccount() == null || command.getUserAccount().getId() == null) {
            throw new BadRequestException("UserAccount with a non-null ID is required for tenant provisioning");
        }
        if (command.getGymName() == null || command.getGymName().isBlank()) {
            throw new BadRequestException("Gym name is required");
        }
        if (command.getAddress() == null || command.getAddress().isBlank()) {
            throw new BadRequestException("Gym address is required");
        }
        if (command.getCity() == null || command.getCity().isBlank()) {
            throw new BadRequestException("Gym city is required");
        }

        if (command.getCustomGymCode() != null && !command.getCustomGymCode().isBlank()) {
            validateCustomGymCode(command.getCustomGymCode());
        }
    }

    @Override
    public void validateCustomGymCode(String customGymCode) {
        if (customGymCode == null || customGymCode.isBlank()) {
            return;
        }
        String sanitized = customGymCode.trim().toUpperCase();
        if (gymTenantRepository.existsByGymCode(sanitized)) {
            throw new BadRequestException("Gym code '" + sanitized + "' is already taken");
        }
    }
}
