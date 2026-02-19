package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleTransactionResponse(UUID id,
                                      UUID transactionTypeId,
                                      String transactionTypeCode,
                                      String customerName,
                                      BigDecimal amount,
                                      String notes,
                                      Instant occurredAt) {
}
