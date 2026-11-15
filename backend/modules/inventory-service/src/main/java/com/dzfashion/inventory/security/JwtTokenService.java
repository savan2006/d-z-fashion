package com.dzfashion.inventory.security;

import com.dzfashion.inventory.config.InventoryProperties;
import com.dzfashion.inventory.enums.Role;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final InventoryProperties inventoryProperties;

    public JwtTokenService(InventoryProperties inventoryProperties) {
        this.inventoryProperties = inventoryProperties;
    }

    public SignedJWT validateAndParseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            byte[] secretBytes = inventoryProperties.getSecret().getBytes(StandardCharsets.UTF_8);
            JWSVerifier verifier = new MACVerifier(secretBytes);

            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("JWT signature validation failed");
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new IllegalArgumentException("JWT token is expired");
            }

            String issuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (!inventoryProperties.getIssuer().equals(issuer)) {
                throw new IllegalArgumentException("Invalid JWT issuer");
            }

            return signedJWT;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate JWT token: " + e.getMessage(), e);
        }
    }

    public UUID getUserId(SignedJWT jwt) {
        try {
            return UUID.fromString(jwt.getJWTClaimsSet().getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to extract user ID from JWT", e);
        }
    }

    public String getUsername(SignedJWT jwt) {
        try {
            return (String) jwt.getJWTClaimsSet().getClaim("username");
        } catch (Exception e) {
            return "user";
        }
    }

    public String getEmail(SignedJWT jwt) {
        try {
            return (String) jwt.getJWTClaimsSet().getClaim("email");
        } catch (Exception e) {
            return "";
        }
    }

    public Role getRole(SignedJWT jwt) {
        try {
            String roleName = (String) jwt.getJWTClaimsSet().getClaim("role");
            return Role.valueOf(roleName);
        } catch (Exception e) {
            return Role.CUSTOMER;
        }
    }

    public String getTokenType(SignedJWT jwt) {
        try {
            return (String) jwt.getJWTClaimsSet().getClaim("token_type");
        } catch (Exception e) {
            return "";
        }
    }
}
