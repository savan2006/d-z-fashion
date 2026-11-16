package com.dzfashion.fulfillment.security;

import com.dzfashion.fulfillment.config.JwtProperties;
import com.dzfashion.fulfillment.enums.Role;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public UserPrincipal validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return null;
            }

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("JWT is expired");
                return null;
            }

            String userIdStr = signedJWT.getJWTClaimsSet().getSubject();
            if (userIdStr == null || userIdStr.isBlank()) {
                return null;
            }
            UUID userId = UUID.fromString(userIdStr);

            String email = (String) signedJWT.getJWTClaimsSet().getClaim("email");
            String username = (String) signedJWT.getJWTClaimsSet().getClaim("username");
            if (username == null || username.isBlank()) {
                username = email != null ? email : userIdStr;
            }

            String roleStr = (String) signedJWT.getJWTClaimsSet().getClaim("role");
            Role role = Role.CUSTOMER;
            if (roleStr != null) {
                try {
                    role = Role.valueOf(roleStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Unrecognized role in token: {}", roleStr);
                }
            }

            return new UserPrincipal(userId, username, email, role);
        } catch (Exception e) {
            log.error("Failed to parse/validate JWT: {}", e.getMessage());
            return null;
        }
    }
}
