package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.InventoryMovementType;

public record InventoryStockChangeRequest(InventoryMovementType movementType,
                                          Integer quantity,
                                          String reason) {
}
