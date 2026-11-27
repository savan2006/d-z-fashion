package com.dzfashion.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerifyOtpResponse(
        String message,
        String email,
        String verificationTicket,
        int expiresInSeconds
) {
    @JsonProperty("otpToken")
    public String otpToken() {
        return verificationTicket;
    }
}
