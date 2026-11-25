package com.company.GymVerse.user.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.user.request.FetchGymByCodeRequest;
import com.company.GymVerse.user.request.RegisterGymRequest;
import com.company.GymVerse.user.request.SubmitJoinRequest;
import com.company.GymVerse.user.request.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
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
    private final UserTenantRoleRepository userTenantRoleRepository;

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

    @Override
    public void validateMasterContext() {
        if (TenantContext.getCurrentTenant() != null) {
            throw new BadCredentialsException("Operation requires a Master Account token; tenant-scoped tokens cannot access this endpoint");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()))) {
            throw new BadCredentialsException("Full authentication is required to access this resource");
        }
    }

    @Override
    public void validateFetchGymByCode(final UserRequest request) {
        if (request == null) {
            throw new BadRequestException("User request must not be null");
        }
        FetchGymByCodeRequest fetch = request.getFetchGymByCodeRequest();
        if (fetch == null) {
            throw new BadRequestException("Fetch gym by code request payload must not be null");
        }
        if (fetch.getGymCode() == null || fetch.getGymCode().isBlank()) {
            throw new BadRequestException("Gym code must not be null or blank");
        }
    }

    @Override
    public void validateSubmitJoinRequest(final UserRequest request) {
        if (request == null) {
            throw new BadRequestException("User request must not be null");
        }
        SubmitJoinRequest join = request.getSubmitJoinRequest();
        if (join == null) {
            throw new BadRequestException("Submit join request payload must not be null");
        }
        if (join.getGymCode() == null || join.getGymCode().isBlank()) {
            throw new BadRequestException("Gym code must not be null or blank");
        }
        if (join.getRequestNotes() != null && join.getRequestNotes().length() > 1000) {
            throw new BadRequestException("Request notes cannot exceed 1000 characters");
        }
    }

    @Override
    public void validateGymActiveForJoin(final GymTenant gymTenant, final GymSubscriptionStatus subscriptionStatus) {
        if (gymTenant == null) {
            throw new BadRequestException("Gym tenant must not be null");
        }
        if (gymTenant.getStatus() != GymStatus.ACTIVE) {
            throw new BadRequestException("Gym '" + gymTenant.getGymCode() + "' is currently " + gymTenant.getStatus() + " and not accepting new members");
        }
        if (subscriptionStatus == GymSubscriptionStatus.EXPIRED || subscriptionStatus == GymSubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Gym '" + gymTenant.getGymCode() + "' subscription is inactive and cannot accept new members");
        }
    }

    @Override
    public void validateUserNotAlreadyAssociated(final Long userAccountId, final Long gymTenantId) {
        if (userAccountId == null || gymTenantId == null) {
            throw new BadRequestException("User account ID and Gym tenant ID must not be null");
        }
        Optional<UserTenantRole> roleOpt = userTenantRoleRepository.findByUserAccountIdAndGymTenantId(userAccountId, gymTenantId);
        if (roleOpt.isPresent()) {
            UserTenantRole role = roleOpt.get();
            if (role.getStatus() == TenantMembershipStatus.ACTIVE) {
                throw new BadRequestException("User is already an active " + role.getRole() + " in gym tenant " + gymTenantId);
            }
            if (role.getStatus() == TenantMembershipStatus.PENDING) {
                throw new BadRequestException("User already has a pending role assignment in this gym");
            }
        }
    }

    @Override
    public void validateNoPendingJoinRequest(final Long userAccountId, final JoinRequestRepository joinRequestRepository) {
        if (userAccountId == null || joinRequestRepository == null) {
            throw new BadRequestException("User account ID and join request repository must not be null");
        }
        if (joinRequestRepository.existsByCentralUserIdAndStatus(userAccountId, JoinRequestStatus.PENDING)) {
            throw new BadRequestException("A pending join request already exists for this user in this gym");
        }
    }
}
