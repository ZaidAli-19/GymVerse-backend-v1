package com.company.GymVerse.platformAdmin.service;

import com.company.GymVerse.config.PlatformAdminSeedProperties;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service responsible for safe, idempotent provisioning of the initial platform administrator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminProvisioningService {

    private final PlatformAdminSeedProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Seeds the initial platform administrator into the central database.
     * Enforces strict idempotency, password hashing, and safe conflict rejection.
     */
    @Transactional("centralTransactionManager")
    public void seedInitialPlatformAdmin() {
        if (!properties.isEnabled()) {
            log.info("Initial platform admin seeding is disabled via configuration. Skipping.");
            return;
        }

        String email = properties.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Initial Platform Admin email must be provided via PLATFORM_ADMIN_EMAIL or platformAdminSeed.yml");
        }
        email = email.trim().toLowerCase();

        Optional<UserAccount> existingUserOpt = userAccountRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            UserAccount existing = existingUserOpt.get();
            if (existing.isPlatformAdmin()) {
                log.info("Platform administrator account already initialized for {}. Skipping seed.", email);
                return;
            } else {
                log.error("Seed conflict: Account with email '{}' already exists but is NOT a platform administrator. Aborting seeding to prevent unauthorized privilege escalation.", email);
                throw new IllegalStateException("Cannot seed platform administrator: email '" + email + "' already registered to a non-admin user");
            }
        }

        String rawPassword = properties.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException("Initial Platform Admin password must be provided via PLATFORM_ADMIN_PASSWORD environment variable when seeding is enabled");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        UserAccount admin = UserAccount.builder()
                .email(email)
                .firstName(properties.getFirstName() != null && !properties.getFirstName().isBlank() ? properties.getFirstName().trim() : "Platform")
                .lastName(properties.getLastName() != null && !properties.getLastName().isBlank() ? properties.getLastName().trim() : "Superadmin")
                .password(hashedPassword)
                .phoneNumber(properties.getPhoneNumber() != null && !properties.getPhoneNumber().isBlank() ? properties.getPhoneNumber().trim() : null)
                .emailVerified(properties.isEmailVerified())
                .isPlatformAdmin(properties.isPlatformAdmin())
                .status(properties.getStatus() != null ? properties.getStatus() : UserAccountStatus.ACTIVE)
                .build();

        userAccountRepository.save(admin);
        log.info("Initial platform administrator created successfully for: {}", email);
    }
}
