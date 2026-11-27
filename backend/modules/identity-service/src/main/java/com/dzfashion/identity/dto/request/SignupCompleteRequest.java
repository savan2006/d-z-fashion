package com.dzfashion.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupCompleteRequest(
        String email,

        @NotBlank(message = "Verification ticket is required")
        @JsonAlias({"otpToken", "verificationTicket", "ticket"})
        String verificationTicket,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
        String password,

        String fullName,

        String confirmPassword
) {
    public SignupCompleteRequest(String email, String verificationTicket, String password, String fullName) {
        this(email, verificationTicket, password, fullName, null);
    }
}
