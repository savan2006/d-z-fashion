package com.dzfashion.identity.service;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.entity.EmailOtpEntity;
import com.dzfashion.identity.enums.OtpPurpose;
import com.dzfashion.identity.exception.InvalidOtpException;
import com.dzfashion.identity.exception.RateLimitExceededException;
import com.dzfashion.identity.repository.EmailOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOtpServiceTest {

    @Mock
    private EmailOtpRepository emailOtpRepository;

    @Mock
    private NotificationEmailSender emailSender;

    private PasswordEncoder passwordEncoder;
    private IdentityProperties properties;
    private EmailOtpService otpService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        properties = new IdentityProperties();
        properties.getJwt().setSecret("dz-fashion-secure-jwt-signing-secret-key-minimum-32-bytes!");
        properties.getOtp().setTtlMinutes(5);
        properties.getOtp().setResendCooldownSeconds(60);
        properties.getOtp().setMaxAttempts(5);

        otpService = new EmailOtpService(emailOtpRepository, emailSender, passwordEncoder, properties);
    }

    @Test
    @DisplayName("Generate 4-digit OTP, store BCrypt hash, and dispatch via email")
    void testGenerateAndSendOtp() {
        String email = "customer@dzfashion.com";
        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        int cooldown = otpService.generateAndSendOtp(email, OtpPurpose.REGISTRATION);
        assertEquals(60, cooldown);

        ArgumentCaptor<EmailOtpEntity> entityCaptor = ArgumentCaptor.forClass(EmailOtpEntity.class);
        verify(emailOtpRepository).save(entityCaptor.capture());
        EmailOtpEntity savedEntity = entityCaptor.getValue();

        assertEquals(email, savedEntity.getEmail());
        assertEquals(OtpPurpose.REGISTRATION, savedEntity.getPurpose());
        assertNotNull(savedEntity.getOtpHash());
        assertEquals(0, savedEntity.getAttempts());
        assertEquals(5, savedEntity.getMaxAttempts());

        ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendOtpEmail(eq(email), rawOtpCaptor.capture(), eq("REGISTRATION"));
        String rawOtp = rawOtpCaptor.getValue();

        assertEquals(4, rawOtp.length());
        assertTrue(rawOtp.matches("^\\d{4}$"));
        assertTrue(passwordEncoder.matches(rawOtp, savedEntity.getOtpHash()));
    }

    @Test
    @DisplayName("Verify valid matching OTP and return signed verification ticket")
    void testVerifyOtpSuccess() {
        String email = "customer@dzfashion.com";
        String rawOtp = "4821";
        String hashedOtp = passwordEncoder.encode(rawOtp);

        EmailOtpEntity entity = new EmailOtpEntity(
                email,
                hashedOtp,
                OtpPurpose.REGISTRATION,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                Instant.now().plus(60, ChronoUnit.SECONDS),
                5
        );

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(entity));

        String ticket = otpService.verifyOtp(email, rawOtp, OtpPurpose.REGISTRATION);
        assertNotNull(ticket);
        assertNotNull(entity.getVerifiedAt());

        assertDoesNotThrow(() -> otpService.validateVerificationTicket(email, ticket, OtpPurpose.REGISTRATION));
    }

    @Test
    @DisplayName("Reject incorrect OTP and increment attempt counter")
    void testVerifyOtpWrongCode() {
        String email = "customer@dzfashion.com";
        String rawOtp = "4821";
        String hashedOtp = passwordEncoder.encode(rawOtp);

        EmailOtpEntity entity = new EmailOtpEntity(
                email,
                hashedOtp,
                OtpPurpose.REGISTRATION,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                Instant.now().plus(60, ChronoUnit.SECONDS),
                5
        );

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(entity));

        InvalidOtpException exception = assertThrows(InvalidOtpException.class,
                () -> otpService.verifyOtp(email, "9999", OtpPurpose.REGISTRATION));

        assertTrue(exception.getMessage().contains("Invalid OTP code"));
        assertEquals(1, entity.getAttempts());
        verify(emailOtpRepository).save(entity);
    }

    @Test
    @DisplayName("Lock OTP verification after maximum 5 failed attempts")
    void testVerifyOtpMaxAttemptsExceeded() {
        String email = "customer@dzfashion.com";
        String hashedOtp = passwordEncoder.encode("4821");

        EmailOtpEntity entity = new EmailOtpEntity(
                email,
                hashedOtp,
                OtpPurpose.REGISTRATION,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                Instant.now().plus(60, ChronoUnit.SECONDS),
                5
        );
        entity.setAttempts(5);

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(entity));

        assertThrows(RateLimitExceededException.class,
                () -> otpService.verifyOtp(email, "4821", OtpPurpose.REGISTRATION));
    }

    @Test
    @DisplayName("Block OTP resend when inside 60-second cooldown period")
    void testResendCooldownEnforced() {
        String email = "customer@dzfashion.com";
        EmailOtpEntity recent = new EmailOtpEntity(
                email,
                "hash",
                OtpPurpose.REGISTRATION,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                Instant.now().plus(45, ChronoUnit.SECONDS),
                5
        );

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(recent));

        assertThrows(RateLimitExceededException.class,
                () -> otpService.generateAndSendOtp(email, OtpPurpose.REGISTRATION));
    }

    @Test
    @DisplayName("Reject expired OTP code")
    void testExpiredOtpRejected() {
        String email = "customer@dzfashion.com";
        String rawOtp = "4821";
        String hashedOtp = passwordEncoder.encode(rawOtp);

        EmailOtpEntity expiredEntity = new EmailOtpEntity(
                email,
                hashedOtp,
                OtpPurpose.REGISTRATION,
                Instant.now().minus(1, ChronoUnit.MINUTES),
                Instant.now().minus(2, ChronoUnit.MINUTES),
                5
        );

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(expiredEntity));

        assertThrows(InvalidOtpException.class,
                () -> otpService.verifyOtp(email, rawOtp, OtpPurpose.REGISTRATION));
    }

    @Test
    @DisplayName("Reject verification ticket when submitted for a different email")
    void testValidateTicketMismatchEmail() {
        String email1 = "user1@dzfashion.com";
        String email2 = "user2@dzfashion.com";
        String rawOtp = "4821";
        String hashedOtp = passwordEncoder.encode(rawOtp);

        EmailOtpEntity entity = new EmailOtpEntity(
                email1,
                hashedOtp,
                OtpPurpose.REGISTRATION,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                Instant.now().plus(60, ChronoUnit.SECONDS),
                5
        );

        when(emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email1, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(entity));

        String ticket = otpService.verifyOtp(email1, rawOtp, OtpPurpose.REGISTRATION);

        assertThrows(InvalidOtpException.class,
                () -> otpService.validateVerificationTicket(email2, ticket, OtpPurpose.REGISTRATION));
    }
}
