package com.dzfashion.commerce.client.impl;

import com.dzfashion.commerce.client.EmailSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnMissingBean(name = "customEmailSenderClient")
public class DefaultEmailSenderClient implements EmailSenderClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailSenderClient.class);

    private boolean simulateFailure = false;

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public EmailSendResult sendEmail(String recipientEmail, String subject, String bodyText) {
        if (simulateFailure) {
            log.warn("Simulated email delivery failure to {}", maskEmail(recipientEmail));
            return EmailSendResult.failure("Simulated SMTP server connection timeout");
        }

        String messageId = "MSG-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Simulated email dispatched: id={}, recipient={}, subject='{}'",
                messageId, maskEmail(recipientEmail), subject);
        return EmailSendResult.success(messageId);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String maskedName = name.length() > 2 ? name.substring(0, 1) + "***" + name.substring(name.length() - 1) : "***";
        return maskedName + "@" + parts[1];
    }
}
