package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.security.jwt.JwtUtil;
import com.company.GymVerse.security.jwt.TenantTokenClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAdminContextHelperTest {

    private JwtUtil jwtUtil;
    private MockHttpServletRequest httpServletRequest;
    private Map<Long, TenantUser> tenantUsers;
    private TenantAdminContextHelper contextHelper;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L);
        jwtUtil.init();

        httpServletRequest = new MockHttpServletRequest();
        tenantUsers = new HashMap<>();

        TenantUserRepository repoProxy = (TenantUserRepository) Proxy.newProxyInstance(
                TenantUserRepository.class.getClassLoader(),
                new Class<?>[]{TenantUserRepository.class},
                (proxy, method, args) -> {
                    if ("findByCentralUserId".equals(method.getName())) {
                        return Optional.ofNullable(tenantUsers.get((Long) args[0]));
                    }
                    throw new UnsupportedOperationException("Unimplemented method: " + method.getName());
                }
        );

        contextHelper = new TenantAdminContextHelper(jwtUtil, httpServletRequest, repoProxy);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Extracts tenantId cleanly from Bearer Authorization header")
    void extractTenantId_Success() {
        String token = jwtUtil.generateTenantAccessToken(TenantTokenClaims.builder()
                .userId(2001L)
                .email("admin@gym.com")
                .tenantId(101L)
                .gymCode("GOLD-01")
                .tenantDbName("gym_db_101")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build());
        httpServletRequest.addHeader("Authorization", "Bearer " + token);

        assertThat(contextHelper.extractTenantId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Throws BadCredentialsException when Authorization header is missing")
    void extractTenantId_MissingHeader() {
        assertThatThrownBy(() -> contextHelper.extractTenantId())
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Resolves authenticated centralUserId and local TenantUser")
    void resolveCurrentAdminUser_Success() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2001", null, Collections.emptyList()));
        TenantUser adminUser = TenantUser.builder()
                .id(11L)
                .centralUserId(2001L)
                .email("admin@gym.com")
                .role(TenantRole.TENANT_ADMIN)
                .build();
        tenantUsers.put(2001L, adminUser);

        assertThat(contextHelper.extractCentralUserId()).isEqualTo(2001L);
        assertThat(contextHelper.resolveCurrentAdminUser()).isEqualTo(adminUser);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when local TenantUser profile is missing")
    void resolveCurrentAdminUser_NotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2001", null, Collections.emptyList()));

        assertThatThrownBy(() -> contextHelper.resolveCurrentAdminUser())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
