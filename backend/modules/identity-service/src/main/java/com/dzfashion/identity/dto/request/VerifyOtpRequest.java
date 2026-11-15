package com.dzfashion.identity.dto.request;

import com.dzfashion.identity.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{4}$", message = "OTP must be exactly 4 digits")
        String otp,

        OtpPurpose purpose
) {
    public OtpPurpose getEffectivePurpose() {
        return purpose != null ? purpose : OtpPurpose.REGISTRATION;
    }
}
