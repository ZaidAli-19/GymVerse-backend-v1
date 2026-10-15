package com.company.GymVerse.platformAdmin.validations;

import com.company.GymVerse.exceptions.BadRequestException;
import com.company.GymVerse.exceptions.ConflictException;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Implementation of domain invariant and permission checks for platform administration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminValidationsImpl implements PlatformAdminValidations {

    private final SaasSubscriptionPlanRepository planRepository;

    @Override
    public void validateCreatePlan(PlatformAdminRequest request) {
        if (request == null) {
            throw new BadRequestException("Plan creation request cannot be null");
        }

        // Validate plan name
        String name = request.getName();
        if (name == null || name.trim().isBlank()) {
            throw new BadRequestException("Plan name cannot be blank");
        }
        name = name.trim();
        if (name.length() > 100) {
            throw new BadRequestException("Plan name must not exceed 100 characters");
        }

        // Check for duplicate plan name (case-insensitive)
        if (planRepository.existsByNameIgnoreCase(name)) {
            log.warn("Plan creation rejected: Duplicate plan name '{}'", name);
            throw new ConflictException("A plan with name '" + name + "' already exists");
        }

        // Validate price
        BigDecimal price = request.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price must be a non-negative decimal value");
        }
        if (price.scale() > 2) {
            throw new BadRequestException("Price cannot have more than 2 decimal places");
        }

        // Validate billing cycle
        if (request.getBillingCycle() == null) {
            throw new BadRequestException("Billing cycle is required");
        }

        // Validate capacity limits
        Integer maxMembers = request.getMaxMembers();
        Integer maxTrainers = request.getMaxTrainers();
        if (maxMembers == null || maxMembers < 1 || maxTrainers == null || maxTrainers < 0) {
            throw new BadRequestException("Maximum members must be at least 1 and trainers must be non-negative");
        }
    }
}
