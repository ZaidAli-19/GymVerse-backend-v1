package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.GymFacilityRepository;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchMemberDetailsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMembersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.members.MembersSummaryResponse;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.4: Members JPA Specification, Summary & Details MySQL Integration Test")
class MembersSpecificationIntegrationTest {

    private static final String TENANT_DB = "gym_db_members_test_001";

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
    private GymFacilityRepository gymFacilityRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMembershipRepository memberMembershipRepository;

    @Autowired
    private RevenueRepository revenueRepository;

    @Autowired
    private FetchMembersSummaryService fetchMembersSummaryService;

    @Autowired
    private FetchMembersService fetchMembersService;

    @Autowired
    private FetchMemberDetailsService fetchMemberDetailsService;

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
    @DisplayName("Executes FETCH_MEMBERS_SUMMARY, MemberSpecification filters/search/sorts, and FETCH_MEMBER_DETAILS against MySQL")
    void endToEndMembersDirectoryAndDetailsAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            LocalDate today = LocalDate.now();
            TenantUser admin = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(9001L).firstName("Admin").lastName("Owner")
                    .email("admin@gym.com").role(TenantRole.TENANT_ADMIN).active(true).build());

            PlanTier eliteTier = planTierRepository.findByNameIgnoreCase("ELITE").orElseThrow();
            GymFacility weights = gymFacilityRepository.saveAndFlush(GymFacility.builder().name("Weightlifting").build());
            GymFacility sauna = gymFacilityRepository.saveAndFlush(GymFacility.builder().name("Sauna").build());

            MembershipPlan elitePlan = membershipPlanRepository.saveAndFlush(MembershipPlan.builder()
                    .name("Elite Pro")
                    .planTier(eliteTier)
                    .facilities(Set.of(weights, sauna))
                    .basePrice(new BigDecimal("120.00"))
                    .finalPrice(new BigDecimal("120.00"))
                    .durationDays(30)
                    .trial(false)
                    .active(true)
                    .build());

            MembershipPlan trialPlan = membershipPlanRepository.saveAndFlush(MembershipPlan.builder()
                    .name("Starter Trial")
                    .planTier(eliteTier)
                    .facilities(Set.of(weights))
                    .basePrice(BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .durationDays(7)
                    .trial(true)
                    .active(true)
                    .build());

            // 1. Active member with 2 revenue payments (50 + 70 = 120)
            TenantUser u1 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(5001L).firstName("Alice").lastName("Morgan")
                    .email("alice@gym.com").phoneNumber("555-1111").role(TenantRole.MEMBER).active(true).build());
            Member m1 = memberRepository.saveAndFlush(Member.builder()
                    .user(u1).joinDate(today.minusDays(10)).gymId("MEM-10000001")
                    .type(MemberType.REGULAR).build());
            MemberMembership mm1 = memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m1).plan(elitePlan)
                    .priceAtPurchase(new BigDecimal("120.00")).finalPrice(new BigDecimal("120.00"))
                    .paidAmount(new BigDecimal("120.00")).paymentStatus(PaymentStatus.PAID)
                    .startDate(today.minusDays(10)).endDate(today.plusDays(20))
                    .active(true).trial(false).build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(mm1).amount(new BigDecimal("50.00")).paymentMode(PaymentMode.CASH)
                    .revenueType(RevenueType.NEW_MEMBERSHIP).receivedAt(Instant.now()).recordedByAdmin(admin).build());
            revenueRepository.saveAndFlush(Revenue.builder()
                    .memberMembership(mm1).amount(new BigDecimal("70.00")).paymentMode(PaymentMode.ONLINE)
                    .revenueType(RevenueType.NEW_MEMBERSHIP).receivedAt(Instant.now()).recordedByAdmin(admin).build());

            // 2. Date-based trial member (trialEndDate >= today, no trial flag on membership)
            TenantUser u2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(5002L).firstName("Bob").lastName("Stone")
                    .email("bob@gym.com").phoneNumber("555-2222").role(TenantRole.MEMBER).active(true).build());
            Member m2 = memberRepository.saveAndFlush(Member.builder()
                    .user(u2).joinDate(today.minusDays(2)).gymId("MEM-10000002")
                    .type(MemberType.TRIAL).trialEndDate(today.plusDays(4)).build());
            memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m2).plan(elitePlan)
                    .priceAtPurchase(new BigDecimal("120.00")).finalPrice(new BigDecimal("120.00"))
                    .paidAmount(BigDecimal.ZERO).paymentStatus(PaymentStatus.PAID)
                    .startDate(today.minusDays(2)).endDate(today.plusDays(4))
                    .active(true).trial(false).build());

            // 3. Flag-based trial member (trialEndDate = null, membership.trial = true)
            TenantUser u3 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(5003L).firstName("Clara").lastName("Vance")
                    .email("clara@gym.com").phoneNumber("555-3333").role(TenantRole.MEMBER).active(true).build());
            Member m3 = memberRepository.saveAndFlush(Member.builder()
                    .user(u3).joinDate(today.minusDays(1)).gymId("MEM-10000003")
                    .type(MemberType.TRIAL).build());
            memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m3).plan(trialPlan)
                    .priceAtPurchase(BigDecimal.ZERO).finalPrice(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO).paymentStatus(PaymentStatus.PAID)
                    .startDate(today.minusDays(1)).endDate(today.plusDays(6))
                    .active(true).trial(true).build());

            // 4. Expired member
            TenantUser u4 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(5004L).firstName("Derek").lastName("Hale")
                    .email("derek@gym.com").phoneNumber("555-4444").role(TenantRole.MEMBER).active(true).build());
            Member m4 = memberRepository.saveAndFlush(Member.builder()
                    .user(u4).joinDate(today.minusDays(45)).gymId("MEM-10000004")
                    .type(MemberType.REGULAR).trialEndDate(today.minusDays(38)).build());
            memberMembershipRepository.saveAndFlush(MemberMembership.builder()
                    .member(m4).plan(elitePlan)
                    .priceAtPurchase(new BigDecimal("120.00")).finalPrice(new BigDecimal("120.00"))
                    .paidAmount(new BigDecimal("120.00")).paymentStatus(PaymentStatus.PAID)
                    .startDate(today.minusDays(40)).endDate(today.minusDays(10))
                    .active(true).trial(false).build());

            // 1. Summary check
            TenantAdminResponse summaryRes = fetchMembersSummaryService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS_SUMMARY)
                    .build());
            MembersSummaryResponse summary = summaryRes.getMembersSummaryResponse();
            assertThat(summary.getTotalMembersCount()).isEqualTo(4L);
            assertThat(summary.getLiveCount()).isZero();
            assertThat(summary.getTrialCount()).isEqualTo(2L);
            assertThat(summary.getExpiredCount()).isEqualTo(1L);

            // 2. Directory filters & sort by user.firstName
            TenantAdminResponse trialRes = fetchMembersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                    .fetchMembersRequest(FetchMembersRequest.builder()
                            .statusFilter(MemberFilterStatus.TRIAL)
                            .sortBy("user.firstName")
                            .sortDirection("desc")
                            .build())
                    .build());
            assertThat(trialRes.getMembersPageResponse().totalElements()).isEqualTo(2L);
            assertThat(trialRes.getMembersPageResponse().content())
                    .extracting("fullName")
                    .containsExactly("Clara Vance", "Bob Stone");

            TenantAdminResponse expiredRes = fetchMembersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                    .fetchMembersRequest(FetchMembersRequest.builder()
                            .statusFilter(MemberFilterStatus.EXPIRED)
                            .build())
                    .build());
            assertThat(expiredRes.getMembersPageResponse().totalElements()).isEqualTo(1L);
            assertThat(expiredRes.getMembersPageResponse().content().get(0).getMembershipStatus())
                    .isEqualTo(MemberMembershipStatus.EXPIRED);

            TenantAdminResponse liveRes = fetchMembersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                    .fetchMembersRequest(FetchMembersRequest.builder()
                            .statusFilter(MemberFilterStatus.LIVE)
                            .build())
                    .build());
            assertThat(liveRes.getMembersPageResponse().totalElements()).isZero();

            TenantAdminResponse searchRes = fetchMembersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                    .fetchMembersRequest(FetchMembersRequest.builder()
                            .search("MEM-10000001")
                            .sortBy("user.firstName")
                            .build())
                    .build());
            assertThat(searchRes.getMembersPageResponse().totalElements()).isEqualTo(1L);
            assertThat(searchRes.getMembersPageResponse().content().get(0).getEmail()).isEqualTo("alice@gym.com");

            // 3. Member Details check
            TenantAdminResponse detailsRes = fetchMemberDetailsService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBER_DETAILS)
                    .fetchMemberDetailsRequest(FetchMemberDetailsRequest.builder().memberId(m1.getId()).build())
                    .build());
            MemberDetailsResponse details = detailsRes.getMemberDetailsResponse();
            assertThat(details.getQuickStats().getTotalAmountPaid()).isEqualByComparingTo("120.00");
            assertThat(details.getQuickStats().getDaysLeftUntilExpiry()).isEqualTo(20L);
            assertThat(details.getMembershipDetails().getThemeKey()).isEqualTo(PlanTheme.GOLDEN);
            assertThat(details.getMembershipDetails().getFacilities()).containsExactly("Sauna", "Weightlifting");
            assertThat(details.getContactDetails().getGymId()).isEqualTo("MEM-10000001");
        });

        TenantContext.setCurrentTenant(TENANT_DB);
        try {
            assertThatThrownBy(() -> fetchMemberDetailsService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBER_DETAILS)
                    .fetchMemberDetailsRequest(FetchMemberDetailsRequest.builder().memberId(99999L).build())
                    .build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        } finally {
            TenantContext.clear();
        }
    }
}
