package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record CustomerResponse(UUID id,
                               String firstName,
                               String lastName,
                               String phone,
                               String email,
                               String notes) {
}
