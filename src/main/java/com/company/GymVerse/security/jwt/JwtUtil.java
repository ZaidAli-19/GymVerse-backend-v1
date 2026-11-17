package com.company.GymVerse.security.jwt;

import com.company.GymVerse.models.central.UserAccount;
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
 * Utility for generating, parsing, and validating JJWT 0.12.x tokens for the master account layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    public static final String TOKEN_TYPE_MASTER_ACCOUNT = "MASTER_ACCOUNT";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

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

    // ─── 1. ACCESS TOKEN ──────────────────────────────────────────────────
    public String generateMasterAccessToken(final UserAccount account) {
        return Jwts.builder()
                .subject(String.valueOf(account.getId()))
                .claim("tokenType", TOKEN_TYPE_MASTER_ACCOUNT)
                .claim("type", TYPE_ACCESS)
                .claim("email", account.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // ─── 2. REFRESH TOKEN ─────────────────────────────────────────────────
    public String generateMasterRefreshToken(final UserAccount account) {
        return Jwts.builder()
                .subject(String.valueOf(account.getId()))
                .claim("tokenType", TOKEN_TYPE_MASTER_ACCOUNT)
                .claim("type", TYPE_REFRESH)
                .claim("email", account.getEmail())
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
        return extractClaims(token).get("email", String.class);
    }

    public String extractTokenType(final String token) {
        return extractClaims(token).get("tokenType", String.class);
    }

    public String extractType(final String token) {
        return extractClaims(token).get("type", String.class);
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
