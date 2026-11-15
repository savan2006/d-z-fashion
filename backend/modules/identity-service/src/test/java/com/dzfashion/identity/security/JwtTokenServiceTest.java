package com.dzfashion.identity.security;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.enums.Role;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    private JwtTokenService tokenService;
    private IdentityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IdentityProperties();
        properties.getJwt().setSecret("dz-fashion-secure-jwt-signing-secret-key-minimum-32-bytes!");
        properties.getJwt().setIssuer("dz-fashion");
        properties.getJwt().setAccessTokenMinutes(15);
        properties.getJwt().setRefreshTokenDays(14);
        tokenService = new JwtTokenService(properties);
    }

    @Test
    @DisplayName("Generate and validate a valid access token")
    void testGenerateAndValidateAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = tokenService.generateAccessToken(userId, "testuser", "test@dzfashion.com", Role.CUSTOMER);
        assertNotNull(token);

        SignedJWT parsed = tokenService.validateAndParseToken(token);
        assertEquals(userId, tokenService.getUserId(parsed));
        assertEquals("testuser", tokenService.getUsername(parsed));
        assertEquals("test@dzfashion.com", tokenService.getEmail(parsed));
        assertEquals(Role.CUSTOMER, tokenService.getRole(parsed));
        assertEquals("ACCESS", tokenService.getTokenType(parsed));
    }

    @Test
    @DisplayName("Generate and validate a valid refresh token")
    void testGenerateAndValidateRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = tokenService.generateRefreshToken(userId, "manager1", "mgr@dzfashion.com", Role.MANAGER);
        assertNotNull(token);

        SignedJWT parsed = tokenService.validateAndParseToken(token);
        assertEquals(userId, tokenService.getUserId(parsed));
        assertEquals(Role.MANAGER, tokenService.getRole(parsed));
        assertEquals("REFRESH", tokenService.getTokenType(parsed));
    }

    @Test
    @DisplayName("Reject token with tampered signature")
    void testRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenService.generateAccessToken(userId, "user1", "user1@dzfashion.com", Role.CUSTOMER);

        String tampered = token.substring(0, token.length() - 5) + "abcde";
        assertThrows(IllegalArgumentException.class, () -> tokenService.validateAndParseToken(tampered));
    }

    @Test
    @DisplayName("Reject token signed with different secret")
    void testRejectDifferentSecret() {
        IdentityProperties otherProps = new IdentityProperties();
        otherProps.getJwt().setSecret("completely-different-secret-key-at-least-32-bytes-long!");
        JwtTokenService otherService = new JwtTokenService(otherProps);

        UUID userId = UUID.randomUUID();
        String token = otherService.generateAccessToken(userId, "user1", "user1@dzfashion.com", Role.CUSTOMER);

        assertThrows(IllegalArgumentException.class, () -> tokenService.validateAndParseToken(token));
    }
}
