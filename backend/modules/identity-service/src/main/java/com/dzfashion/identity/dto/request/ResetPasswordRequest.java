package com.dzfashion.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        String email,

        @NotBlank(message = "Reset ticket is required")
        @JsonAlias({"otpToken", "resetTicket", "verificationTicket", "ticket"})
        String resetTicket,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
        @JsonAlias({"newPassword", "password"})
        String newPassword
) {
}
