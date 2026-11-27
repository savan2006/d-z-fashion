package com.dzfashion.identity.service;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.dto.request.ForgotPasswordInitiateRequest;
import com.dzfashion.identity.dto.request.LoginRequest;
import com.dzfashion.identity.dto.request.RefreshTokenRequest;
import com.dzfashion.identity.dto.request.ResetPasswordRequest;
import com.dzfashion.identity.dto.request.SignupCompleteRequest;
import com.dzfashion.identity.dto.request.SignupInitiateRequest;
import com.dzfashion.identity.dto.request.VerifyOtpRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.AuthResponse;
import com.dzfashion.identity.dto.response.OtpInitiateResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.dto.response.VerifyOtpResponse;
import com.dzfashion.identity.entity.CustomerProfileEntity;
import com.dzfashion.identity.entity.UserEntity;
import com.dzfashion.identity.enums.OtpPurpose;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.exception.AuthenticationFailedException;
import com.dzfashion.identity.exception.ResourceConflictException;
import com.dzfashion.identity.exception.ResourceNotFoundException;
import com.dzfashion.identity.repository.UserRepository;
import com.dzfashion.identity.security.JwtTokenService;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmailOtpService emailOtpService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;

    public AuthService(UserRepository userRepository,
                       EmailOtpService emailOtpService,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder,
                       IdentityProperties properties) {
        this.userRepository = userRepository;
        this.emailOtpService = emailOtpService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public OtpInitiateResponse initiateSignup(SignupInitiateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = (request.username() != null && !request.username().isBlank())
                ? request.username().trim()
                : (request.fullName() != null && !request.fullName().isBlank())
                    ? request.fullName().trim().toLowerCase().replaceAll("[^a-zA-Z0-9_.]", "")
                    : normalizedEmail.split("@")[0];

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResourceConflictException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("An account with this email already exists");
        }

        int cooldownSeconds = emailOtpService.generateAndSendOtp(normalizedEmail, OtpPurpose.REGISTRATION);

        return new OtpInitiateResponse(
                "A 4-digit verification code has been sent to your email",
                normalizedEmail,
                cooldownSeconds
        );
    }

    @Transactional
    public VerifyOtpResponse verifySignupOtp(VerifyOtpRequest request) {
        String ticket = emailOtpService.verifyOtp(request.email(), request.otp(), OtpPurpose.REGISTRATION);
        return new VerifyOtpResponse(
                "OTP verified successfully",
                request.email().trim().toLowerCase(),
                ticket,
                900
        );
    }

    @Transactional
    public AuthResponse completeSignup(SignupCompleteRequest request, String clientProvidedUsername) {
        String normalizedEmail = (request.email() != null && !request.email().isBlank())
                ? request.email().trim().toLowerCase()
                : emailOtpService.getEmailFromTicket(request.verificationTicket());

        emailOtpService.validateVerificationTicket(normalizedEmail, request.verificationTicket(), OtpPurpose.REGISTRATION);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("An account with this email already exists");
        }

        String username = (clientProvidedUsername != null && !clientProvidedUsername.isBlank())
                ? clientProvidedUsername.trim()
                : (request.fullName() != null && !request.fullName().isBlank())
                    ? request.fullName().trim().toLowerCase().replaceAll("[^a-zA-Z0-9_.]", "")
                    : normalizedEmail.split("@")[0];

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            username = username + "_" + System.currentTimeMillis() % 10000;
        }

        String passwordHash = passwordEncoder.encode(request.password());

        // Public signup can NEVER grant privileged roles; always hardcoded to CUSTOMER
        UserEntity user = new UserEntity(username, normalizedEmail, passwordHash, Role.CUSTOMER);
        String fullName = (request.fullName() != null && !request.fullName().isBlank())
                ? request.fullName().trim()
                : username;
        CustomerProfileEntity profile = new CustomerProfileEntity(user, fullName);
        user.setProfile(profile);

        UserEntity savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();

        UserEntity user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier.toLowerCase()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username/email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid username/email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            SignedJWT jwt = jwtTokenService.validateAndParseToken(request.refreshToken());
            String tokenType = jwtTokenService.getTokenType(jwt);

            if (!"REFRESH".equals(tokenType)) {
                throw new AuthenticationFailedException("Invalid token type for refresh operation");
            }

            UUID userId = jwtTokenService.getUserId(jwt);
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationFailedException("User not found"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AuthenticationFailedException("Account is not active");
            }

            return buildAuthResponse(user);
        } catch (AuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationFailedException("Invalid or expired refresh token");
        }
    }

    @Transactional
    public ActionResponse initiateForgotPassword(ForgotPasswordInitiateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            emailOtpService.generateAndSendOtp(normalizedEmail, OtpPurpose.PASSWORD_RESET);
        }

        // Return a generic success message to prevent account enumeration attacks
        return new ActionResponse("If an account exists for " + normalizedEmail + ", a password reset code has been sent.");
    }

    @Transactional
    public VerifyOtpResponse verifyForgotPasswordOtp(VerifyOtpRequest request) {
        String ticket = emailOtpService.verifyOtp(request.email(), request.otp(), OtpPurpose.PASSWORD_RESET);
        return new VerifyOtpResponse(
                "Password reset OTP verified successfully",
                request.email().trim().toLowerCase(),
                ticket,
                900
        );
    }

    @Transactional
    public ActionResponse completeForgotPassword(ResetPasswordRequest request) {
        String normalizedEmail = (request.email() != null && !request.email().isBlank())
                ? request.email().trim().toLowerCase()
                : emailOtpService.getEmailFromTicket(request.resetTicket());

        emailOtpService.validateVerificationTicket(normalizedEmail, request.resetTicket(), OtpPurpose.PASSWORD_RESET);

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new ActionResponse("Password has been reset successfully. Please log in with your new password.");
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        long expiresInSeconds = (long) properties.getJwt().getAccessTokenMinutes() * 60;

        UserSummaryResponse summary = new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );

        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, summary);
    }
}
