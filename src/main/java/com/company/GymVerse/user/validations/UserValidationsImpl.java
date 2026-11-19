package com.company.GymVerse.user.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.user.request.RegisterGymRequest;
import com.company.GymVerse.user.request.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Implementation of domain invariant and constraint validations for central master user operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidationsImpl implements UserValidations {

    private static final Pattern GYM_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9-]{4,10}$");
    private final GymTenantRepository gymTenantRepository;

    @Override
    public void validateRegisterGym(final UserRequest request) {
        if (request == null) {
            throw new BadRequestException("User request must not be null");
        }
        RegisterGymRequest reg = request.getRegisterGymRequest();
        if (reg == null) {
            throw new BadRequestException("Register gym request payload must not be null");
        }
        if (reg.getGymName() == null || reg.getGymName().isBlank()) {
            throw new BadRequestException("Gym name is required");
        }
        if (reg.getAddress() == null || reg.getAddress().isBlank()) {
            throw new BadRequestException("Gym address is required");
        }
        if (reg.getCity() == null || reg.getCity().isBlank()) {
            throw new BadRequestException("Gym city is required");
        }

        if (reg.getCustomGymCode() != null && !reg.getCustomGymCode().isBlank()) {
            validateCustomGymCode(reg.getCustomGymCode());
        }

        if (reg.getLatitude() != null) {
            if (reg.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 || reg.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new BadRequestException("Latitude must be between -90 and 90");
            }
        }

        if (reg.getLongitude() != null) {
            if (reg.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 || reg.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new BadRequestException("Longitude must be between -180 and 180");
            }
        }
    }

    @Override
    public void validateCustomGymCode(final String customGymCode) {
        if (customGymCode == null || customGymCode.isBlank()) {
            throw new BadRequestException("Custom gym code must not be blank");
        }
        String trimmed = customGymCode.trim().toUpperCase();
        if (!GYM_CODE_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Custom gym code must be 4 to 10 alphanumeric characters");
        }
        if (gymTenantRepository.findByGymCode(trimmed).isPresent()) {
            throw new BadRequestException("Gym code '" + trimmed + "' is already registered");
        }
    }
}
