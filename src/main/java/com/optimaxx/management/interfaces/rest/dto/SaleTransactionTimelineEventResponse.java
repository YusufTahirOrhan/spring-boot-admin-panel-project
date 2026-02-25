package com.optimaxx.management.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record SaleTransactionTimelineEventResponse(UUID id,
                                                   String eventType,
                                                   String resourceType,
                                                   String resourceId,
                                                   String afterJson,
                                                   Instant occurredAt) {
}
