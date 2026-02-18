package com.optimaxx.management.interfaces.rest.dto;

import java.time.Instant;

public record DeviceSessionResponse(
        String deviceId,
        String userAgent,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt,
        boolean current
) {
}
