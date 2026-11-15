package com.dzfashion.identity.dto.response;

public record OtpInitiateResponse(
        String message,
        String email,
        int resendAvailableInSeconds
) {
}
