package com.company.GymVerse.persistence;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    private static final String TENANT_1 = "gym_db_test_001";
    private static final String TENANT_2 = "gym_db_test_002";

    @Autowired
    private TenantDatabaseService tenantDatabaseService;

    @Autowired
    private TenantFlywayService tenantFlywayService;

    @Autowired
    private TenantDataSourceCache tenantDataSourceCache;

    @Autowired
    private TenantRoutingBridge routingBridge;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // Clean any lingering test state
        teardownTenants();

        // Dynamically provision two independent physical databases
        tenantDatabaseService.createDatabase(TENANT_1);
        tenantDatabaseService.createDatabase(TENANT_2);

        // Apply tenant migrations to both physical schemas
        tenantFlywayService.migrate(TENANT_1);
        tenantFlywayService.migrate(TENANT_2);
    }

    @AfterEach
    void tearDown() {
        teardownTenants();
        TenantContext.clear();
    }

    private void teardownTenants() {
        tenantDataSourceCache.evict(TENANT_1);
        tenantDataSourceCache.evict(TENANT_2);
        tenantDatabaseService.dropDatabase(TENANT_1);
        tenantDatabaseService.dropDatabase(TENANT_2);
    }

    @Test
    @DisplayName("AC-1.7.4: Strict physical database isolation between tenant schemas")
    void testPhysicalTenantDatabaseIsolation() {
        // Step 1: Create a member in Tenant 1
        routingBridge.runInTenant(TENANT_1, () -> {
            TenantUser u1 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(2001L)
                    .firstName("Tenant1")
                    .lastName("User")
                    .email("user@gym1.com")
                    .role(TenantRole.MEMBER)
                    .active(true)
                    .build());

            memberRepository.saveAndFlush(Member.builder()
                    .user(u1)
                    .gymId("GYM1-MEMBER")
                    .joinDate(LocalDate.now())
                    .independent(true)
                    .type("FULL")
                    .build());
        });

        // Step 2: Switch to Tenant 2 and verify zero members exist (complete isolation)
        routingBridge.runInTenant(TENANT_2, () -> {
            List<Member> membersInTenant2 = memberRepository.findAll();
            assertThat(membersInTenant2)
                    .as("Tenant 2 database must not see any members from Tenant 1")
                    .isEmpty();

            List<TenantUser> usersInTenant2 = tenantUserRepository.findAll();
            assertThat(usersInTenant2)
                    .as("Tenant 2 database must not see any users from Tenant 1")
                    .isEmpty();

            // Step 3: Insert a distinct member in Tenant 2
            TenantUser u2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(2002L)
                    .firstName("Tenant2")
                    .lastName("User")
                    .email("user@gym2.com")
                    .role(TenantRole.MEMBER)
                    .active(true)
                    .build());

            memberRepository.saveAndFlush(Member.builder()
                    .user(u2)
                    .gymId("GYM2-MEMBER")
                    .joinDate(LocalDate.now())
                    .independent(true)
                    .type("FULL")
                    .build());
        });

        // Step 4: Switch back to Tenant 1 and verify it only sees its own member
        routingBridge.runInTenant(TENANT_1, () -> {
            List<Member> membersInTenant1 = memberRepository.findAll();
            assertThat(membersInTenant1)
                    .as("Tenant 1 must contain exactly 1 member")
                    .hasSize(1);
            assertThat(membersInTenant1.get(0).getGymId()).isEqualTo("GYM1-MEMBER");
        });

        // Step 5: Switch to Tenant 2 and verify it only sees its own member
        routingBridge.runInTenant(TENANT_2, () -> {
            List<Member> membersInTenant2 = memberRepository.findAll();
            assertThat(membersInTenant2)
                    .as("Tenant 2 must contain exactly 1 member")
                    .hasSize(1);
            assertThat(membersInTenant2.get(0).getGymId()).isEqualTo("GYM2-MEMBER");
        });
    }
}
