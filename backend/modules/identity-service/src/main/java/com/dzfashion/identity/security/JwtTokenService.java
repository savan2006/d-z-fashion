package com.dzfashion.identity.security;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.enums.Role;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final IdentityProperties properties;

    public JwtTokenService(IdentityProperties properties) {
        this.properties = properties;
    }

    public String generateAccessToken(UUID userId, String username, String email, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getAccessTokenMinutes(), ChronoUnit.MINUTES);

        return createToken(userId, username, email, role, "ACCESS", now, expiry);
    }

    public String generateRefreshToken(UUID userId, String username, String email, Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getRefreshTokenDays(), ChronoUnit.DAYS);

        return createToken(userId, username, email, role, "REFRESH", now, expiry);
    }

    public SignedJWT validateAndParseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be at least 32 bytes long");
            }
            JWSVerifier verifier = new MACVerifier(secretBytes);

            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new IllegalArgumentException("JWT token has expired");
            }

            String issuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (issuer != null && !issuer.equals(properties.getJwt().getIssuer())) {
                throw new IllegalArgumentException("Invalid JWT issuer");
            }

            return signedJWT;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Malformed JWT token", e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("JWT verification failed: " + e.getMessage(), e);
        }
    }

    public UUID getUserId(SignedJWT jwt) throws ParseException {
        return UUID.fromString(jwt.getJWTClaimsSet().getSubject());
    }

    public String getUsername(SignedJWT jwt) throws ParseException {
        return jwt.getJWTClaimsSet().getStringClaim("username");
    }

    public String getEmail(SignedJWT jwt) throws ParseException {
        return jwt.getJWTClaimsSet().getStringClaim("email");
    }

    public Role getRole(SignedJWT jwt) throws ParseException {
        String roleStr = jwt.getJWTClaimsSet().getStringClaim("role");
        return Role.valueOf(roleStr);
    }

    public String getTokenType(SignedJWT jwt) throws ParseException {
        return jwt.getJWTClaimsSet().getStringClaim("type");
    }

    private String createToken(UUID userId, String username, String email, Role role, String type, Instant issuedAt, Instant expiresAt) {
        try {
            byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be at least 32 bytes long");
            }
            JWSSigner signer = new MACSigner(secretBytes);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issuer(properties.getJwt().getIssuer())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("username", username)
                    .claim("email", email)
                    .claim("role", role.name())
                    .claim("type", type)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Error signing JWT token", e);
        }
    }
}
