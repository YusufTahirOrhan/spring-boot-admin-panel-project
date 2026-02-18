package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.UserRole;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean active
) {
}
