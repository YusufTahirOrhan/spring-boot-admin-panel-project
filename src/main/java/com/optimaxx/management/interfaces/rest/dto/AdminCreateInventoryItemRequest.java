package com.optimaxx.management.interfaces.rest.dto;

public record AdminCreateInventoryItemRequest(String sku,
                                              String name,
                                              String category,
                                              Integer quantity,
                                              Integer minQuantity) {
}
