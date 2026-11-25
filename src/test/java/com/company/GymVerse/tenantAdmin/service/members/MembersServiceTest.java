package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.attendance.service.AttendanceQueryStubImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.tenantAdmin.config.TenantOperationsProperties;
import com.company.GymVerse.tenantAdmin.helper.MembershipCalculationHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchMemberDetailsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMembersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberListItemResponse;
import com.company.GymVerse.tenantAdmin.response.members.MembersSummaryResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.4: Members Directory, Mini-Dashboard & Member Details Unit Tests")
class MembersServiceTest {

    private Map<Long, Member> membersById;
    private Map<Long, List<MemberMembership>> membershipsByMemberId;
    private Map<Long, BigDecimal> totalPaidByMemberId;
    private AtomicInteger specCountCalls;
    private AtomicInteger liveIdsCalls;

    private FetchMembersSummaryService fetchMembersSummaryService;
    private FetchMembersService fetchMembersService;
    private FetchMemberDetailsService fetchMemberDetailsService;

    @BeforeEach
    void setUp() {
        membersById = new LinkedHashMap<>();
        membershipsByMemberId = new LinkedHashMap<>();
        totalPaidByMemberId = new LinkedHashMap<>();
        specCountCalls = new AtomicInteger(0);
        liveIdsCalls = new AtomicInteger(0);

        LocalDate today = LocalDate.now();
        PlanTier eliteTier = PlanTier.builder().id(1L).name("ELITE").themeKey(PlanTheme.GOLDEN).defaultTier(true).build();
        PlanTier customTier = PlanTier.builder().id(2L).name("SPARTAN").themeKey(PlanTheme.EMERALD_GREEN).defaultTier(false).build();

        MembershipPlan elitePlan = MembershipPlan.builder()
                .id(1L)
                .name("Elite Monthly")
                .planTier(eliteTier)
                .facilities(Set.of(
                        GymFacility.builder().id(1L).name("Weightlifting").build(),
                        GymFacility.builder().id(2L).name("Cardio").build()
                ))
                .basePrice(new BigDecimal("100.00"))
                .finalPrice(new BigDecimal("100.00"))
                .durationDays(30)
                .trial(false)
                .active(true)
                .build();

        MembershipPlan trialPlan = MembershipPlan.builder()
                .id(2L)
                .name("7-Day Trial")
                .planTier(customTier)
                .facilities(Set.of(GymFacility.builder().id(2L).name("Cardio").build()))
                .basePrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .durationDays(7)
                .trial(true)
                .active(true)
                .build();

        // Member 1: Active regular member with partial payment and total revenue 140.00
        Member activeMember = Member.builder()
                .id(1L)
                .gymId("MEM-ACTIVE01")
                .joinDate(today.minusDays(10))
                .type(MemberType.REGULAR)
                .user(TenantUser.builder()
                        .id(11L).centralUserId(1001L).firstName("Alex").lastName("Rivera")
                        .email("alex@gym.com").phoneNumber("555-0101").role(TenantRole.MEMBER).build())
                .build();
        membersById.put(1L, activeMember);
        membershipsByMemberId.put(1L, List.of(MemberMembership.builder()
                .id(101L)
                .member(activeMember)
                .plan(elitePlan)
                .priceAtPurchase(new BigDecimal("100.00"))
                .finalPrice(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("60.00"))
                .paymentStatus(PaymentStatus.PARTIALLY_PAID)
                .startDate(today.minusDays(10))
                .endDate(today.plusDays(20))
                .active(true)
                .trial(false)
                .build()));
        totalPaidByMemberId.put(1L, new BigDecimal("140.00"));

        // Member 2: Trial member via members.trialEndDate (no trial flag on membership)
        Member dateTrialMember = Member.builder()
                .id(2L)
                .gymId("MEM-TRIAL01")
                .joinDate(today.minusDays(2))
                .type(MemberType.TRIAL)
                .trialEndDate(today.plusDays(5))
                .user(TenantUser.builder()
                        .id(12L).centralUserId(1002L).firstName("Bella").lastName("Chen")
                        .email("bella@gym.com").phoneNumber("555-0102").role(TenantRole.MEMBER).build())
                .build();
        membersById.put(2L, dateTrialMember);
        membershipsByMemberId.put(2L, List.of(MemberMembership.builder()
                .id(102L)
                .member(dateTrialMember)
                .plan(elitePlan)
                .priceAtPurchase(new BigDecimal("100.00"))
                .finalPrice(new BigDecimal("100.00"))
                .paidAmount(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.PAID)
                .startDate(today.minusDays(2))
                .endDate(today.plusDays(5))
                .active(true)
                .trial(false)
                .build()));

        // Member 3: Trial member via isTrial = true flag (trialEndDate = null)
        Member flagTrialMember = Member.builder()
                .id(3L)
                .gymId("MEM-TRIAL02")
                .joinDate(today.minusDays(1))
                .type(MemberType.TRIAL)
                .trialEndDate(null)
                .user(TenantUser.builder()
                        .id(13L).centralUserId(1003L).firstName("Carlos").lastName("Diaz")
                        .email("carlos@gym.com").phoneNumber("555-0103").role(TenantRole.MEMBER).build())
                .build();
        membersById.put(3L, flagTrialMember);
        membershipsByMemberId.put(3L, List.of(MemberMembership.builder()
                .id(103L)
                .member(flagTrialMember)
                .plan(trialPlan)
                .priceAtPurchase(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.PAID)
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(6))
                .active(true)
                .trial(true)
                .build()));

