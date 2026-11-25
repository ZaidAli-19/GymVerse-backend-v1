package com.company.GymVerse.tenantAdmin.service.subscription;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.BillingCycle;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FetchTenantSubscriptionServiceTest {

    private Map<Long, GymSubscription> subscriptions;
    private FetchTenantSubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptions = new HashMap<>();

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L);
        jwtUtil.init();

        String token = jwtUtil.generateTenantAccessToken(TenantTokenClaims.builder()
                .userId(2001L)
                .email("admin@gym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gym_db_101")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        TenantContext.setCurrentTenant("gym_db_101");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "2001",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))));

        GymSubscriptionRepository repoProxy = (GymSubscriptionRepository) Proxy.newProxyInstance(
                GymSubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{GymSubscriptionRepository.class},
                (proxy, method, args) -> {
                    if ("findFirstByGymTenantIdOrderBySubscribedAtDesc".equals(method.getName())) {
                        return Optional.ofNullable(subscriptions.get((Long) args[0]));
                    }
                    throw new UnsupportedOperationException("Unimplemented method: " + method.getName());
                }
        );

        TenantAdminContextHelper contextHelper = new TenantAdminContextHelper(jwtUtil, request, null);
        service = new FetchTenantSubscriptionService(
                repoProxy,
                new TenantAdminValidationsImpl(),
                new TenantAdminMapperImpl(),
                contextHelper
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Retrieves active trial subscription details cleanly via subfoldered service")
    void execute_Success() {
        GymTenant tenant = GymTenant.builder().id(101L).gymCode("GOLD-01").gymName("Gold Gym").build();
        SaasSubscriptionPlan plan = SaasSubscriptionPlan.builder()
                .id(1L).name("Growth").price(new BigDecimal("99.99")).billingCycle(BillingCycle.MONTHLY)
                .maxMembers(200).maxTrainers(10).analyticsEnabled(true).multiTrainerEnabled(true).customBrandingEnabled(true)
                .build();
        GymSubscription sub = GymSubscription.builder()
                .id(10L).gymTenant(tenant).plan(plan).pricePaid(BigDecimal.ZERO)
                .billingCycle(BillingCycle.MONTHLY).status(GymSubscriptionStatus.TRIAL)
                .startDate(LocalDate.now().minusDays(2)).endDate(LocalDate.now().plusDays(12))
                .build();
        subscriptions.put(101L, sub);

        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        TenantAdminResponse response = service.execute(request);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getTenantSubscriptionDetailsResponse().getTenantId()).isEqualTo(101L);
        assertThat(response.getTenantSubscriptionDetailsResponse().isTrial()).isTrue();
        assertThat(response.getTenantSubscriptionDetailsResponse().getDaysRemaining()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when tenant has no subscription")
    void execute_NotFound() {
        TenantAdminRequest request = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TENANT_SUBSCRIPTION)
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
