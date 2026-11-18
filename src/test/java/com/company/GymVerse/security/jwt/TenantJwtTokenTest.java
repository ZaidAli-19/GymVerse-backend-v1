package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.multitanancy.TenantContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Story 4.2: Tenant-Scoped JWT Engine & Security Context Resolution Tests")
class TenantJwtTokenTest {

    private JwtUtil jwtUtil;
    private JwtFilter jwtFilter;

    private static final String TEST_SECRET = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L);
        jwtUtil.init();

        jwtFilter = new JwtFilter(jwtUtil);
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should generate valid TENANT_ACCOUNT access token with all tenant-scoped claims")
    void generateTenantAccessToken_ContainsAllRequiredClaims() {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(501L)
                .email("admin@ironfit.com")
                .tenantId(15L)
                .gymCode("GYM-IRON123")
                .tenantDbName("gymverse_tenant_iron_fit_123")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();

        String token = jwtUtil.generateTenantAccessToken(claims);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractSubject(token)).isEqualTo("501");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("admin@ironfit.com");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.TOKEN_TYPE_TENANT_ACCOUNT);
        assertThat(jwtUtil.extractType(token)).isEqualTo(JwtUtil.TYPE_ACCESS);
        assertThat(jwtUtil.extractTenantId(token)).isEqualTo(15L);
        assertThat(jwtUtil.extractGymCode(token)).isEqualTo("GYM-IRON123");
        assertThat(jwtUtil.extractTenantDbName(token)).isEqualTo("gymverse_tenant_iron_fit_123");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("TENANT_ADMIN");
        assertThat(jwtUtil.extractAdminAuthority(token)).isEqualTo("PRIMARY");
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.isTokenValid(token, "501")).isTrue();

        TenantTokenClaims extracted = jwtUtil.extractTenantClaims(token);
        assertThat(extracted.getUserId()).isEqualTo(501L);
        assertThat(extracted.getEmail()).isEqualTo("admin@ironfit.com");
        assertThat(extracted.getTenantId()).isEqualTo(15L);
        assertThat(extracted.getGymCode()).isEqualTo("GYM-IRON123");
        assertThat(extracted.getTenantDbName()).isEqualTo("gymverse_tenant_iron_fit_123");
        assertThat(extracted.getRole()).isEqualTo(TenantRole.TENANT_ADMIN);
        assertThat(extracted.getAdminAuthority()).isEqualTo(AdminAuthority.PRIMARY);
    }

    @Test
    @DisplayName("Should generate valid TENANT_ACCOUNT refresh token with REFRESH use type")
    void generateTenantRefreshToken_ContainsRefreshType() {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(502L)
                .email("member@ironfit.com")
                .tenantId(16L)
                .gymCode("GYM-MEM456")
                .tenantDbName("gymverse_tenant_iron_fit_456")
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .build();

        String refreshToken = jwtUtil.generateTenantRefreshToken(claims);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtil.extractSubject(refreshToken)).isEqualTo("502");
        assertThat(jwtUtil.extractTokenType(refreshToken)).isEqualTo(JwtUtil.TOKEN_TYPE_TENANT_ACCOUNT);
        assertThat(jwtUtil.extractType(refreshToken)).isEqualTo(JwtUtil.TYPE_REFRESH);
        assertThat(jwtUtil.extractRole(refreshToken)).isEqualTo("MEMBER");
        assertThat(jwtUtil.extractAdminAuthority(refreshToken)).isEqualTo("NONE");
        assertThat(jwtUtil.isTokenExpired(refreshToken)).isFalse();
        assertThat(jwtUtil.isTokenValid(refreshToken, "502")).isTrue();
    }

    @Test
    @DisplayName("JwtFilter authenticates TENANT_ACCOUNT access token, sets authorities, sets TenantContext during chain, and cleans up after")
    void jwtFilter_ValidTenantToken_SetsAuthoritiesAndCleansTenantContext() throws ServletException, IOException {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(503L)
                .email("staff@ironfit.com")
                .tenantId(17L)
                .gymCode("GYM-STAFF789")
                .tenantDbName("gymverse_tenant_iron_fit_789")
                .role(TenantRole.STAFF)
                .adminAuthority(AdminAuthority.NONE)
                .build();

        String token = jwtUtil.generateTenantAccessToken(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenant-operations");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] tenantDuringExecution = new String[1];
        final Authentication[] authDuringExecution = new Authentication[1];

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                tenantDuringExecution[0] = TenantContext.getCurrentTenant();
                authDuringExecution[0] = SecurityContextHolder.getContext().getAuthentication();
            }
        };

        jwtFilter.doFilter(request, response, filterChain);

        // Verification during execution
        assertThat(tenantDuringExecution[0]).isEqualTo("gymverse_tenant_iron_fit_789");
        assertThat(authDuringExecution[0]).isNotNull();
        assertThat(authDuringExecution[0].getName()).isEqualTo("503");

        List<String> authorityStrings = authDuringExecution[0].getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorityStrings).containsExactly("ROLE_STAFF");

        // Verification after execution: TenantContext must be cleared to prevent thread-local leakage
        assertThat(TenantContext.getCurrentTenant()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("JwtFilter sets both ROLE_TENANT_ADMIN and AUTHORITY_PRIMARY for primary tenant admin")
    void jwtFilter_TenantAdminPrimary_SetsBothAuthorities() throws ServletException, IOException {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(505L)
                .email("owner@ironfit.com")
                .tenantId(20L)
                .gymCode("GYM-OWNER01")
                .tenantDbName("gymverse_tenant_iron_fit_01")
                .role(TenantRole.TENANT_ADMIN)
                .adminAuthority(AdminAuthority.PRIMARY)
                .build();

        String token = jwtUtil.generateTenantAccessToken(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenant-admin");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Authentication[] authDuringExecution = new Authentication[1];
        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                authDuringExecution[0] = SecurityContextHolder.getContext().getAuthentication();
            }
        };

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(authDuringExecution[0]).isNotNull();
        List<String> authorities = authDuringExecution[0].getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_TENANT_ADMIN", "AUTHORITY_PRIMARY");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("JwtFilter rejects tenant refresh token with HTTP 401 and does not execute chain")
    void jwtFilter_TenantRefreshToken_RejectedWith401() throws ServletException, IOException {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(504L)
                .email("trainer@ironfit.com")
                .tenantId(18L)
                .gymCode("GYM-TRAIN001")
                .tenantDbName("gymverse_tenant_iron_fit_001")
                .role(TenantRole.TRAINER)
                .adminAuthority(AdminAuthority.NONE)
                .build();

        String refreshToken = jwtUtil.generateTenantRefreshToken(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenant-operations");
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean[] chainExecuted = new boolean[1];
        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                chainExecuted[0] = true;
            }
        };

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(chainExecuted[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Refresh token cannot be used to access APIs");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("JwtFilter rejects tenant token without tenantDbName claim")
    void jwtFilter_TenantTokenWithoutDbName_RejectedWith401() throws ServletException, IOException {
        TenantTokenClaims claims = TenantTokenClaims.builder()
                .userId(506L)
                .email("broken@ironfit.com")
                .tenantId(19L)
                .gymCode("GYM-BROKEN")
                .tenantDbName(null) // Missing DB name
                .role(TenantRole.MEMBER)
                .adminAuthority(AdminAuthority.NONE)
                .build();

        String tokenWithoutDbName = jwtUtil.generateTenantAccessToken(claims);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenant-operations");
        request.addHeader("Authorization", "Bearer " + tokenWithoutDbName);
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or malformed JWT token");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("JwtFilter authenticates MASTER_ACCOUNT token with empty authorities and zero TenantContext")
    void jwtFilter_MasterAccountToken_AuthenticatesWithEmptyAuthorities() throws ServletException, IOException {
        com.company.GymVerse.models.central.UserAccount account = com.company.GymVerse.models.central.UserAccount.builder()
                .id(101L)
                .email("master@gymverse.com")
                .build();

        String token = jwtUtil.generateMasterAccessToken(account);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenant-admin");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Authentication[] authDuringExecution = new Authentication[1];
        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                authDuringExecution[0] = SecurityContextHolder.getContext().getAuthentication();
            }
        };

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(authDuringExecution[0]).isNotNull();
        assertThat(authDuringExecution[0].getName()).isEqualTo("101");
        assertThat(authDuringExecution[0].getAuthorities()).isEmpty();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
