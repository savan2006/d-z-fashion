package com.dzfashion.identity.dto.response;

public record VerifyOtpResponse(
        String message,
        String email,
        String verificationTicket,
        int expiresInSeconds
) {
}
