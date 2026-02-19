package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record TransactionTypeResponse(UUID id, String code, String name, boolean active, int sortOrder) {
}
