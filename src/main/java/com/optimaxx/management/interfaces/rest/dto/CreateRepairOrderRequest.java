package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record CreateRepairOrderRequest(UUID customerId,
                                       UUID transactionTypeId,
                                       String title,
                                       String description,
                                       UUID inventoryItemId,
                                       Integer inventoryQuantity) {
}
