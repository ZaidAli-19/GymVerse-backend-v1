package com.company.GymVerse.user.util;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Helper component encapsulating reusable boilerplate logic for user module operations.
 */
@Component
@RequiredArgsConstructor
public class UserHelper {

    private final UserAccountRepository userAccountRepository;
    private final GymSubscriptionRepository gymSubscriptionRepository;

    /**
     * Resolves the authenticated {@link UserAccount} from Spring SecurityContextHolder.
     *
     * @return active UserAccount
     * @throws ResourceNotFoundException if user record is missing in central database
     */
    public UserAccount getAuthenticatedUserAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("No authenticated security context found");
        }
        Long userId = Long.parseLong(authentication.getName());
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found for authenticated ID: " + userId));
    }

    /**
     * Normalizes and cleans a gym code string.
     *
     * @param gymCode raw gym code
     * @return trimmed, upper-cased gym code or null
     */
    public String normalizeGymCode(final String gymCode) {
        return gymCode != null ? gymCode.trim().toUpperCase() : null;
    }

    /**
     * Retrieves the latest SaaS subscription status for a given gym tenant.
     *
     * @param gymTenantId central gym tenant ID
     * @return latest GymSubscriptionStatus or null if unassigned
     */
    public GymSubscriptionStatus getLatestSubscriptionStatus(final Long gymTenantId) {
        if (gymTenantId == null) {
            return null;
        }
        List<GymSubscription> subscriptions = gymSubscriptionRepository.findByGymTenantIdOrderBySubscribedAtDesc(gymTenantId);
        return subscriptions.isEmpty() ? null : subscriptions.get(0).getStatus();
    }
}
