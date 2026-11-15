package com.dzfashion.identity.dto.response;

import com.dzfashion.identity.enums.Role;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        Role role,
        String fullName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country
) {
}
