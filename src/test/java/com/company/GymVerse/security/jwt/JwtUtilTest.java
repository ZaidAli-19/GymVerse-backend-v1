package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.UserAccount;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 3.2: JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLWhzMjU2LWFsZ29yaXRobQ";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 2592000000L); // 30 days
        jwtUtil.init();
    }

    @Test
    @DisplayName("Should generate valid MASTER_ACCOUNT access token with correct claims")
    void generateMasterAccessToken_ContainsCorrectClaims() {
        final UserAccount account = UserAccount.builder()
                .id(101L)
                .email("master@gymverse.com")
                .build();

        final String token = jwtUtil.generateMasterAccessToken(account);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractSubject(token)).isEqualTo("101");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("master@gymverse.com");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.TOKEN_TYPE_MASTER_ACCOUNT);
        assertThat(jwtUtil.extractType(token)).isEqualTo(JwtUtil.TYPE_ACCESS);
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.isTokenValid(token, "101")).isTrue();
    }

    @Test
    @DisplayName("Should generate valid MASTER_ACCOUNT refresh token with REFRESH type")
    void generateMasterRefreshToken_ContainsRefreshType() {
        final UserAccount account = UserAccount.builder()
                .id(102L)
                .email("refresh@gymverse.com")
                .build();

        final String refreshToken = jwtUtil.generateMasterRefreshToken(account);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtil.extractSubject(refreshToken)).isEqualTo("102");
        assertThat(jwtUtil.extractTokenType(refreshToken)).isEqualTo(JwtUtil.TOKEN_TYPE_MASTER_ACCOUNT);
        assertThat(jwtUtil.extractType(refreshToken)).isEqualTo(JwtUtil.TYPE_REFRESH);
        assertThat(jwtUtil.isTokenExpired(refreshToken)).isFalse();
        assertThat(jwtUtil.isTokenValid(refreshToken, "102")).isTrue();
    }

    @Test
    @DisplayName("Should flag token as expired if expiration is in the past")
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        final JwtUtil expiredUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(expiredUtil, "accessTokenExpiration", -1000L);
        ReflectionTestUtils.setField(expiredUtil, "refreshTokenExpiration", -1000L);
        expiredUtil.init();

        final UserAccount account = UserAccount.builder().id(99L).email("expired@gymverse.com").build();
        final String expiredToken = expiredUtil.generateMasterAccessToken(account);

        assertThat(expiredUtil.isTokenExpired(expiredToken)).isTrue();
        assertThat(expiredUtil.isTokenValid(expiredToken, "99")).isFalse();
    }

    @Test
    @DisplayName("Should return false when checking validity with mismatched expected subject")
    void isTokenValid_MismatchedSubject_ReturnsFalse() {
        final UserAccount account = UserAccount.builder().id(50L).email("user@gymverse.com").build();
        final String token = jwtUtil.generateMasterAccessToken(account);

        assertThat(jwtUtil.isTokenValid(token, "999")).isFalse();
    }

    @Test
    @DisplayName("Should throw JwtException when attempting to parse tampered token")
    void extractClaims_TamperedToken_ThrowsJwtException() {
        final UserAccount account = UserAccount.builder().id(20L).email("tamper@gymverse.com").build();
        final String validToken = jwtUtil.generateMasterAccessToken(account);

        final String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        assertThatThrownBy(() -> jwtUtil.extractClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }
}
