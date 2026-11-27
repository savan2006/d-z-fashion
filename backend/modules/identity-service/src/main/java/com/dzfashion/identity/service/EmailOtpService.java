package com.dzfashion.identity.service;

import com.dzfashion.identity.config.IdentityProperties;
import com.dzfashion.identity.entity.EmailOtpEntity;
import com.dzfashion.identity.enums.OtpPurpose;
import com.dzfashion.identity.exception.InvalidOtpException;
import com.dzfashion.identity.exception.RateLimitExceededException;
import com.dzfashion.identity.repository.EmailOtpRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class EmailOtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final NotificationEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailOtpService(EmailOtpRepository emailOtpRepository,
                           NotificationEmailSender emailSender,
                           PasswordEncoder passwordEncoder,
                           IdentityProperties properties) {
        this.emailOtpRepository = emailOtpRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public int generateAndSendOtp(String email, OtpPurpose purpose) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<EmailOtpEntity> existingOpt = emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose);
        if (existingOpt.isPresent()) {
            EmailOtpEntity existing = existingOpt.get();
            if (existing.isResendCoolingDown()) {
                long remainingSeconds = ChronoUnit.SECONDS.between(Instant.now(), existing.getResendAvailableAt());
                throw new RateLimitExceededException("Please wait " + Math.max(1, remainingSeconds) + " seconds before requesting a new OTP");
            }
        }

        // Generate 4-digit code (1000 - 9999)
        int code = 1000 + secureRandom.nextInt(9000);
        String rawOtp = String.valueOf(code);
        String hashedOtp = passwordEncoder.encode(rawOtp);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getOtp().getTtlMinutes(), ChronoUnit.MINUTES);
        Instant resendAvailableAt = now.plus(properties.getOtp().getResendCooldownSeconds(), ChronoUnit.SECONDS);

        EmailOtpEntity otpEntity = new EmailOtpEntity(
                normalizedEmail,
                hashedOtp,
                purpose,
                expiresAt,
                resendAvailableAt,
                properties.getOtp().getMaxAttempts()
        );
        emailOtpRepository.save(otpEntity);

        emailSender.sendOtpEmail(normalizedEmail, rawOtp, purpose.name());

        return properties.getOtp().getResendCooldownSeconds();
    }

    @Transactional
    public String verifyOtp(String email, String submittedOtp, OtpPurpose purpose) {
        String normalizedEmail = email.trim().toLowerCase();

        EmailOtpEntity otpEntity = emailOtpRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose)
                .orElseThrow(() -> new InvalidOtpException("No active OTP request found for this email"));

        if (otpEntity.isVerified()) {
            throw new InvalidOtpException("This OTP has already been verified");
        }

        if (otpEntity.isMaxAttemptsExceeded()) {
            throw new RateLimitExceededException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        if (otpEntity.isExpired()) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(submittedOtp, otpEntity.getOtpHash())) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            emailOtpRepository.save(otpEntity);
            int remaining = Math.max(0, otpEntity.getMaxAttempts() - otpEntity.getAttempts());
            throw new InvalidOtpException("Invalid OTP code. " + remaining + " attempts remaining.");
        }

        otpEntity.setVerifiedAt(Instant.now());
        emailOtpRepository.save(otpEntity);

        return createVerificationTicket(normalizedEmail, purpose);
    }

    public String getEmailFromTicket(String ticket) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(ticket);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new InvalidOtpException("Unable to read verification ticket: " + e.getMessage());
        }
    }

    public void validateVerificationTicket(String email, String ticket, OtpPurpose expectedPurpose) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(ticket);
            byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
            JWSVerifier verifier = new MACVerifier(secretBytes);

            if (!signedJWT.verify(verifier)) {
                throw new InvalidOtpException("Invalid verification ticket signature");
            }

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new InvalidOtpException("Verification ticket has expired. Please verify OTP again.");
            }

            String ticketEmail = signedJWT.getJWTClaimsSet().getSubject();
            if (ticketEmail == null || !ticketEmail.equalsIgnoreCase(email.trim())) {
                throw new InvalidOtpException("Verification ticket does not match the provided email");
            }

            String purposeClaim = signedJWT.getJWTClaimsSet().getStringClaim("purpose");
            if (purposeClaim == null || !purposeClaim.equals(expectedPurpose.name())) {
                throw new InvalidOtpException("Verification ticket purpose mismatch");
            }
        } catch (InvalidOtpException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidOtpException("Invalid verification ticket: " + e.getMessage());
        }
    }

    private String createVerificationTicket(String email, OtpPurpose purpose) {
        try {
            byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
            JWSSigner signer = new MACSigner(secretBytes);

            Instant now = Instant.now();
            Instant expiresAt = now.plus(15, ChronoUnit.MINUTES);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(email.toLowerCase())
                    .issuer(properties.getJwt().getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .claim("purpose", purpose.name())
                    .claim("type", "VERIFICATION_TICKET")
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Error issuing verification ticket", e);
        }
    }
}
