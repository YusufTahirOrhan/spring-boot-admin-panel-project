package com.optimaxx.management.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogEventResponse(UUID id,
                                       UUID actorUserId,
                                       String actorRole,
                                       String action,
                                       String resourceType,
                                       String resourceId,
                                       String beforeJson,
                                       String afterJson,
                                       String requestId,
                                       String ipAddress,
                                       String userAgent,
                                       Instant occurredAt) {
}
