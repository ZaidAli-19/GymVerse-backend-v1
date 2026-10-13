package com.company.GymVerse.persistence;

import com.company.GymVerse.models.central.AdminAuditLog;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuditEvent;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.central.enums.UserAccountStatus;
import com.company.GymVerse.repositories.central.AdminAuditLogRepository;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.SaasSubscriptionPlanRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional("centralTransactionManager")
class CentralPersistenceIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GymTenantRepository gymTenantRepository;

    @Autowired
    private UserTenantRoleRepository userTenantRoleRepository;

    @Autowired
    private SaasSubscriptionPlanRepository planRepository;

    @Autowired
    private GymSubscriptionRepository gymSubscriptionRepository;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("AC-1.7.2: UserAccount CRUD and unique email constraint verification")
    void testUserAccountCrudAndUniqueEmailConstraint() {
        String uniqueEmail = "admin." + UUID.randomUUID() + "@gymverse.com";
        UserAccount user = UserAccount.builder()
                .email(uniqueEmail)
                .password("$2a$10$abcdefghijklmnopqrstuvwxyz123456")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .status(UserAccountStatus.ACTIVE)
                .emailVerified(true)
                .isPlatformAdmin(false)
                .build();

        UserAccount savedUser = userAccountRepository.saveAndFlush(user);
        assertThat(savedUser.getId()).isNotNull();

        Optional<UserAccount> fetched = userAccountRepository.findByEmail(uniqueEmail);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getFirstName()).isEqualTo("John");

        // Duplicate email must violate unique constraint
        UserAccount duplicate = UserAccount.builder()
                .email(uniqueEmail)
                .password("someOtherHash")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+9876543210")
                .status(UserAccountStatus.ACTIVE)
                .emailVerified(false)
                .isPlatformAdmin(false)
                .build();

        assertThatThrownBy(() -> userAccountRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-1.7.2: GymTenant CRUD and unique gymCode and dbName constraints")
    void testGymTenantUniqueConstraints() {
        String code = "TEST" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String db = "gym_db_" + UUID.randomUUID().toString().substring(0, 8);

        GymTenant tenant = GymTenant.builder()
                .gymName("Iron Paradise Gym")
                .gymCode(code)
                .dbName(db)
                .address("123 Fitness St")
                .city("Metropolis")
                .contactEmail("contact@" + code.toLowerCase() + ".com")
                .phoneNumber("+15550001")
                .status(GymStatus.ACTIVE)
                .build();

        GymTenant savedTenant = gymTenantRepository.saveAndFlush(tenant);
        assertThat(savedTenant.getId()).isNotNull();

        Optional<GymTenant> fetched = gymTenantRepository.findByGymCode(code);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDbName()).isEqualTo(db);

        // Duplicate gymCode
        GymTenant duplicateCode = GymTenant.builder()
                .gymName("Duplicate Code Gym")
                .gymCode(code)
                .dbName("gym_db_other_" + UUID.randomUUID().toString().substring(0, 6))
                .address("456 Other St")
                .city("Metropolis")
                .contactEmail("other@test.com")
                .status(GymStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> gymTenantRepository.saveAndFlush(duplicateCode))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Duplicate dbName
        GymTenant duplicateDb = GymTenant.builder()
                .gymName("Duplicate DB Gym")
                .gymCode("NEW" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .dbName(db)
                .address("789 Another St")
                .city("Metropolis")
                .contactEmail("other2@test.com")
                .status(GymStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> gymTenantRepository.saveAndFlush(duplicateDb))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-1.7.2: UserTenantRole mapping with PRIMARY admin authority and unique user-gym-role constraint")
    void testUserTenantRoleAndAuthority() {
        String email = "owner." + UUID.randomUUID() + "@gymverse.com";
        UserAccount user = userAccountRepository.saveAndFlush(UserAccount.builder()
                .email(email)
                .password("hashedPass")
                .firstName("Facility")
                .lastName("Owner")
                .status(UserAccountStatus.ACTIVE)
                .build());

        String code = "ROLE" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        GymTenant tenant = gymTenantRepository.saveAndFlush(GymTenant.builder()
                .gymName("Role Test Gym")
                .gymCode(code)
                .dbName("gym_db_role_" + UUID.randomUUID().toString().substring(0, 8))
                .address("101 Role St")
                .city("Metropolis")
                .contactEmail("role@test.com")
                .status(GymStatus.ACTIVE)
                .build());

        UserTenantRole role = UserTenantRole.builder()
                .userAccount(user)
                .gymTenant(tenant)
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .status(TenantMembershipStatus.ACTIVE)
                .build();

        UserTenantRole savedRole = userTenantRoleRepository.saveAndFlush(role);
        assertThat(savedRole.getId()).isNotNull();

        Optional<UserTenantRole> fetched = userTenantRoleRepository
                .findByUserAccountIdAndGymTenantIdAndRole(user.getId(), tenant.getId(), TenantRole.TENANT_ADMIN);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getAdminAuthority()).isEqualTo(AdminAuthority.PRIMARY);

        // Duplicate user-gym-role violates uq_utr_user_gym_role
        UserTenantRole duplicateRole = UserTenantRole.builder()
                .userAccount(user)
                .gymTenant(tenant)
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.DELEGATED)
                .status(TenantMembershipStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> userTenantRoleRepository.saveAndFlush(duplicateRole))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("AC-1.7.2: SaasSubscriptionPlan and GymSubscription monetary precision (BigDecimal scale 2)")
    void testSaasSubscriptionPlanMonetaryPrecision() {
        BigDecimal planPrice = new BigDecimal("49.99");

        SaasSubscriptionPlan plan = SaasSubscriptionPlan.builder()
                .name("Pro Enterprise Tier " + UUID.randomUUID().toString().substring(0, 6))
                .price(planPrice)
                .billingCycle(BillingCycle.MONTHLY)
                .maxMembers(500)
                .maxTrainers(10)
                .active(true)
                .build();

        SaasSubscriptionPlan savedPlan = planRepository.saveAndFlush(plan);
        assertThat(savedPlan.getId()).isNotNull();

        SaasSubscriptionPlan fetchedPlan = planRepository.findById(savedPlan.getId()).orElseThrow();
        assertThat(fetchedPlan.getPrice()).isEqualByComparingTo(planPrice);
        assertThat(fetchedPlan.getPrice().scale()).isEqualTo(2);

        // Verify GymSubscription persistence with pricePaid
        String code = "SUB" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        GymTenant tenant = gymTenantRepository.saveAndFlush(GymTenant.builder()
                .gymName("Sub Gym")
                .gymCode(code)
                .dbName("gym_db_sub_" + UUID.randomUUID().toString().substring(0, 8))
                .address("202 Sub St")
                .city("Metropolis")
                .contactEmail("sub@test.com")
                .status(GymStatus.ACTIVE)
                .build());

        GymSubscription subscription = GymSubscription.builder()
                .gymTenant(tenant)
                .plan(savedPlan)
                .pricePaid(new BigDecimal("49.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(GymSubscriptionStatus.ACTIVE)
                .transactionReference("TXN-" + UUID.randomUUID())
                .build();

        GymSubscription savedSub = gymSubscriptionRepository.saveAndFlush(subscription);
        assertThat(savedSub.getId()).isNotNull();
        assertThat(savedSub.getPricePaid()).isEqualByComparingTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("AC-1.7.2: AdminAuditLog persistence and retrieval")
    void testAdminAuditLogPersistence() {
        String email = "actor." + UUID.randomUUID() + "@gymverse.com";
        UserAccount actor = userAccountRepository.saveAndFlush(UserAccount.builder()
                .email(email)
                .password("pass")
                .firstName("Actor")
                .lastName("Admin")
                .status(UserAccountStatus.ACTIVE)
                .build());

        String code = "LOG" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        GymTenant tenant = gymTenantRepository.saveAndFlush(GymTenant.builder()
                .gymName("Audit Gym")
                .gymCode(code)
                .dbName("gym_db_log_" + UUID.randomUUID().toString().substring(0, 8))
                .address("303 Audit St")
                .city("Metropolis")
                .contactEmail("audit@test.com")
                .status(GymStatus.ACTIVE)
                .build());

        AdminAuditLog log = AdminAuditLog.builder()
                .gymTenant(tenant)
                .actorUserAccount(actor)
                .action(AdminAuditEvent.APPOINT_DELEGATED_ADMIN)
                .metadata("{\"reason\":\"Appointed for floor supervision\"}")
                .build();

        AdminAuditLog savedLog = auditLogRepository.saveAndFlush(log);
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getTimestamp()).isNotNull();

        Optional<AdminAuditLog> fetched = auditLogRepository.findById(savedLog.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getAction()).isEqualTo(AdminAuditEvent.APPOINT_DELEGATED_ADMIN);
    }
}
