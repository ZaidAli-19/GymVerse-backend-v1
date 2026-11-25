package com.company.GymVerse.tenantAdmin.service.dashboard;

import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.attendance.service.AttendanceQueryStubImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.tenantAdmin.config.TenantOperationsProperties;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.dashboard.FetchAdminDashboardRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.liveRoster.FetchLiveRosterRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.AdminDashboardResponse;
import com.company.GymVerse.tenantAdmin.response.liveRoster.LiveRosterResponse;
import com.company.GymVerse.tenantAdmin.service.liveRoster.FetchLiveRosterService;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.7: Admin Dashboard & Live Roster Unit Tests")
class DashboardAndLiveRosterServiceTest {

    private AtomicLong totalMembers;
    private AtomicLong expiredMembers;
    private AtomicLong expiringSoonMembers;
    private List<Object[]> revenueRows;

    private FetchAdminDashboardService fetchAdminDashboardService;
    private FetchLiveRosterService fetchLiveRosterService;

    @BeforeEach
    void setUp() {
        totalMembers = new AtomicLong(10L);
        expiredMembers = new AtomicLong(2L);
        expiringSoonMembers = new AtomicLong(3L);
        revenueRows = List.of(
                new Object[]{RevenueType.NEW_MEMBERSHIP, new BigDecimal("500.00")},
                new Object[]{RevenueType.RENEWAL, new BigDecimal("300.00")},
                new Object[]{RevenueType.OTHER, new BigDecimal("150.00")}
        );

        List<PlanTier> tiers = List.of(
                PlanTier.builder().id(1L).name("ELITE").themeKey(PlanTheme.GOLDEN).defaultTier(true).build(),
                PlanTier.builder().id(2L).name("LEGENDARY").themeKey(PlanTheme.PURPLE).defaultTier(true).build(),
                PlanTier.builder().id(3L).name("SPARTAN").themeKey(PlanTheme.EMERALD_GREEN).defaultTier(false).build()
        );

        MemberRepository memberRepo = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class<?>[]{MemberRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "count" -> (args != null && args.length == 1 && args[0] instanceof Specification<?>)
                            ? expiredMembers.get()
                            : totalMembers.get();
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MemberMembershipRepository membershipRepo = (MemberMembershipRepository) Proxy.newProxyInstance(
                MemberMembershipRepository.class.getClassLoader(),
                new Class<?>[]{MemberMembershipRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "countActiveMembersGroupedByTier" -> List.<Object[]>of(
                            new Object[]{"ELITE", PlanTheme.GOLDEN, 5L},
                            new Object[]{"LEGENDARY", PlanTheme.PURPLE, 3L}
                    );
                    case "countExpiringSoonMembers" -> expiringSoonMembers.get();
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (p, m, args) -> "findAllByOrderByDefaultTierDescNameAsc".equals(m.getName()) ? tiers : false
        );

        RevenueRepository revenueRepo = (RevenueRepository) Proxy.newProxyInstance(
                RevenueRepository.class.getClassLoader(),
                new Class<?>[]{RevenueRepository.class},
                (p, m, args) -> "sumAmountGroupedByRevenueType".equals(m.getName()) ? revenueRows : null
        );

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (p, m, args) -> false
        );

        AttendanceQueryPort attendanceQueryPort = new AttendanceQueryStubImpl();
        TenantOperationsProperties properties = new TenantOperationsProperties();
        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();

        fetchAdminDashboardService = new FetchAdminDashboardService(
                memberRepo, membershipRepo, tierRepo, revenueRepo, attendanceQueryPort, properties, validations, mapper
        );
        fetchLiveRosterService = new FetchLiveRosterService(
                attendanceQueryPort, properties, validations, mapper
        );

        TenantContext.setCurrentTenant("gym_db_test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FETCH_ADMIN_DASHBOARD computes KPIs, activeRate, tier breakdown, revenue card, and expiry card cleanly")
    void fetchAdminDashboard_MetricsAndZeroGuard() {
        TenantAdminResponse res = fetchAdminDashboardService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_ADMIN_DASHBOARD)
                .fetchAdminDashboardRequest(FetchAdminDashboardRequest.builder()
                        .referenceDate(LocalDate.of(2026, 11, 25))
                        .build())
                .build());

        AdminDashboardResponse dashboard = res.getAdminDashboardResponse();
        assertThat(dashboard.getTotalMembers()).isEqualTo(10L);
        assertThat(dashboard.getLiveMembers()).isZero();
        assertThat(dashboard.getActiveRate()).isEqualByComparingTo("80.00");
        assertThat(dashboard.getTotalRevenue()).isEqualByComparingTo("950.00");

        assertThat(dashboard.getActiveByTier()).hasSize(3);
        assertThat(dashboard.getActiveByTier().get(0).getTierName()).isEqualTo("ELITE");
        assertThat(dashboard.getActiveByTier().get(0).getCount()).isEqualTo(5L);
        assertThat(dashboard.getActiveByTier().get(1).getTierName()).isEqualTo("LEGENDARY");
        assertThat(dashboard.getActiveByTier().get(1).getCount()).isEqualTo(3L);
        assertThat(dashboard.getActiveByTier().get(2).getTierName()).isEqualTo("SPARTAN");
        assertThat(dashboard.getActiveByTier().get(2).getCount()).isZero();

        assertThat(dashboard.getMembershipCard().getTotalMembers()).isEqualTo(10L);
        assertThat(dashboard.getMembershipCard().getTierBreakdown()).hasSize(3);

        assertThat(dashboard.getRevenueCard().getNewMembershipRevenue()).isEqualByComparingTo("500.00");
        assertThat(dashboard.getRevenueCard().getRenewalRevenue()).isEqualByComparingTo("300.00");
        assertThat(dashboard.getRevenueCard().getOtherRevenue()).isEqualByComparingTo("150.00");
        assertThat(dashboard.getRevenueCard().getTotalRevenue()).isEqualByComparingTo("950.00");

        assertThat(dashboard.getMembershipExpiryCard().getExpiringSoonCount()).isEqualTo(3L);
        assertThat(dashboard.getMembershipExpiryCard().getExpiringSoonWindowDays()).isEqualTo(7);
        assertThat(dashboard.getMembershipExpiryCard().getLiveExpiringSoonCount()).isZero();

        // Zero-member activeRate guard
        totalMembers.set(0L);
        expiredMembers.set(0L);
        AdminDashboardResponse emptyGym = fetchAdminDashboardService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_ADMIN_DASHBOARD)
                .build()).getAdminDashboardResponse();
        assertThat(emptyGym.getActiveRate()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("FETCH_LIVE_ROSTER delegates to AttendanceQueryPort stub and enforces filter & sort whitelists")
    void fetchLiveRoster_StubAndValidationGuards() {
        TenantAdminResponse res = fetchLiveRosterService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_LIVE_ROSTER)
                .fetchLiveRosterRequest(FetchLiveRosterRequest.builder()
                        .membershipFilter("TRIAL")
                        .search("alex")
                        .page(0)
                        .size(10)
                        .sortBy("elapsedMinutes")
                        .sortDirection("desc")
                        .build())
                .build());

        LiveRosterResponse live = res.getLiveRosterResponse();
        assertThat(live.getTotalLiveCount()).isZero();
        assertThat(live.getAvgTimeSpentMinutes()).isZero();
        assertThat(live.getLiveExpiredCount()).isZero();
        assertThat(live.getLiveTrialCount()).isZero();
        assertThat(live.getRoster().content()).isEmpty();
        assertThat(live.getRoster().totalElements()).isZero();

        assertThatThrownBy(() -> fetchLiveRosterService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_LIVE_ROSTER)
                .fetchLiveRosterRequest(FetchLiveRosterRequest.builder().membershipFilter("VIP").build())
                .build()))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> fetchLiveRosterService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_LIVE_ROSTER)
                .fetchLiveRosterRequest(FetchLiveRosterRequest.builder().sortBy("secretColumn").build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }
}
