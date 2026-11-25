package com.company.GymVerse.tenantAdmin.service.dashboard;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.dashboard.FetchAdminDashboardRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.liveRoster.FetchLiveRosterRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.AdminDashboardResponse;
import com.company.GymVerse.tenantAdmin.service.liveRoster.FetchLiveRosterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.7: Admin Dashboard & Live Roster MySQL Integration Test")
class DashboardIntegrationTest {

    private static final String TENANT_DB = "gym_db_dash_test_001";

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
    private PlanTierRepository planTierRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMembershipRepository memberMembershipRepository;

    @Autowired
    private RevenueRepository revenueRepository;

    @Autowired
    private FetchAdminDashboardService fetchAdminDashboardService;

    @Autowired
    private FetchLiveRosterService fetchLiveRosterService;

    @BeforeEach
    void setUp() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
    }

    @Test
    @DisplayName("Executes FETCH_ADMIN_DASHBOARD and FETCH_LIVE_ROSTER end-to-end against MySQL")
    void endToEndDashboardAndLiveRosterAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            LocalDate today = LocalDate.now();
            TenantUser admin = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(9001L).firstName("Admin").lastName("Owner")
                    .email("admin@gym.com").role(TenantRole.TENANT_ADMIN).active(true).build());

            PlanTier elite = planTierRepository.findByNameIgnoreCase("ELITE").orElseThrow();
            PlanTier legendary = planTierRepository.findByNameIgnoreCase("LEGENDARY").orElseThrow();

            MembershipPlan elitePlan = membershipPlanRepository.saveAndFlush(MembershipPlan.builder()
                    .name("Elite Plan").planTier(elite)
                    .basePrice(new BigDecimal("100.00")).finalPrice(new BigDecimal("100.00"))
                    .durationDays(30).active(true).build());

            MembershipPlan legendaryPlan = membershipPlanRepository.saveAndFlush(MembershipPlan.builder()
                    .name("Legendary Plan").planTier(legendary)
                    .basePrice(new BigDecimal("200.00")).finalPrice(new BigDecimal("200.00"))
                    .durationDays(30).active(true).build());

            // 1. Active ELITE expiring in 5 days (within 7-day window)
            TenantUser u1 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(8001L).firstName("M1").lastName("One")
                    .email("m1@gym.com").role(TenantRole.MEMBER).active(true).build());
            Member m1 = memberRepository.saveAndFlush(Member.builder()
                    .user(u1).joinDate(today.minusDays(25)).gymId("MEM-D01").type(MemberType.REGULAR).build());
            MemberMembership mm1 = memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m1).plan(elitePlan).priceAtPurchase(new BigDecimal("100.00"))
                    .finalPrice(new BigDecimal("100.00")).paidAmount(new BigDecimal("100.00"))
                    .paymentStatus(PaymentStatus.PAID).startDate(today.minusDays(25)).endDate(today.plusDays(5))
                    .active(true).build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(mm1).amount(new BigDecimal("100.00")).paymentMode(PaymentMode.ONLINE)
                    .revenueType(RevenueType.NEW_MEMBERSHIP).receivedAt(Instant.now()).recordedByAdmin(admin).build());

            // 2. Active LEGENDARY expiring in 20 days (outside 7-day window)
            TenantUser u2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(8002L).firstName("M2").lastName("Two")
                    .email("m2@gym.com").role(TenantRole.MEMBER).active(true).build());
            Member m2 = memberRepository.saveAndFlush(Member.builder()
                    .user(u2).joinDate(today.minusDays(10)).gymId("MEM-D02").type(MemberType.REGULAR).build());
            MemberMembership mm2 = memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m2).plan(legendaryPlan).priceAtPurchase(new BigDecimal("200.00"))
                    .finalPrice(new BigDecimal("200.00")).paidAmount(new BigDecimal("200.00"))
                    .paymentStatus(PaymentStatus.PAID).startDate(today.minusDays(10)).endDate(today.plusDays(20))
                    .active(true).build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(mm2).amount(new BigDecimal("200.00")).paymentMode(PaymentMode.CASH)
                    .revenueType(RevenueType.RENEWAL).receivedAt(Instant.now()).recordedByAdmin(admin).build());

            // 3. Expired ELITE member
            TenantUser u3 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(8003L).firstName("M3").lastName("Three")
                    .email("m3@gym.com").role(TenantRole.MEMBER).active(true).build());
            Member m3 = memberRepository.saveAndFlush(Member.builder()
                    .user(u3).joinDate(today.minusDays(40)).gymId("MEM-D03").type(MemberType.REGULAR).build());
            memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m3).plan(elitePlan).priceAtPurchase(new BigDecimal("100.00"))
                    .finalPrice(new BigDecimal("100.00")).paidAmount(new BigDecimal("100.00"))
                    .paymentStatus(PaymentStatus.PAID).startDate(today.minusDays(40)).endDate(today.minusDays(10))
                    .active(true).build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(null).amount(new BigDecimal("50.00")).paymentMode(PaymentMode.CASH)
                    .revenueType(RevenueType.OTHER).receivedAt(Instant.now()).recordedByAdmin(admin).build());

            TenantAdminResponse dashRes = fetchAdminDashboardService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_ADMIN_DASHBOARD)
                    .fetchAdminDashboardRequest(FetchAdminDashboardRequest.builder().referenceDate(today).build())
                    .build());

            AdminDashboardResponse dash = dashRes.getAdminDashboardResponse();
            assertThat(dash.getTotalMembers()).isEqualTo(3L);
            assertThat(dash.getLiveMembers()).isZero();
            assertThat(dash.getActiveRate()).isEqualByComparingTo("66.67"); // 2/3 * 100
            assertThat(dash.getTotalRevenue()).isEqualByComparingTo("350.00");
            assertThat(dash.getActiveByTier()).extracting("tierName").containsExactly("ELITE", "LEGENDARY");
            assertThat(dash.getActiveByTier().get(0).getCount()).isEqualTo(1L);
            assertThat(dash.getActiveByTier().get(1).getCount()).isEqualTo(1L);
            assertThat(dash.getMembershipExpiryCard().getExpiringSoonCount()).isEqualTo(1L);
            assertThat(dash.getMembershipExpiryCard().getExpiringSoonWindowDays()).isEqualTo(7);
            assertThat(dash.getRevenueCard().getNewMembershipRevenue()).isEqualByComparingTo("100.00");
            assertThat(dash.getRevenueCard().getRenewalRevenue()).isEqualByComparingTo("200.00");
            assertThat(dash.getRevenueCard().getOtherRevenue()).isEqualByComparingTo("50.00");

            TenantAdminResponse rosterRes = fetchLiveRosterService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_LIVE_ROSTER)
                    .fetchLiveRosterRequest(FetchLiveRosterRequest.builder().membershipFilter("ALL").build())
                    .build());
            assertThat(rosterRes.getLiveRosterResponse().getTotalLiveCount()).isZero();
            assertThat(rosterRes.getLiveRosterResponse().getRoster().content()).isEmpty();
        });
    }
}
