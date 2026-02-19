package com.optimaxx.management.interfaces.rest.dto;

public record AdminUpdateInventoryItemRequest(String name,
                                              String category,
                                              Integer minQuantity) {
}
