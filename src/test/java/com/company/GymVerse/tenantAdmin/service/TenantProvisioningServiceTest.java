package com.company.GymVerse.tenantAdmin.service;

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
import com.company.GymVerse.tenantAdmin.config.TenantConfigProperties;
import com.company.GymVerse.tenantAdmin.dto.TenantProvisioningCommand;
import com.company.GymVerse.tenantAdmin.dto.TenantProvisioningResult;
import com.company.GymVerse.tenantAdmin.exceptions.TenantProvisioningException;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 4.3: Dynamic Tenant Provisioning & Schema Migration Service Tests")
class TenantProvisioningServiceTest {

    private FakeTenantDatabaseService fakeDatabaseService;
    private FakeTenantFlywayService fakeFlywayService;
    private FakeTenantRoutingBridge fakeRoutingBridge;
    private FakeTenantDataSourceCache fakeDataSourceCache;

    private Set<String> existingGymCodes;
    private Set<String> existingDbNames;
    private List<GymTenant> savedTenants;
    private List<UserTenantRole> savedRoles;
    private List<GymSubscription> savedSubscriptions;
    private List<TenantUser> savedTenantUsers;
    private List<GymConfig> savedConfigs;
    private SaasSubscriptionPlan defaultPlan;

    private GymTenantRepository gymTenantRepository;
    private UserTenantRoleRepository userTenantRoleRepository;
    private GymSubscriptionRepository gymSubscriptionRepository;
    private SaasSubscriptionPlanRepository saasSubscriptionPlanRepository;
    private TenantUserRepository tenantUserRepository;
    private GymConfigRepository gymConfigRepository;

    private TenantConfigProperties tenantConfigProperties;
    private TenantProvisioningService provisioningService;

    private UserAccount mockUserAccount;

    @BeforeEach
    void setUp() {
        fakeDatabaseService = new FakeTenantDatabaseService();
        fakeFlywayService = new FakeTenantFlywayService();
        fakeRoutingBridge = new FakeTenantRoutingBridge();
        fakeDataSourceCache = new FakeTenantDataSourceCache();

        existingGymCodes = new HashSet<>();
        existingDbNames = new HashSet<>();
        savedTenants = new ArrayList<>();
        savedRoles = new ArrayList<>();
        savedSubscriptions = new ArrayList<>();
        savedTenantUsers = new ArrayList<>();
        savedConfigs = new ArrayList<>();

        defaultPlan = SaasSubscriptionPlan.builder()
                .id(1L)
                .name("Growth Tier")
                .price(BigDecimal.valueOf(99.00))
                .billingCycle(BillingCycle.MONTHLY)
                .build();

        gymTenantRepository = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (proxy, method, args) -> {
                    if ("existsByGymCode".equals(method.getName())) {
                        return existingGymCodes.contains(args[0]);
                    }
                    if ("existsByDbName".equals(method.getName())) {
                        return existingDbNames.contains(args[0]);
                    }
                    if ("save".equals(method.getName())) {
                        GymTenant t = (GymTenant) args[0];
                        t.setId(10L);
                        savedTenants.add(t);
                        return t;
                    }
                    return null;
                }
        );

