package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSaleTransactionRequest(UUID transactionTypeId,
                                           String customerName,
                                           BigDecimal amount,
                                           String notes) {
}
