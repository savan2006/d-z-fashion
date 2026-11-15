package com.dzfashion.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailSender.class);

    private final JavaMailSender mailSender;

    public NotificationEmailSender(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String recipientEmail, String otpCode, String purpose) {
        // Never log the raw OTP in logs
        log.info("Sending OTP email to recipient for purpose={}", purpose);

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject("D & Z Fashion - Your Verification Code");
                message.setText("Your D & Z Fashion verification code is: " + otpCode + "\n\nThis code is valid for 5 minutes. Do not share this code with anyone.");
                mailSender.send(message);
                log.info("Successfully dispatched OTP email to mail server");
                return;
            } catch (Exception e) {
                log.warn("Failed to dispatch email through configured mailSender (falling back to sandbox delivery): {}", e.getMessage());
            }
        }

        // Development sandbox mode: delivery recorded safely without logging sensitive OTP in cleartext
        log.info("Sandbox delivery: verification code generated for {}", recipientEmail.replaceAll("(^[^@]{2})[^@]+", "$1***"));
    }
}
