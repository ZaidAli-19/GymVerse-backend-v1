package com.company.GymVerse.security.jwt;

import com.company.GymVerse.multitanancy.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter intercepting incoming requests, extracting and validating Bearer tokens,
 * blocking refresh tokens from accessing APIs, establishing multi-tenant context,
 * and populating the Spring SecurityContext with appropriate granted authorities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwtToken = authHeader.substring(7).trim();

            if (jwtToken.isBlank()) {
                throw new BadCredentialsException("Bearer token is empty");
            }

            final String tokenUseType = jwtUtil.extractType(jwtToken); // ACCESS / REFRESH

            if (JwtUtil.TYPE_REFRESH.equalsIgnoreCase(tokenUseType)) {
                throw new InsufficientAuthenticationException(
                        "Refresh token cannot be used to access APIs"
                );
            }

            final String subject = jwtUtil.extractSubject(jwtToken);

            if (subject != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtUtil.isTokenValid(jwtToken, subject)) {

                final String tokenType = jwtUtil.extractTokenType(jwtToken);
                final List<GrantedAuthority> authorities = new ArrayList<>();

                if (JwtUtil.TOKEN_TYPE_MASTER_ACCOUNT.equalsIgnoreCase(tokenType)) {
                    // Master account operates at central platform layer; zero tenant context and empty authorities list
                } else if (JwtUtil.TOKEN_TYPE_TENANT_ACCOUNT.equalsIgnoreCase(tokenType)) {
                    final String dbName = jwtUtil.extractTenantDbName(jwtToken);
                    if (dbName == null || dbName.isBlank()) {
                        throw new BadCredentialsException("Tenant token missing tenantDbName claim");
                    }
                    TenantContext.setCurrentTenant(dbName);

                    final String role = jwtUtil.extractRole(jwtToken);
                    if (role != null && !role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    }

                    final String adminAuthority = jwtUtil.extractAdminAuthority(jwtToken);
                    if (adminAuthority != null && !adminAuthority.isBlank() && !"NONE".equalsIgnoreCase(adminAuthority)) {
                        authorities.add(new SimpleGrantedAuthority("AUTHORITY_" + adminAuthority.toUpperCase()));
                    }
                } else {
                    throw new BadCredentialsException("Unsupported or unknown token type: " + tokenType);
                }

                final UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                authorities
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (InsufficientAuthenticationException e) {
            log.warn("Insufficient authentication on [{}]: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + e.getMessage() + "\",\"path\":\""
                            + request.getRequestURI() + "\"}"
            );
        } catch (JwtException | BadCredentialsException e) {
            log.warn("Invalid JWT token on [{}]: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or malformed JWT token\",\"path\":\""
                            + request.getRequestURI() + "\"}"
            );
        } finally {
            TenantContext.clear();
        }
    }
}
