package com.dzfashion.identity.dto.response;

import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String email,
        Role role,
        UserStatus status
) {
}
