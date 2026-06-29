package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleTransactionLineItemRequest(UUID inventoryItemId,
                                             String name,
                                             String sku,
                                             Integer quantity,
                                             BigDecimal unitPrice,
                                             BigDecimal lineTotal) {
}
