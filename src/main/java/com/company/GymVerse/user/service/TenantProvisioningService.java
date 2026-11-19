package com.company.GymVerse.user.service;

import com.company.GymVerse.config.DefaultSaasPlanSeeder;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.GymConfig;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.repositories.tenant.GymConfigRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.user.config.TenantConfigProperties;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import com.company.GymVerse.user.dto.TenantProvisioningResult;
import com.company.GymVerse.user.exceptions.TenantProvisioningException;
import com.company.GymVerse.user.util.SubscriptionDateUtil;
import com.company.GymVerse.user.validations.TenantProvisioningValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator service responsible for end-to-end tenant provisioning:
 * 1. Generates unique tenant identifier code and physical database name.
 * 2. Creates the isolated physical MySQL database.
 * 3. Runs tenant-scoped Flyway migrations to establish initial schema.
 * 4. Seeds initial operational admin profile (zero passwords) and gym configuration.
 * 5. Registers central tenant records, administrative authority, and SaaS trial subscription.
 * 6. Guarantees transactional compensation cleanup (drops database) on failure to prevent orphan resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private static final int MAX_COLLISION_ATTEMPTS = 50;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantDatabaseService tenantDatabaseService;
    private final TenantFlywayService tenantFlywayService;
    private final TenantRoutingBridge tenantRoutingBridge;
    private final TenantDataSourceCache tenantDataSourceCache;
    private final GymTenantRepository gymTenantRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final GymSubscriptionRepository gymSubscriptionRepository;
    private final SaasSubscriptionPlanRepository saasSubscriptionPlanRepository;
    private final TenantUserRepository tenantUserRepository;
    private final GymConfigRepository gymConfigRepository;
    private final TenantConfigProperties tenantConfigProperties;
    private final TenantProvisioningValidations tenantProvisioningValidations;
    private final PlatformTransactionManager centralTransactionManager;
    private final DefaultSaasPlanSeeder defaultSaasPlanSeeder;

    /**
     * Provisions a new gym tenant with physical database, schema migrations, local seed, and central registry.
     *
     * @param command the provisioning parameters
     * @return the result containing central and tenant entity models and connection identifiers
     */
    public TenantProvisioningResult provisionTenant(TenantProvisioningCommand command) {
        tenantProvisioningValidations.validateProvisioningCommand(command);

        String gymCode = resolveGymCode(command.getCustomGymCode());
        String dbName = generateUniqueDbName(command.getGymName());

        boolean dbCreated = false;
        try {
            log.info("Step 1/5: Creating physical tenant database '{}'...", dbName);
            tenantDatabaseService.createDatabase(dbName);
            dbCreated = true;

            log.info("Step 2/5: Executing Flyway migrations on tenant database '{}'...", dbName);
            tenantFlywayService.migrate(dbName);

            log.info("Step 3/5: Seeding tenant-local admin and facility configuration in '{}'...", dbName);
            TenantSeedData seedData = tenantRoutingBridge.callInTenantTx(dbName, status -> seedTenantDatabase(command));

            log.info("Step 4/5: Persisting central registry, authority role, and trial subscription for '{}'...", gymCode);
            CentralPersistenceResult centralResult = persistCentralRecords(command, gymCode, dbName);

            log.info("Step 5/5: Successfully provisioned tenant '{}' [dbName='{}', gymCode='{}']",
                    command.getGymName(), dbName, gymCode);

            return TenantProvisioningResult.builder()
                    .gymTenant(centralResult.gymTenant())
                    .userTenantRole(centralResult.userTenantRole())
                    .gymSubscription(centralResult.gymSubscription())
                    .tenantAdminUser(seedData.tenantUser())
                    .gymConfig(seedData.gymConfig())
                    .gymCode(gymCode)
                    .dbName(dbName)
                    .trialEndDate(centralResult.trialEndDate())
                    .trialDaysRemaining(centralResult.trialDaysRemaining())
                    .build();

        } catch (Exception ex) {
            log.error("Tenant provisioning failed for dbName='{}', gymCode='{}'. Executing compensation rollback.",
                    dbName, gymCode, ex);

            if (dbCreated) {
                try {
                    tenantDataSourceCache.evict(dbName);
                } catch (Exception evictEx) {
                    log.warn("Failed to evict datasource cache for '{}': {}", dbName, evictEx.getMessage());
                }

                try {
                    tenantDatabaseService.dropDatabase(dbName);
                    log.info("Compensation rollback successful: dropped orphan tenant database '{}'", dbName);
                } catch (Exception dropEx) {
                    log.error("Failed to drop database '{}' during compensation rollback!", dbName, dropEx);
                }
            }

            TenantContext.clear();

            if (ex instanceof BadRequestException) {
                throw (BadRequestException) ex;
            }
            throw new TenantProvisioningException("Failed to provision tenant: " + ex.getMessage(), ex);
        }
    }

    private String resolveGymCode(String customGymCode) {
        if (customGymCode != null && !customGymCode.isBlank()) {
            return customGymCode.trim().toUpperCase();
        }

        for (int i = 0; i < MAX_COLLISION_ATTEMPTS; i++) {
            String candidate = "GYM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!gymTenantRepository.existsByGymCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique gym code after " + MAX_COLLISION_ATTEMPTS + " attempts");
    }

    private String generateUniqueDbName(String gymName) {
        String prefix = gymName.trim().toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (prefix.isBlank()) {
            prefix = "gym";
        }
        if (prefix.length() > 25) {
            prefix = prefix.substring(0, 25).replaceAll("_+$", "");
        }

        for (int i = 0; i < MAX_COLLISION_ATTEMPTS; i++) {
            int randomNum = 100000 + RANDOM.nextInt(900000);
            String candidate = "gymverse_tenant_" + prefix + "_" + randomNum;
            if (!gymTenantRepository.existsByDbName(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique database name after " + MAX_COLLISION_ATTEMPTS + " attempts");
    }

    private TenantSeedData seedTenantDatabase(TenantProvisioningCommand command) {
        UserAccount account = command.getUserAccount();

        TenantUser adminUser = TenantUser.builder()
                .centralUserId(account.getId())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber() != null ? account.getPhoneNumber() : command.getPhoneNumber())
                .role(TenantRole.TENANT_ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .build();
        TenantUser savedUser = tenantUserRepository.save(adminUser);

        GymConfig config = GymConfig.builder()
                .latitude(command.getLatitude() != null ? command.getLatitude() : 0.0)
                .longitude(command.getLongitude() != null ? command.getLongitude() : 0.0)
                .radius(command.getRadius() != null ? command.getRadius() : 100.0)
                .timeZone(command.getTimeZone() != null && !command.getTimeZone().isBlank() ? command.getTimeZone() : "UTC")
                .openingTime(command.getOpeningTime() != null ? command.getOpeningTime() : LocalTime.of(6, 0))
                .closingTime(command.getClosingTime() != null ? command.getClosingTime() : LocalTime.of(22, 0))
                .autoCheckoutTime(command.getAutoCheckoutTime())
                .build();
        GymConfig savedConfig = gymConfigRepository.save(config);

        return new TenantSeedData(savedUser, savedConfig);
    }

    private CentralPersistenceResult persistCentralRecords(TenantProvisioningCommand command, String gymCode, String dbName) {
        TransactionTemplate centralTx = new TransactionTemplate(centralTransactionManager);
        return Objects.requireNonNull(centralTx.execute(status -> {
            String contactEmail = (command.getContactEmail() != null && !command.getContactEmail().isBlank())
                    ? command.getContactEmail().trim()
                    : command.getUserAccount().getEmail();

            GymTenant gymTenant = GymTenant.builder()
                    .gymName(command.getGymName().trim())
                    .contactEmail(contactEmail)
                    .phoneNumber(command.getPhoneNumber())
                    .address(command.getAddress().trim())
                    .city(command.getCity().trim())
                    .logoUrl(command.getLogoUrl())
                    .gymCode(gymCode)
                    .dbName(dbName)
                    .status(GymStatus.ACTIVE)
                    .registeredAt(Instant.now())
                    .build();
            GymTenant savedTenant = gymTenantRepository.save(gymTenant);

            UserTenantRole userTenantRole = UserTenantRole.builder()
                    .userAccount(command.getUserAccount())
                    .gymTenant(savedTenant)
                    .role(TenantRole.TENANT_ADMIN)
                    .adminAuthority(AdminAuthority.PRIMARY)
                    .status(TenantMembershipStatus.ACTIVE)
                    .joinedAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UserTenantRole savedRole = userTenantRoleRepository.save(userTenantRole);

            SaasSubscriptionPlan plan = saasSubscriptionPlanRepository.findFirstByIsTrialTrueAndActiveTrue()
                    .orElseGet(defaultSaasPlanSeeder::seedDefaultTrialPlan);

            LocalDate startDate = LocalDate.now();
            LocalDate trialEndDate = SubscriptionDateUtil.calculateTrialEndDate(startDate, tenantConfigProperties.getDurationDays());
            long trialDaysRemaining = SubscriptionDateUtil.calculateDaysRemaining(trialEndDate, startDate);

            GymSubscription subscription = GymSubscription.builder()
                    .gymTenant(savedTenant)
                    .plan(plan)
                    .pricePaid(BigDecimal.ZERO)
                    .billingCycle(plan.getBillingCycle() != null ? plan.getBillingCycle() : BillingCycle.MONTHLY)
                    .startDate(startDate)
                    .endDate(trialEndDate)
                    .status(GymSubscriptionStatus.TRIAL)
                    .subscribedAt(Instant.now())
                    .transactionReference("FREE_TRIAL")
                    .build();
            GymSubscription savedSubscription = gymSubscriptionRepository.save(subscription);

            return new CentralPersistenceResult(savedTenant, savedRole, savedSubscription, trialEndDate, trialDaysRemaining);
        }), "Central transaction result must not be null");
    }

    private record TenantSeedData(TenantUser tenantUser, GymConfig gymConfig) {}

    private record CentralPersistenceResult(
            GymTenant gymTenant,
            UserTenantRole userTenantRole,
            GymSubscription gymSubscription,
            LocalDate trialEndDate,
            long trialDaysRemaining
    ) {}
}
