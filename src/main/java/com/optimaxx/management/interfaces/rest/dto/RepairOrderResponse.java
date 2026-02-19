package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.RepairStatus;
import java.time.Instant;
import java.util.UUID;

public record RepairOrderResponse(UUID id,
                                  UUID customerId,
                                  UUID transactionTypeId,
                                  String title,
                                  String description,
                                  RepairStatus status,
                                  Instant receivedAt) {
}