        userTenantRoleRepository = (UserTenantRoleRepository) Proxy.newProxyInstance(
                UserTenantRoleRepository.class.getClassLoader(),
                new Class<?>[]{UserTenantRoleRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        UserTenantRole r = (UserTenantRole) args[0];
                        r.setId(20L);
                        savedRoles.add(r);
                        return r;
                    }
                    return null;
                }
        );

        gymSubscriptionRepository = (GymSubscriptionRepository) Proxy.newProxyInstance(
                GymSubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{GymSubscriptionRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        GymSubscription s = (GymSubscription) args[0];
                        s.setId(30L);
                        savedSubscriptions.add(s);
                        return s;
                    }
                    return null;
                }
        );

        saasSubscriptionPlanRepository = (SaasSubscriptionPlanRepository) Proxy.newProxyInstance(
                SaasSubscriptionPlanRepository.class.getClassLoader(),
                new Class<?>[]{SaasSubscriptionPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        if (defaultPlan != null && defaultPlan.getId().equals(id)) {
                            return Optional.of(defaultPlan);
                        }
                        return Optional.empty();
                    }
                    return null;
                }
        );

        tenantUserRepository = (TenantUserRepository) Proxy.newProxyInstance(
                TenantUserRepository.class.getClassLoader(),
                new Class<?>[]{TenantUserRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        TenantUser u = (TenantUser) args[0];
                        u.setId(40L);
                        savedTenantUsers.add(u);
                        return u;
                    }
                    return null;
                }
        );

        gymConfigRepository = (GymConfigRepository) Proxy.newProxyInstance(
                GymConfigRepository.class.getClassLoader(),
                new Class<?>[]{GymConfigRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        GymConfig c = (GymConfig) args[0];
                        c.setId(50L);
                        savedConfigs.add(c);
                        return c;
                    }
                    return null;
                }
        );

        tenantConfigProperties = new TenantConfigProperties();
        tenantConfigProperties.setDurationDays(14);
        tenantConfigProperties.setDefaultPlanId(1L);

        TenantAdminValidations tenantAdminValidations = new TenantAdminValidationsImpl(gymTenantRepository);

        provisioningService = new TenantProvisioningService(
                fakeDatabaseService,
                fakeFlywayService,
                fakeRoutingBridge,
                fakeDataSourceCache,
                gymTenantRepository,
                userTenantRoleRepository,
                gymSubscriptionRepository,
                saasSubscriptionPlanRepository,
                tenantUserRepository,
                gymConfigRepository,
                tenantConfigProperties,
                tenantAdminValidations,
                new FakeTransactionManager()
        );

        mockUserAccount = UserAccount.builder()
                .id(100L)
                .email("owner@gymverse.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .build();
    }

    private TenantProvisioningCommand buildValidCommand() {
        return TenantProvisioningCommand.builder()
                .userAccount(mockUserAccount)
                .gymName("Iron Paradise Gym")
                .contactEmail("contact@ironparadise.com")
                .phoneNumber("+1987654321")
                .address("100 Muscle Beach Blvd")
                .city("Los Angeles")
                .logoUrl("https://cdn.gymverse.com/logos/iron.png")
                .latitude(34.0522)
                .longitude(-118.2437)
                .radius(150.0)
                .openingTime(LocalTime.of(5, 30))
                .closingTime(LocalTime.of(23, 0))
                .autoCheckoutTime(LocalTime.of(23, 30))
                .timeZone("America/Los_Angeles")
                .build();
    }

    @Test
    @DisplayName("Successfully provisions a new tenant with auto-generated gym code and unique dbName")
    void provisionTenant_Success_AutoGeneratedCode() {
        TenantProvisioningCommand command = buildValidCommand();

        TenantProvisioningResult result = provisioningService.provisionTenant(command);

        assertThat(result).isNotNull();
        assertThat(result.getGymTenant()).isNotNull();
        assertThat(result.getGymTenant().getGymName()).isEqualTo("Iron Paradise Gym");
        assertThat(result.getGymTenant().getStatus()).isEqualTo(GymStatus.ACTIVE);
        assertThat(result.getGymCode()).startsWith("GYM-");
        assertThat(result.getDbName()).startsWith("gymverse_tenant_iron_paradise_gym_");

        assertThat(result.getUserTenantRole()).isNotNull();
        assertThat(result.getUserTenantRole().getRole()).isEqualTo(TenantRole.TENANT_ADMIN);
        assertThat(result.getUserTenantRole().getAdminAuthority()).isEqualTo(AdminAuthority.PRIMARY);
        assertThat(result.getUserTenantRole().getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);

        assertThat(result.getGymSubscription()).isNotNull();
        assertThat(result.getGymSubscription().getStatus()).isEqualTo(GymSubscriptionStatus.TRIAL);
        assertThat(result.getGymSubscription().getPricePaid()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTrialDaysRemaining()).isEqualTo(14L);
        assertThat(result.getTrialEndDate()).isEqualTo(LocalDate.now().plusDays(14));

        // Verify tenant user in tenant-local DB has centralUserId and zero passwords
        assertThat(savedTenantUsers).hasSize(1);
        TenantUser savedLocalUser = savedTenantUsers.get(0);
        assertThat(savedLocalUser.getCentralUserId()).isEqualTo(100L);
        assertThat(savedLocalUser.getEmail()).isEqualTo("owner@gymverse.com");
        assertThat(savedLocalUser.getRole()).isEqualTo(TenantRole.TENANT_ADMIN);
        assertThat(savedLocalUser.isActive()).isTrue();

        // Verify facility configuration
        assertThat(savedConfigs).hasSize(1);
        GymConfig savedConfig = savedConfigs.get(0);
        assertThat(savedConfig.getLatitude()).isEqualTo(34.0522);
        assertThat(savedConfig.getLongitude()).isEqualTo(-118.2437);
        assertThat(savedConfig.getRadius()).isEqualTo(150.0);
        assertThat(savedConfig.getTimeZone()).isEqualTo("America/Los_Angeles");

        // Verify physical database and Flyway executions
        assertThat(fakeDatabaseService.createdDb).isEqualTo(result.getDbName());
        assertThat(fakeFlywayService.migratedDb).isEqualTo(result.getDbName());
        assertThat(fakeDatabaseService.droppedDb).isNull();
    }

    @Test
    @DisplayName("Successfully provisions a new tenant using a custom gym code")
    void provisionTenant_Success_CustomGymCode() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setCustomGymCode("IRON-LA-01");

        TenantProvisioningResult result = provisioningService.provisionTenant(command);

        assertThat(result.getGymCode()).isEqualTo("IRON-LA-01");
        assertThat(fakeDatabaseService.createdDb).isNotNull();
        assertThat(fakeDatabaseService.droppedDb).isNull();
    }

    @Test
    @DisplayName("Throws BadRequestException when custom gym code already exists")
    void provisionTenant_CustomGymCodeCollision_ThrowsBadRequest() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setCustomGymCode("DUPLICATE-CODE");
        existingGymCodes.add("DUPLICATE-CODE");

        assertThatThrownBy(() -> provisioningService.provisionTenant(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym code 'DUPLICATE-CODE' is already taken");

        assertThat(fakeDatabaseService.createdDb).isNull();
        assertThat(fakeFlywayService.migratedDb).isNull();
    }

    @Test
    @DisplayName("Throws BadRequestException on missing required command attributes")
    void provisionTenant_ValidationFailures() {
        assertThatThrownBy(() -> provisioningService.provisionTenant(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant provisioning command must not be null");

        TenantProvisioningCommand cmdNoUser = buildValidCommand();
        cmdNoUser.setUserAccount(null);
        assertThatThrownBy(() -> provisioningService.provisionTenant(cmdNoUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UserAccount with a non-null ID is required");

        TenantProvisioningCommand cmdUserNoId = buildValidCommand();
        cmdUserNoId.setUserAccount(UserAccount.builder().email("no-id@test.com").build());
        assertThatThrownBy(() -> provisioningService.provisionTenant(cmdUserNoId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UserAccount with a non-null ID is required");

        TenantProvisioningCommand cmdBlankName = buildValidCommand();
        cmdBlankName.setGymName("   ");
        assertThatThrownBy(() -> provisioningService.provisionTenant(cmdBlankName))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym name is required");

        TenantProvisioningCommand cmdBlankAddress = buildValidCommand();
        cmdBlankAddress.setAddress("");
        assertThatThrownBy(() -> provisioningService.provisionTenant(cmdBlankAddress))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym address is required");

        TenantProvisioningCommand cmdBlankCity = buildValidCommand();
        cmdBlankCity.setCity(" ");
        assertThatThrownBy(() -> provisioningService.provisionTenant(cmdBlankCity))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym city is required");
    }

    @Test
    @DisplayName("Executes compensation rollback and drops database when Flyway migration fails")
    void provisionTenant_FlywayFailure_RollsBackAndDropsDatabase() {
        TenantProvisioningCommand command = buildValidCommand();
        fakeFlywayService.throwOnMigrate = true;

        assertThatThrownBy(() -> provisioningService.provisionTenant(command))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("Failed to provision tenant: Flyway connection timeout on tenant DB");

        assertThat(fakeDatabaseService.createdDb).isNotNull();
        assertThat(fakeDataSourceCache.evictedDb).isEqualTo(fakeDatabaseService.createdDb);
        assertThat(fakeDatabaseService.droppedDb).isEqualTo(fakeDatabaseService.createdDb);

        assertThat(savedTenants).isEmpty();
        assertThat(savedRoles).isEmpty();
        assertThat(savedSubscriptions).isEmpty();
    }

    @Test
    @DisplayName("Executes compensation rollback and drops database when tenant seeding fails")
    void provisionTenant_LocalSeedFailure_RollsBackAndDropsDatabase() {
        TenantProvisioningCommand command = buildValidCommand();
        fakeRoutingBridge.throwOnCall = true;

        assertThatThrownBy(() -> provisioningService.provisionTenant(command))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("Tenant DB connection pool exhausted");

        assertThat(fakeDatabaseService.createdDb).isNotNull();
        assertThat(fakeDataSourceCache.evictedDb).isEqualTo(fakeDatabaseService.createdDb);
        assertThat(fakeDatabaseService.droppedDb).isEqualTo(fakeDatabaseService.createdDb);
        assertThat(savedTenants).isEmpty();
    }

    @Test
    @DisplayName("Executes compensation rollback when central SaaS plan resolution fails")
    void provisionTenant_PlanNotFound_RollsBackAndDropsDatabase() {
        TenantProvisioningCommand command = buildValidCommand();
        defaultPlan = null; // Forces plan not found

        assertThatThrownBy(() -> provisioningService.provisionTenant(command))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("Default SaaS subscription plan not found with ID: 1");

        assertThat(fakeDatabaseService.createdDb).isNotNull();
        assertThat(fakeDataSourceCache.evictedDb).isEqualTo(fakeDatabaseService.createdDb);
        assertThat(fakeDatabaseService.droppedDb).isEqualTo(fakeDatabaseService.createdDb);
    }

    // --- Lightweight Test Doubles (Zero ByteBuddy / Zero Java Agent Loading) ---

    private static class FakeTenantDatabaseService extends TenantDatabaseService {
        String createdDb;
        String droppedDb;

        FakeTenantDatabaseService() {
            super(null);
        }

        @Override
        public void createDatabase(String dbName) {
            this.createdDb = dbName;
        }

        @Override
        public void dropDatabase(String dbName) {
            this.droppedDb = dbName;
        }
    }

    private static class FakeTenantFlywayService extends TenantFlywayService {
        String migratedDb;
        boolean throwOnMigrate;

        FakeTenantFlywayService() {
            super(null);
        }

        @Override
        public void migrate(String tenantDbName) {
            if (throwOnMigrate) {
                throw new RuntimeException("Flyway connection timeout on tenant DB");
            }
            this.migratedDb = tenantDbName;
        }
    }

    private static class FakeTenantRoutingBridge extends TenantRoutingBridge {
        String calledDb;
        boolean throwOnCall;

        FakeTenantRoutingBridge() {
            super(null);
        }

        @Override
        public <T> T callInTenantTx(String tenantDbName, TransactionCallback<T> action) {
            if (throwOnCall) {
                throw new RuntimeException("Tenant DB connection pool exhausted");
            }
            this.calledDb = tenantDbName;
            return action.doInTransaction(new SimpleTransactionStatus());
        }
    }

    private static class FakeTenantDataSourceCache extends TenantDataSourceCache {
        String evictedDb;

        @Override
        public void evict(String dbName) {
            this.evictedDb = dbName;
        }
    }

    private static class FakeTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
