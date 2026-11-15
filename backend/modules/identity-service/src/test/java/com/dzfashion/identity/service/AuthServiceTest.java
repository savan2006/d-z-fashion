package com.dzfashion.identity.service;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.dto.request.ForgotPasswordInitiateRequest;
import com.dzfashion.identity.dto.request.LoginRequest;
import com.dzfashion.identity.dto.request.ResetPasswordRequest;
import com.dzfashion.identity.dto.request.SignupCompleteRequest;
import com.dzfashion.identity.dto.request.SignupInitiateRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.AuthResponse;
import com.dzfashion.identity.dto.response.OtpInitiateResponse;
import com.dzfashion.identity.entity.UserEntity;
import com.dzfashion.identity.enums.OtpPurpose;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.exception.AuthenticationFailedException;
import com.dzfashion.identity.exception.ResourceConflictException;
import com.dzfashion.identity.repository.UserRepository;
import com.dzfashion.identity.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailOtpService emailOtpService;

    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private IdentityProperties properties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        properties = new IdentityProperties();
        properties.getJwt().setSecret("dz-fashion-secure-jwt-signing-secret-key-minimum-32-bytes!");
        properties.getJwt().setAccessTokenMinutes(15);
        properties.getJwt().setRefreshTokenDays(14);
        jwtTokenService = new JwtTokenService(properties);

        authService = new AuthService(userRepository, emailOtpService, jwtTokenService, passwordEncoder, properties);
    }

    @Test
    @DisplayName("Initiate signup sends OTP and returns resend cooldown")
    void testInitiateSignupSuccess() {
        SignupInitiateRequest request = new SignupInitiateRequest("priya_sharma", "priya@example.com");
        when(userRepository.existsByUsernameIgnoreCase("priya_sharma")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("priya@example.com")).thenReturn(false);
        when(emailOtpService.generateAndSendOtp("priya@example.com", OtpPurpose.REGISTRATION)).thenReturn(60);

        OtpInitiateResponse response = authService.initiateSignup(request);
        assertEquals("priya@example.com", response.email());
        assertEquals(60, response.resendAvailableInSeconds());
    }

    @Test
    @DisplayName("Initiate signup rejects duplicate username and duplicate email")
    void testInitiateSignupDuplicateRejection() {
        SignupInitiateRequest request1 = new SignupInitiateRequest("existing_user", "new@example.com");
        when(userRepository.existsByUsernameIgnoreCase("existing_user")).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> authService.initiateSignup(request1));

        SignupInitiateRequest request2 = new SignupInitiateRequest("new_user", "existing@example.com");
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> authService.initiateSignup(request2));
    }

    @Test
    @DisplayName("Complete signup enforces CUSTOMER role and saves profile")
    void testCompleteSignupEnforcesCustomerRole() {
        SignupCompleteRequest request = new SignupCompleteRequest(
                "anita@example.com",
                "valid-ticket-token",
                "SecureP@ss123",
                "Anita Patel"
        );

        doNothing().when(emailOtpService).validateVerificationTicket("anita@example.com", "valid-ticket-token", OtpPurpose.REGISTRATION);
        when(userRepository.existsByEmailIgnoreCase("anita@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("anita")).thenReturn(false);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse authResponse = authService.completeSignup(request, "anita");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertEquals(Role.CUSTOMER, savedUser.getRole(), "Public signup must ALWAYS assign CUSTOMER role");
        assertEquals("anita", savedUser.getUsername());
        assertEquals("anita@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("SecureP@ss123", savedUser.getPasswordHash()));

        assertNotNull(savedUser.getProfile());
        assertEquals("Anita Patel", savedUser.getProfile().getFullName());

        assertNotNull(authResponse.accessToken());
        assertNotNull(authResponse.refreshToken());
        assertEquals(Role.CUSTOMER, authResponse.user().role());
    }

    @Test
    @DisplayName("Login by username succeeds with valid credentials")
    void testLoginByUsernameSuccess() {
        UserEntity user = new UserEntity("sita_m", "sita@example.com", passwordEncoder.encode("SecretPassword1"), Role.CUSTOMER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByUsernameIgnoreCase("sita_m")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("sita_m", "SecretPassword1");
        AuthResponse response = authService.login(request);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(user.getId(), response.user().id());
        assertEquals(Role.CUSTOMER, response.user().role());
    }

    @Test
    @DisplayName("Login by email succeeds with valid credentials")
    void testLoginByEmailSuccess() {
        UserEntity user = new UserEntity("sita_m", "sita@example.com", passwordEncoder.encode("SecretPassword1"), Role.CUSTOMER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByUsernameIgnoreCase("sita@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("sita@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("sita@example.com", "SecretPassword1");
        AuthResponse response = authService.login(request);

        assertNotNull(response.accessToken());
        assertEquals("sita@example.com", response.user().email());
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void testLoginInvalidPassword() {
        UserEntity user = new UserEntity("sita_m", "sita@example.com", passwordEncoder.encode("CorrectPassword"), Role.CUSTOMER);

        when(userRepository.findByUsernameIgnoreCase("sita_m")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("sita_m", "WrongPassword");
        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login fails for inactive or suspended account")
    void testLoginSuspendedAccount() {
        UserEntity user = new UserEntity("sita_m", "sita@example.com", passwordEncoder.encode("CorrectPassword"), Role.CUSTOMER);
        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByUsernameIgnoreCase("sita_m")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("sita_m", "CorrectPassword");
        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Forgot password complete updates password hash")
    void testForgotPasswordComplete() {
        UserEntity user = new UserEntity("user1", "user1@example.com", passwordEncoder.encode("OldPassword1"), Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("user1@example.com")).thenReturn(Optional.of(user));
        doNothing().when(emailOtpService).validateVerificationTicket("user1@example.com", "reset-ticket", OtpPurpose.PASSWORD_RESET);

        ResetPasswordRequest request = new ResetPasswordRequest("user1@example.com", "reset-ticket", "NewPassword123");
        ActionResponse response = authService.completeForgotPassword(request);

        assertTrue(passwordEncoder.matches("NewPassword123", user.getPasswordHash()));
        verify(userRepository).save(user);
        assertNotNull(response.message());
    }
}
