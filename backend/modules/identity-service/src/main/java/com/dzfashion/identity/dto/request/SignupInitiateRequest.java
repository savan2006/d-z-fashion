package com.dzfashion.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupInitiateRequest(
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @JsonAlias({"fullName", "name"})
        String fullName
) {
    public SignupInitiateRequest(String username, String email) {
        this(username, email, null);
    }
}
