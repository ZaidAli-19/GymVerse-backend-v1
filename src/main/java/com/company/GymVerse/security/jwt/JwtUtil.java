package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.AdminAuthority;
import com.company.GymVerse.models.central.enums.TenantRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility for generating, parsing, and validating JJWT 0.12.x tokens for both
 * the Master Account Layer (central) and the Tenant Scoped Layer (multi-tenant).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    public static final String TOKEN_TYPE_MASTER_ACCOUNT = "MASTER_ACCOUNT";
    public static final String TOKEN_TYPE_TENANT_ACCOUNT = "TENANT_ACCOUNT";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_USE_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_GYM_CODE = "gymCode";
    public static final String CLAIM_TENANT_DB_NAME = "tenantDbName";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ADMIN_AUTHORITY = "adminAuthority";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64URL.decode(secret);
        } catch (Exception e) {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── 1. MASTER ACCESS TOKEN ───────────────────────────────────────────
    public String generateMasterAccessToken(final UserAccount account) {
        return Jwts.builder()
                .subject(String.valueOf(account.getId()))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_MASTER_ACCOUNT)
                .claim(CLAIM_USE_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, account.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // ─── 2. MASTER REFRESH TOKEN ──────────────────────────────────────────
    public String generateMasterRefreshToken(final UserAccount account) {
        return Jwts.builder()
                .subject(String.valueOf(account.getId()))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_MASTER_ACCOUNT)
                .claim(CLAIM_USE_TYPE, TYPE_REFRESH)
                .claim(CLAIM_EMAIL, account.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // ─── 3. TENANT ACCESS TOKEN ───────────────────────────────────────────
    public String generateTenantAccessToken(final TenantTokenClaims claims) {
        if (claims == null) {
            throw new IllegalArgumentException("TenantTokenClaims must not be null");
        }
        return Jwts.builder()
                .subject(String.valueOf(claims.getUserId()))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_TENANT_ACCOUNT)
                .claim(CLAIM_USE_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, claims.getEmail())
                .claim(CLAIM_TENANT_ID, claims.getTenantId())
                .claim(CLAIM_GYM_CODE, claims.getGymCode())
                .claim(CLAIM_TENANT_DB_NAME, claims.getTenantDbName())
                .claim(CLAIM_ROLE, claims.getRole() != null ? claims.getRole().name() : null)
                .claim(CLAIM_ADMIN_AUTHORITY, claims.getAdminAuthority() != null ? claims.getAdminAuthority().name() : AdminAuthority.NONE.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // ─── 4. TENANT REFRESH TOKEN ──────────────────────────────────────────
    public String generateTenantRefreshToken(final TenantTokenClaims claims) {
        if (claims == null) {
            throw new IllegalArgumentException("TenantTokenClaims must not be null");
        }
        return Jwts.builder()
                .subject(String.valueOf(claims.getUserId()))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_TENANT_ACCOUNT)
                .claim(CLAIM_USE_TYPE, TYPE_REFRESH)
                .claim(CLAIM_EMAIL, claims.getEmail())
                .claim(CLAIM_TENANT_ID, claims.getTenantId())
                .claim(CLAIM_GYM_CODE, claims.getGymCode())
                .claim(CLAIM_TENANT_DB_NAME, claims.getTenantDbName())
                .claim(CLAIM_ROLE, claims.getRole() != null ? claims.getRole().name() : null)
                .claim(CLAIM_ADMIN_AUTHORITY, claims.getAdminAuthority() != null ? claims.getAdminAuthority().name() : AdminAuthority.NONE.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // ─── EXTRACTORS & VALIDATION ──────────────────────────────────────────

    public String extractSubject(final String token) {
        return extractClaims(token).getSubject();
    }

    public String extractEmail(final String token) {
        return extractClaims(token).get(CLAIM_EMAIL, String.class);
    }

    public String extractTokenType(final String token) {
        return extractClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    public String extractType(final String token) {
        return extractClaims(token).get(CLAIM_USE_TYPE, String.class);
    }

    public Long extractTenantId(final String token) {
        Object val = extractClaims(token).get(CLAIM_TENANT_ID);
        if (val instanceof Number num) {
            return num.longValue();
        }
        return val != null ? Long.valueOf(val.toString()) : null;
    }

    public String extractGymCode(final String token) {
        return extractClaims(token).get(CLAIM_GYM_CODE, String.class);
    }

    public String extractTenantDbName(final String token) {
        return extractClaims(token).get(CLAIM_TENANT_DB_NAME, String.class);
    }

    public String extractRole(final String token) {
        return extractClaims(token).get(CLAIM_ROLE, String.class);
    }

    public String extractAdminAuthority(final String token) {
        return extractClaims(token).get(CLAIM_ADMIN_AUTHORITY, String.class);
    }

    public TenantTokenClaims extractTenantClaims(final String token) {
        Claims claims = extractClaims(token);
        String subject = claims.getSubject();
        Long userId = subject != null ? Long.valueOf(subject) : null;
        String email = claims.get(CLAIM_EMAIL, String.class);
        Object tenantIdObj = claims.get(CLAIM_TENANT_ID);
        Long tenantId = tenantIdObj instanceof Number num ? num.longValue() : (tenantIdObj != null ? Long.valueOf(tenantIdObj.toString()) : null);
        String gymCode = claims.get(CLAIM_GYM_CODE, String.class);
        String dbName = claims.get(CLAIM_TENANT_DB_NAME, String.class);
        String roleStr = claims.get(CLAIM_ROLE, String.class);
        String authStr = claims.get(CLAIM_ADMIN_AUTHORITY, String.class);

        TenantRole role = roleStr != null ? TenantRole.valueOf(roleStr) : null;
        AdminAuthority adminAuthority = authStr != null ? AdminAuthority.valueOf(authStr) : null;

        return TenantTokenClaims.builder()
                .userId(userId)
                .email(email)
                .tenantId(tenantId)
                .gymCode(gymCode)
                .tenantDbName(dbName)
                .role(role)
                .adminAuthority(adminAuthority)
                .build();
    }

    public boolean isTokenExpired(final String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public boolean isTokenValid(final String token, final String expectedSubject) {
        try {
            return extractSubject(token).equals(expectedSubject) && !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    public Claims extractClaims(final String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