        // Member 4: Expired member
        Member expiredMember = Member.builder()
                .id(4L)
                .gymId("MEM-EXP0001")
                .joinDate(today.minusDays(40))
                .type(MemberType.REGULAR)
                .trialEndDate(today.minusDays(33))
                .user(TenantUser.builder()
                        .id(14L).centralUserId(1004L).firstName("Diana").lastName("Prince")
                        .email("diana@gym.com").phoneNumber("555-0104").role(TenantRole.MEMBER).build())
                .build();
        membersById.put(4L, expiredMember);
        membershipsByMemberId.put(4L, List.of(MemberMembership.builder()
                .id(104L)
                .member(expiredMember)
                .plan(elitePlan)
                .priceAtPurchase(new BigDecimal("100.00"))
                .finalPrice(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("100.00"))
                .paymentStatus(PaymentStatus.PAID)
                .startDate(today.minusDays(35))
                .endDate(today.minusDays(5))
                .active(true)
                .trial(false)
                .build()));

        MemberRepository memberRepo = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class<?>[]{MemberRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "count" -> {
                        if (args != null && args.length == 1 && args[0] instanceof Specification<?>) {
                            int idx = specCountCalls.incrementAndGet();
                            // FetchMembersSummaryService queries EXPIRED first, then TRIAL
                            yield idx % 2 == 1 ? 1L : 2L;
                        }
                        yield (long) membersById.size();
                    }
                    case "findById" -> Optional.ofNullable(membersById.get((Long) args[0]));
                    case "findAll" -> {
                        if (args != null && args.length == 2 && args[0] instanceof Specification<?> && args[1] instanceof Pageable pageable) {
                            List<Member> all = new ArrayList<>(membersById.values());
                            if (liveIdsCalls.get() > 0) {
                                // LIVE stub returns empty list -> empty page
                                yield new PageImpl<>(List.of(), pageable, 0);
                            }
                            yield new PageImpl<>(all, pageable, all.size());
                        }
                        yield new ArrayList<>(membersById.values());
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MemberMembershipRepository membershipRepo = (MemberMembershipRepository) Proxy.newProxyInstance(
                MemberMembershipRepository.class.getClassLoader(),
                new Class<?>[]{MemberMembershipRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "findAllWithPlanByMemberIdIn" -> {
                        @SuppressWarnings("unchecked")
                        Collection<Long> ids = (Collection<Long>) args[0];
                        List<MemberMembership> result = new ArrayList<>();
                        for (Long id : ids) {
                            result.addAll(membershipsByMemberId.getOrDefault(id, List.of()));
                        }
                        yield result;
                    }
                    case "findAllWithPlanAndFacilitiesByMemberId" ->
                            membershipsByMemberId.getOrDefault((Long) args[0], List.of());
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        RevenueRepository revenueRepo = (RevenueRepository) Proxy.newProxyInstance(
                RevenueRepository.class.getClassLoader(),
                new Class<?>[]{RevenueRepository.class},
                (p, m, args) -> "sumTotalPaidByMemberId".equals(m.getName())
                        ? totalPaidByMemberId.getOrDefault((Long) args[0], BigDecimal.ZERO)
                        : null
        );

        AttendanceQueryPort attendanceQueryPort = new AttendanceQueryStubImpl() {
            @Override
            public List<Long> getLiveMemberIds() {
                liveIdsCalls.incrementAndGet();
                return super.getLiveMemberIds();
            }

            @Override
            public LiveAttendanceSummary getLiveAttendanceSummary(final LocalDate expiringSoonCutoff) {
                return super.getLiveAttendanceSummary(expiringSoonCutoff);
            }

            @Override
            public MemberAttendanceSnapshot getMemberAttendanceSnapshot(final Long memberId) {
                return super.getMemberAttendanceSnapshot(memberId);
            }
        };

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (p, m, args) -> false
        );
        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (p, m, args) -> false
        );

        TenantOperationsProperties properties = new TenantOperationsProperties();
        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();

        fetchMembersSummaryService = new FetchMembersSummaryService(
                memberRepo, attendanceQueryPort, properties, validations, mapper
        );
        fetchMembersService = new FetchMembersService(
                memberRepo, membershipRepo, attendanceQueryPort, validations, mapper
        );
        fetchMemberDetailsService = new FetchMemberDetailsService(
                memberRepo, membershipRepo, revenueRepo, attendanceQueryPort, validations, mapper
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
    @DisplayName("FETCH_MEMBERS_SUMMARY returns total, stubbed live=0, expired, and dual-trial counts")
    void fetchMembersSummary_Success() {
        TenantAdminResponse response = fetchMembersSummaryService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS_SUMMARY)
                .build());

        MembersSummaryResponse summary = response.getMembersSummaryResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(summary.getTotalMembersCount()).isEqualTo(4L);
        assertThat(summary.getLiveCount()).isZero();
        assertThat(summary.getExpiredCount()).isEqualTo(1L);
        assertThat(summary.getTrialCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Dual trial & expiry calculation helper classifies date trials, flag trials, active, and expired accurately")
    void dualTrialAndStatusHelper_Verification() {
        LocalDate today = LocalDate.now();

        assertThat(MembershipCalculationHelper.resolveMemberStatus(
                today.plusDays(3), null, today)).isEqualTo(MemberMembershipStatus.TRIAL);
        assertThat(MembershipCalculationHelper.resolveMemberDaysRemaining(
                today.plusDays(3), null, today)).isEqualTo(3L);

        MemberMembership flagTrialMembership = MemberMembership.builder()
                .active(true).trial(true).startDate(today).endDate(today.plusDays(7)).build();
        assertThat(MembershipCalculationHelper.resolveMemberStatus(
                null, flagTrialMembership, today)).isEqualTo(MemberMembershipStatus.TRIAL);

        MemberMembership activePaidMembership = MemberMembership.builder()
                .active(true).trial(false).startDate(today).endDate(today.plusDays(15)).build();
        assertThat(MembershipCalculationHelper.resolveMemberStatus(
                null, activePaidMembership, today)).isEqualTo(MemberMembershipStatus.ACTIVE);
        assertThat(MembershipCalculationHelper.resolveMemberDaysRemaining(
                null, activePaidMembership, today)).isEqualTo(15L);

        MemberMembership expiredMembership = MemberMembership.builder()
                .active(true).trial(false).startDate(today.minusDays(30)).endDate(today.minusDays(1)).build();
        assertThat(MembershipCalculationHelper.resolveMemberStatus(
                today.minusDays(2), expiredMembership, today)).isEqualTo(MemberMembershipStatus.EXPIRED);
        assertThat(MembershipCalculationHelper.resolveMemberDaysRemaining(
                today.minusDays(2), expiredMembership, today)).isZero();
    }

    @Test
    @DisplayName("FETCH_MEMBERS maps statuses, live filter stub, whitelisted sorts, and rejects invalid sortBy")
    void fetchMembers_MappingLiveStubAndSortGuards() {
        TenantAdminResponse allRes = fetchMembersService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                .fetchMembersRequest(FetchMembersRequest.builder()
                        .statusFilter(MemberFilterStatus.ALL)
                        .page(0)
                        .size(10)
                        .sortBy("user.firstName")
                        .sortDirection("asc")
                        .build())
                .build());

        List<MemberListItemResponse> items = allRes.getMembersPageResponse().content();
        assertThat(items).hasSize(4);

        MemberListItemResponse activeRow = items.get(0);
        assertThat(activeRow.getFullName()).isEqualTo("Alex Rivera");
        assertThat(activeRow.getPlanName()).isEqualTo("Elite Monthly");
        assertThat(activeRow.getTier()).isEqualTo("ELITE");
        assertThat(activeRow.getThemeKey()).isEqualTo(PlanTheme.GOLDEN);
        assertThat(activeRow.getMembershipStatus()).isEqualTo(MemberMembershipStatus.ACTIVE);
        assertThat(activeRow.getDaysRemaining()).isEqualTo(20L);
        assertThat(activeRow.getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(activeRow.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(activeRow.isCheckedInToday()).isFalse();

        // Both date-based trial and flag-based trial classify as TRIAL
        assertThat(items.get(1).getMembershipStatus()).isEqualTo(MemberMembershipStatus.TRIAL);
        assertThat(items.get(2).getMembershipStatus()).isEqualTo(MemberMembershipStatus.TRIAL);
        // Expired member classifies as EXPIRED with 0 remaining days
        assertThat(items.get(3).getMembershipStatus()).isEqualTo(MemberMembershipStatus.EXPIRED);
        assertThat(items.get(3).getDaysRemaining()).isZero();

        // LIVE filter delegates to AttendanceQueryPort and returns empty page safely
        TenantAdminResponse liveRes = fetchMembersService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                .fetchMembersRequest(FetchMembersRequest.builder()
                        .statusFilter(MemberFilterStatus.LIVE)
                        .build())
                .build());
        assertThat(liveIdsCalls.get()).isEqualTo(1);
        assertThat(liveRes.getMembersPageResponse().content()).isEmpty();

        // Non-whitelisted sortBy rejected
        assertThatThrownBy(() -> fetchMembersService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBERS)
                .fetchMembersRequest(FetchMembersRequest.builder().sortBy("secretColumn").build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("FETCH_MEMBER_DETAILS returns all 4 UI cards and throws ResourceNotFoundException for unknown ID")
    void fetchMemberDetails_FourCardsAndGuards() {
        TenantAdminResponse response = fetchMemberDetailsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBER_DETAILS)
                .fetchMemberDetailsRequest(FetchMemberDetailsRequest.builder().memberId(1L).build())
                .build());

        MemberDetailsResponse details = response.getMemberDetailsResponse();
        assertThat(details.getAttendanceActivity().getTotalVisitCount()).isZero();
        assertThat(details.getAttendanceActivity().getCurrentStreak()).isZero();
        assertThat(details.getAttendanceActivity().getDailyAttendance().isCheckedIn()).isFalse();
        assertThat(details.getAttendanceActivity().getDailyAttendance().getCheckInTime()).isNull();
        assertThat(details.getAttendanceActivity().getDailyAttendance().getElapsedMinutes()).isNull();

        assertThat(details.getQuickStats().getTotalAmountPaid()).isEqualByComparingTo("140.00");
        assertThat(details.getQuickStats().getDaysLeftUntilExpiry()).isEqualTo(20L);

        assertThat(details.getMembershipDetails().getPlanName()).isEqualTo("Elite Monthly");
        assertThat(details.getMembershipDetails().getTier()).isEqualTo("ELITE");
        assertThat(details.getMembershipDetails().getThemeKey()).isEqualTo(PlanTheme.GOLDEN);
        assertThat(details.getMembershipDetails().getFacilities()).containsExactly("Cardio", "Weightlifting");
        assertThat(details.getMembershipDetails().getRemainingDays()).isEqualTo(20L);
        assertThat(details.getMembershipDetails().getPaidAmount()).isEqualByComparingTo("60.00");
        assertThat(details.getMembershipDetails().getFinalPrice()).isEqualByComparingTo("100.00");
        assertThat(details.getMembershipDetails().getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(details.getMembershipDetails().isTrial()).isFalse();
        assertThat(details.getMembershipDetails().getMembershipStatus()).isEqualTo(MemberMembershipStatus.ACTIVE);

        assertThat(details.getContactDetails().getMemberId()).isEqualTo(1L);
        assertThat(details.getContactDetails().getGymId()).isEqualTo("MEM-ACTIVE01");
        assertThat(details.getContactDetails().getFirstName()).isEqualTo("Alex");
        assertThat(details.getContactDetails().getLastName()).isEqualTo("Rivera");
        assertThat(details.getContactDetails().getEmail()).isEqualTo("alex@gym.com");
        assertThat(details.getContactDetails().getPhoneNumber()).isEqualTo("555-0101");

        assertThatThrownBy(() -> fetchMemberDetailsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBER_DETAILS)
                .fetchMemberDetailsRequest(FetchMemberDetailsRequest.builder().memberId(999L).build())
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> fetchMemberDetailsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_MEMBER_DETAILS)
                .fetchMemberDetailsRequest(FetchMemberDetailsRequest.builder().memberId(0L).build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }
}
