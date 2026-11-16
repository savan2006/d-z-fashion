package com.dzfashion.commerce.client;

public interface EmailSenderClient {

    record EmailSendResult(boolean success, String messageId, String errorMessage) {
        public static EmailSendResult success(String messageId) {
            return new EmailSendResult(true, messageId, null);
        }

        public static EmailSendResult failure(String errorMessage) {
            return new EmailSendResult(false, null, errorMessage);
        }
    }

    EmailSendResult sendEmail(String recipientEmail, String subject, String bodyText);
}
