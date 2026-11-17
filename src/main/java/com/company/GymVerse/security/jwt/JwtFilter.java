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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter intercepting incoming requests, extracting and validating Bearer tokens,
 * blocking refresh tokens from accessing APIs, and populating the Spring SecurityContext.
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

                final UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                Collections.emptyList()
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
