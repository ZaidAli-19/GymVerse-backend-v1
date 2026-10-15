package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.config.PlatformAdminSeedProperties;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.platformAdmin.service.PlatformAdminProvisioningService;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional("centralTransactionManager")
@DisplayName("PlatformAdminProvisioningService Integration Tests")
class PlatformAdminProvisioningIntegrationTest {

    @Autowired
    private PlatformAdminProvisioningService provisioningService;

    @Autowired
    private PlatformAdminSeedProperties properties;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_SEED_EMAIL = "seed_test_admin@gymverse.com";
    private static final String TEST_RAW_PASSWORD = "ValidSecretPassword2026!";

    @BeforeEach
    void setUp() {
        // Clean up any test accounts before each test
        userAccountRepository.findByEmail(TEST_SEED_EMAIL).ifPresent(userAccountRepository::delete);
        userAccountRepository.findByEmail("non_admin_conflict@gymverse.com").ifPresent(userAccountRepository::delete);

        // Configure default test seed properties
        properties.setEnabled(true);
        properties.setEmail(TEST_SEED_EMAIL);
        properties.setFirstName("Platform");
        properties.setLastName("Superadmin");
        properties.setPassword(TEST_RAW_PASSWORD);
        properties.setPhoneNumber("+19999999999");
        properties.setEmailVerified(true);
        properties.setPlatformAdmin(true);
        properties.setStatus(UserAccountStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        userAccountRepository.findByEmail(TEST_SEED_EMAIL).ifPresent(userAccountRepository::delete);
        userAccountRepository.findByEmail("non_admin_conflict@gymverse.com").ifPresent(userAccountRepository::delete);
    }

    @Test
    @DisplayName("AC-PA1.2: Successful fresh bootstrapping seeds admin with verified BCrypt hash")
    void testSuccessfulFreshSeeding() {
        provisioningService.seedInitialPlatformAdmin();

        Optional<UserAccount> adminOpt = userAccountRepository.findByEmail(TEST_SEED_EMAIL);
        assertThat(adminOpt).isPresent();

        UserAccount admin = adminOpt.get();
        assertThat(admin.isPlatformAdmin()).isTrue();
        assertThat(admin.isEmailVerified()).isTrue();
        assertThat(admin.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(admin.getFirstName()).isEqualTo("Platform");
        assertThat(admin.getLastName()).isEqualTo("Superadmin");
        assertThat(admin.getPhoneNumber()).isEqualTo("+19999999999");
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();

        // Verify password is encrypted with BCrypt and matches raw password
        assertThat(passwordEncoder.matches(TEST_RAW_PASSWORD, admin.getPassword())).isTrue();
        assertThat(admin.getPassword()).doesNotContain(TEST_RAW_PASSWORD);
    }

    @Test
    @DisplayName("AC-PA1.3: Repeated startup is idempotent and does not reset existing administrator password")
    void testIdempotencyOnSubsequentRuns() {
        // First run
        provisioningService.seedInitialPlatformAdmin();
        UserAccount initialAdmin = userAccountRepository.findByEmail(TEST_SEED_EMAIL).orElseThrow();
        String initialPasswordHash = initialAdmin.getPassword();

        // Second run
        provisioningService.seedInitialPlatformAdmin();
        UserAccount secondCheck = userAccountRepository.findByEmail(TEST_SEED_EMAIL).orElseThrow();

        assertThat(secondCheck.getPassword())
                .as("Password hash must not be reset on repeated startups")
                .isEqualTo(initialPasswordHash);
        assertThat(secondCheck.isPlatformAdmin()).isTrue();
    }

    @Test
    @DisplayName("AC-PA1.4: Safe conflict guard rejects silent promotion of pre-existing non-admin account")
    void testSafeConflictOnExistingNonAdminAccount() {
        String nonAdminEmail = "non_admin_conflict@gymverse.com";

        // Pre-existing non-admin user
        UserAccount regularUser = UserAccount.builder()
                .email(nonAdminEmail)
                .firstName("Regular")
                .lastName("Member")
                .password(passwordEncoder.encode("UserPassword123!"))
                .isPlatformAdmin(false)
                .emailVerified(true)
                .status(UserAccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userAccountRepository.save(regularUser);

        // Attempt seeding with the non-admin's email
        properties.setEmail(nonAdminEmail);

        assertThatThrownBy(() -> provisioningService.seedInitialPlatformAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot seed platform administrator: email 'non_admin_conflict@gymverse.com' already registered to a non-admin user");

        // Verify account was NOT silently promoted
        UserAccount checkUser = userAccountRepository.findByEmail(nonAdminEmail).orElseThrow();
        assertThat(checkUser.isPlatformAdmin())
                .as("Non-admin account must never be silently promoted to platform admin")
                .isFalse();
    }

    @Test
    @DisplayName("AC-PA1.5: Clean no-op when seeding is disabled via configuration")
    void testSeedingDisabled() {
        properties.setEnabled(false);

        provisioningService.seedInitialPlatformAdmin();

        Optional<UserAccount> adminOpt = userAccountRepository.findByEmail(TEST_SEED_EMAIL);
        assertThat(adminOpt).isEmpty();
    }

    @Test
    @DisplayName("Validation fails when password is blank while seeding is enabled")
    void testMissingPasswordThrowsException() {
        properties.setPassword("");

        assertThatThrownBy(() -> provisioningService.seedInitialPlatformAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Initial Platform Admin password must be provided via PLATFORM_ADMIN_PASSWORD environment variable when seeding is enabled");
    }

    @Test
    @DisplayName("Repository methods for platform admin existence operate correctly")
    void testRepositoryPlatformAdminMethods() {
        provisioningService.seedInitialPlatformAdmin();

        assertThat(userAccountRepository.existsByIsPlatformAdminTrue()).isTrue();
        assertThat(userAccountRepository.findFirstByIsPlatformAdminTrue()).isPresent();
    }
}
