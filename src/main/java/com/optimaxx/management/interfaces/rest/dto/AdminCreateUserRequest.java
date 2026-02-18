package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.UserRole;

public record AdminCreateUserRequest(
        String username,
        String email,
        String password,
        UserRole role,
        Boolean active
) {
}
