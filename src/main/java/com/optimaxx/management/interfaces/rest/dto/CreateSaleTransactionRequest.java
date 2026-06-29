package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSaleTransactionRequest(UUID transactionTypeId,
                                           UUID customerId,
                                           String customerName,
                                           BigDecimal amount,
                                           String notes,
                                           String paymentMethod,
                                           String paymentReference,
                                           UUID inventoryItemId,
                                           Integer inventoryQuantity,
                                           List<SaleTransactionLineItemRequest> items) {

    public CreateSaleTransactionRequest(UUID transactionTypeId,
                                        UUID customerId,
                                        String customerName,
                                        BigDecimal amount,
                                        String notes,
                                        String paymentMethod,
                                        String paymentReference,
                                        UUID inventoryItemId,
                                        Integer inventoryQuantity) {
        this(transactionTypeId, customerId, customerName, amount, notes, paymentMethod, paymentReference, inventoryItemId, inventoryQuantity, null);
    }
}
