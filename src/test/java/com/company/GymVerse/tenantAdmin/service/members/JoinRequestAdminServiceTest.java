package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ForbiddenException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.tenantAdmin.helper.JoinRequestHelper;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.ApproveJoinRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchJoinRequestsRequest;
import com.company.GymVerse.tenantAdmin.request.members.RejectJoinRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.members.ApproveJoinRequestResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.3: Join Requests Review & Approval Unit Tests")
class JoinRequestAdminServiceTest {

    private Map<Long, JoinRequest> joinRequestsById;
    private Map<Long, MembershipPlan> plansById;
    private Map<Long, TenantUser> usersById;
    private Map<Long, Member> membersById;
    private Map<Long, MemberMembership> membershipsById;
    private Map<Long, Revenue> revenuesById;
    private Map<Long, UserAccount> centralUsersById;
    private Map<Long, GymTenant> centralTenantsById;
    private List<UserTenantRole> centralRoles;
    private GymSubscription activeSubscription;
    private AtomicBoolean failCentralRoleSave;

    private FetchJoinRequestsService fetchJoinRequestsService;
    private ApproveJoinRequestService approveJoinRequestService;
    private RejectJoinRequestService rejectJoinRequestService;

    @BeforeEach
    void setUp() {
        joinRequestsById = new LinkedHashMap<>();
        plansById = new LinkedHashMap<>();
        usersById = new LinkedHashMap<>();
        membersById = new LinkedHashMap<>();
        membershipsById = new LinkedHashMap<>();
        revenuesById = new LinkedHashMap<>();
        centralUsersById = new LinkedHashMap<>();
        centralTenantsById = new LinkedHashMap<>();
        centralRoles = new ArrayList<>();
        failCentralRoleSave = new AtomicBoolean(false);

        AtomicLong userSeq = new AtomicLong(100);
        AtomicLong memberSeq = new AtomicLong(1);
        AtomicLong membershipSeq = new AtomicLong(1);
        AtomicLong revenueSeq = new AtomicLong(1);
        AtomicLong roleSeq = new AtomicLong(1);

        TenantUser adminUser = TenantUser.builder()
                .id(1L).centralUserId(9001L).firstName("Admin").lastName("Owner")
                .email("admin@gym.com").role(TenantRole.TENANT_ADMIN).active(true).build();
        usersById.put(1L, adminUser);

        GymTenant tenant = GymTenant.builder()
                .id(10L).gymCode("IRON-01").gymName("Iron Gym").dbName("gym_db_10").status(GymStatus.ACTIVE).build();
        centralTenantsById.put(10L, tenant);

        SaasSubscriptionPlan saasPlan = SaasSubscriptionPlan.builder()
                .id(1L).name("Pro").maxMembers(5).maxTrainers(5).active(true).build();
        activeSubscription = GymSubscription.builder()
                .id(50L).gymTenant(tenant).plan(saasPlan).status(GymSubscriptionStatus.ACTIVE).build();

        centralUsersById.put(2001L, UserAccount.builder()
                .id(2001L).email("alex@member.com").firstName("Alex").lastName("Rivera").phoneNumber("555-1234").build());

        joinRequestsById.put(1L, JoinRequest.builder()
                .id(1L).centralUserId(2001L).email("alex@member.com")
                .firstName("Alex").lastName("Rivera").phoneNumber("555-1234")
                .status(JoinRequestStatus.PENDING).requestNotes("Morning lifter").createdAt(Instant.now()).build());

        PlanTier eliteTier = PlanTier.builder().id(1L).name("ELITE").themeKey(PlanTheme.GOLDEN).defaultTier(true).build();
        plansById.put(1L, MembershipPlan.builder()
                .id(1L).name("Elite Monthly").planTier(eliteTier)
                .basePrice(new BigDecimal("100.00")).finalPrice(new BigDecimal("100.00"))
                .durationDays(30).trial(false).active(true).build());
        plansById.put(2L, MembershipPlan.builder()
                .id(2L).name("Free 7-Day Trial").planTier(eliteTier)
                .basePrice(BigDecimal.ZERO).finalPrice(BigDecimal.ZERO)
                .durationDays(7).trial(true).active(true).build());

        JoinRequestRepository joinRequestRepo = (JoinRequestRepository) Proxy.newProxyInstance(
                JoinRequestRepository.class.getClassLoader(),
                new Class<?>[]{JoinRequestRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "findById" -> Optional.ofNullable(joinRequestsById.get((Long) args[0]));
                    case "findAll" -> {
                        if (args != null && args.length == 2 && args[0] instanceof Specification<?> && args[1] instanceof Pageable pageable) {
                            List<JoinRequest> all = new ArrayList<>(joinRequestsById.values());
                            yield new PageImpl<>(all, pageable, all.size());
                        }
                        yield new ArrayList<>(joinRequestsById.values());
                    }
                    case "save" -> {
                        JoinRequest jr = (JoinRequest) args[0];
                        joinRequestsById.put(jr.getId(), jr);
                        yield jr;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "findById" -> Optional.ofNullable(plansById.get((Long) args[0]));
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (p, m, args) -> null
        );

        TenantUserRepository tenantUserRepo = (TenantUserRepository) Proxy.newProxyInstance(
                TenantUserRepository.class.getClassLoader(),
                new Class<?>[]{TenantUserRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "findById" -> Optional.ofNullable(usersById.get((Long) args[0]));
                    case "findByCentralUserId" -> usersById.values().stream()
                            .filter(u -> u.getCentralUserId().equals(args[0])).findFirst();
                    case "existsByCentralUserId" -> usersById.values().stream()
                            .anyMatch(u -> u.getCentralUserId().equals(args[0]));
                    case "existsByEmail" -> usersById.values().stream()
                            .anyMatch(u -> u.getEmail().equalsIgnoreCase((String) args[0]));
                    case "save" -> {
                        TenantUser u = (TenantUser) args[0];
                        if (u.getId() == null) u.setId(userSeq.incrementAndGet());
                        usersById.put(u.getId(), u);
                        yield u;
                    }
                    case "deleteById" -> {
                        usersById.remove((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MemberRepository memberRepo = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class<?>[]{MemberRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "count" -> (long) membersById.size();
                    case "existsByGymId" -> membersById.values().stream()
                            .anyMatch(mb -> ((String) args[0]).equals(mb.getGymId()));
                    case "save" -> {
                        Member mb = (Member) args[0];
                        if (mb.getId() == null) mb.setId(memberSeq.incrementAndGet());
                        membersById.put(mb.getId(), mb);
                        yield mb;
                    }
                    case "deleteById" -> {
                        membersById.remove((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MemberMembershipRepository membershipRepo = (MemberMembershipRepository) Proxy.newProxyInstance(
                MemberMembershipRepository.class.getClassLoader(),
                new Class<?>[]{MemberMembershipRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "save" -> {
                        MemberMembership mm = (MemberMembership) args[0];
                        if (mm.getId() == null) mm.setId(membershipSeq.incrementAndGet());
                        membershipsById.put(mm.getId(), mm);
                        yield mm;
                    }
                    case "deleteById" -> {
                        membershipsById.remove((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        RevenueRepository revenueRepo = (RevenueRepository) Proxy.newProxyInstance(
                RevenueRepository.class.getClassLoader(),
                new Class<?>[]{RevenueRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "save" -> {
                        Revenue rev = (Revenue) args[0];
                        if (rev.getId() == null) rev.setId(revenueSeq.incrementAndGet());
                        revenuesById.put(rev.getId(), rev);
                        yield rev;
                    }
                    case "deleteById" -> {
                        revenuesById.remove((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        GymTenantRepository gymTenantRepo = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (p, m, args) -> "findById".equals(m.getName())
                        ? Optional.ofNullable(centralTenantsById.get((Long) args[0]))
                        : null
        );

        GymSubscriptionRepository subRepo = (GymSubscriptionRepository) Proxy.newProxyInstance(
                GymSubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{GymSubscriptionRepository.class},
                (p, m, args) -> "findFirstByGymTenantIdOrderBySubscribedAtDesc".equals(m.getName())
                        ? Optional.ofNullable(activeSubscription)
                        : null
        );

        UserAccountRepository centralUserRepo = (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (p, m, args) -> "findById".equals(m.getName())
                        ? Optional.ofNullable(centralUsersById.get((Long) args[0]))
                        : null
        );

        UserTenantRoleRepository roleRepo = (UserTenantRoleRepository) Proxy.newProxyInstance(
                UserTenantRoleRepository.class.getClassLoader(),
                new Class<?>[]{UserTenantRoleRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "findAllByUserAccountIdAndGymTenantId" -> centralRoles.stream()
                            .filter(r -> r.getUserAccount().getId().equals(args[0]) && r.getGymTenant().getId().equals(args[1]))
                            .toList();
                    case "findByUserAccountIdAndGymTenantIdAndRole" -> centralRoles.stream()
                            .filter(r -> r.getUserAccount().getId().equals(args[0])
                                    && r.getGymTenant().getId().equals(args[1])
                                    && r.getRole() == args[2])
                            .findFirst();
                    case "save" -> {
                        if (failCentralRoleSave.get()) {
                            throw new RuntimeException("Simulated central DB failure");
                        }
                        UserTenantRole r = (UserTenantRole) args[0];
                        if (r.getId() == null) r.setId(roleSeq.incrementAndGet());
                        centralRoles.add(r);
                        yield r;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        PlatformTransactionManager noOpTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L);
        jwtUtil.init();

        String token = jwtUtil.generateTenantAccessToken(TenantTokenClaims.builder()
                .userId(9001L).email("admin@gym.com").tenantId(10L).gymCode("IRON-01")
                .tenantDbName("gym_db_10").role(TenantRole.TENANT_ADMIN).adminAuthority(AdminAuthority.PRIMARY)
                .build());

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.addHeader("Authorization", "Bearer " + token);

        TenantAdminContextHelper contextHelper = new TenantAdminContextHelper(jwtUtil, httpReq, tenantUserRepo);
        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();
        JoinRequestHelper joinRequestHelper = new JoinRequestHelper(
                gymTenantRepo, subRepo, centralUserRepo, roleRepo, memberRepo, noOpTxManager
        );

        fetchJoinRequestsService = new FetchJoinRequestsService(joinRequestRepo, validations, mapper);
        approveJoinRequestService = new ApproveJoinRequestService(
                joinRequestRepo, planRepo, tenantUserRepo, memberRepo, membershipRepo, revenueRepo,
                validations, contextHelper, joinRequestHelper, mapper, noOpTxManager
        );
        rejectJoinRequestService = new RejectJoinRequestService(joinRequestRepo, validations, contextHelper, mapper);

        TenantContext.setCurrentTenant("gym_db_10");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FETCH_JOIN_REQUESTS returns paged results and rejects invalid status filters")
    void fetchJoinRequests_PagedAndValidation() {
        TenantAdminResponse res = fetchJoinRequestsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_JOIN_REQUESTS)
                .fetchJoinRequestsRequest(FetchJoinRequestsRequest.builder().status("PENDING").page(0).size(10).build())
                .build());

        assertThat(res.getJoinRequestsPageResponse().totalElements()).isEqualTo(1L);
        assertThat(res.getJoinRequestsPageResponse().content().get(0).getStatus()).isEqualTo(JoinRequestStatus.PENDING);

        assertThatThrownBy(() -> fetchJoinRequestsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_JOIN_REQUESTS)
                .fetchJoinRequestsRequest(FetchJoinRequestsRequest.builder().status("BOGUS").build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("APPROVE_JOIN_REQUEST handles PAID flow creating TenantUser, Member, Membership, Revenue, and Central Role")
    void approveJoinRequest_PaidSuccess() {
        TenantAdminResponse res = approveJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.APPROVE_JOIN_REQUEST)
                .approveJoinRequest(ApproveJoinRequest.builder()
                        .joinRequestId(1L)
                        .planId(1L)
                        .paymentStatus(PaymentStatus.PAID)
                        .paidAmount(new BigDecimal("100.00"))
                        .paymentMode(PaymentMode.ONLINE)
                        .transactionReference("TXN-999")
                        .adminNotes("Welcome!")
                        .build())
                .build());

        ApproveJoinRequestResponse payload = res.getApproveJoinRequestResponse();
        assertThat(payload.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
        assertThat(payload.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payload.getRemainingDue()).isEqualByComparingTo("0.00");
        assertThat(payload.getGymId()).startsWith("MEM-");
        assertThat(membersById).hasSize(1);
        assertThat(membersById.values().iterator().next().getType()).isEqualTo(MemberType.REGULAR);
        assertThat(revenuesById).hasSize(1);
        assertThat(revenuesById.values().iterator().next().getRevenueType()).isEqualTo(RevenueType.NEW_MEMBERSHIP);
        assertThat(centralRoles).hasSize(1);
        assertThat(centralRoles.get(0).getRole()).isEqualTo(TenantRole.MEMBER);
        assertThat(centralRoles.get(0).getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("APPROVE_JOIN_REQUEST handles PARTIALLY_PAID flow and tracks remaining due accurately")
    void approveJoinRequest_PartiallyPaidSuccess() {
        TenantAdminResponse res = approveJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.APPROVE_JOIN_REQUEST)
                .approveJoinRequest(ApproveJoinRequest.builder()
                        .joinRequestId(1L)
                        .planId(1L)
                        .paymentStatus(PaymentStatus.PARTIALLY_PAID)
                        .paidAmount(new BigDecimal("40.00"))
                        .paymentMode(PaymentMode.CASH)
                        .build())
                .build());

        ApproveJoinRequestResponse payload = res.getApproveJoinRequestResponse();
        assertThat(payload.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(payload.getPaidAmount()).isEqualByComparingTo("40.00");
        assertThat(payload.getRemainingDue()).isEqualByComparingTo("60.00");
        assertThat(revenuesById.values().iterator().next().getAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("APPROVE_JOIN_REQUEST enforces payment invariants, max_members quota, and central rollback compensation")
    void approveJoinRequest_GuardsAndCompensation() {
        // 1. Invalid PAID amount
        assertThatThrownBy(() -> approveJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.APPROVE_JOIN_REQUEST)
                .approveJoinRequest(ApproveJoinRequest.builder()
                        .joinRequestId(1L).planId(1L)
                        .paymentStatus(PaymentStatus.PAID).paidAmount(new BigDecimal("50.00"))
                        .paymentMode(PaymentMode.CASH).build())
                .build()))
                .isInstanceOf(BadRequestException.class);

        // 2. SaaS max_members quota exceeded
        activeSubscription.getPlan().setMaxMembers(0);
        assertThatThrownBy(() -> approveJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.APPROVE_JOIN_REQUEST)
                .approveJoinRequest(ApproveJoinRequest.builder()
                        .joinRequestId(1L).planId(1L)
                        .paymentStatus(PaymentStatus.PAID).paidAmount(new BigDecimal("100.00"))
                        .paymentMode(PaymentMode.CASH).build())
                .build()))
                .isInstanceOf(ForbiddenException.class);
        activeSubscription.getPlan().setMaxMembers(5);

        // 3. Central DB write failure triggers clean tenant compensation rollback
        failCentralRoleSave.set(true);
        assertThatThrownBy(() -> approveJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.APPROVE_JOIN_REQUEST)
                .approveJoinRequest(ApproveJoinRequest.builder()
                        .joinRequestId(1L).planId(1L)
                        .paymentStatus(PaymentStatus.PAID).paidAmount(new BigDecimal("100.00"))
                        .paymentMode(PaymentMode.CASH).build())
                .build()))
                .isInstanceOf(RuntimeException.class);

        assertThat(joinRequestsById.get(1L).getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(membersById).isEmpty();
        assertThat(membershipsById).isEmpty();
        assertThat(revenuesById).isEmpty();
    }

    @Test
    @DisplayName("REJECT_JOIN_REQUEST transitions PENDING to REJECTED and prevents re-review")
    void rejectJoinRequest_SuccessAndGuard() {
        TenantAdminResponse res = rejectJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.REJECT_JOIN_REQUEST)
                .rejectJoinRequest(RejectJoinRequest.builder()
                        .joinRequestId(1L)
                        .adminNotes("Incomplete documents")
                        .build())
                .build());

        assertThat(res.getJoinRequestItemResponse().getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
        assertThat(res.getJoinRequestItemResponse().getAdminNotes()).isEqualTo("Incomplete documents");

        assertThatThrownBy(() -> rejectJoinRequestService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.REJECT_JOIN_REQUEST)
                .rejectJoinRequest(RejectJoinRequest.builder().joinRequestId(1L).build())
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PENDING join requests");
    }
}
