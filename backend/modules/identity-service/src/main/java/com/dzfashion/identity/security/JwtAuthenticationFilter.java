package com.dzfashion.identity.security;

import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                SignedJWT jwt = jwtTokenService.validateAndParseToken(token);
                String tokenType = jwtTokenService.getTokenType(jwt);

                if ("ACCESS".equals(tokenType)) {
                    UUID userId = jwtTokenService.getUserId(jwt);
                    String username = jwtTokenService.getUsername(jwt);
                    String email = jwtTokenService.getEmail(jwt);
                    Role role = jwtTokenService.getRole(jwt);

                    UserPrincipal principal = new UserPrincipal(userId, username, email, "", role, UserStatus.ACTIVE);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // If token is invalid or expired, clear context so unauthenticated flow or entry point catches it
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
