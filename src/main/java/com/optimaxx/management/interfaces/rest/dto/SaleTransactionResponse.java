package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleTransactionResponse(UUID id,
                                      UUID transactionTypeId,
                                      String transactionTypeCode,
                                      String receiptNumber,
                                      String status,
                                      UUID customerId,
                                      String customerName,
                                      BigDecimal amount,
                                      BigDecimal refundedAmount,
                                      Instant refundedAt,
                                      String notes,
                                      Instant occurredAt) {
}
