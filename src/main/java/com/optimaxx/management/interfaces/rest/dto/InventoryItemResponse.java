package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record InventoryItemResponse(UUID id,
                                    String sku,
                                    String name,
                                    String category,
                                    int quantity,
                                    int minQuantity) {
}
