package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleTransactionDetailResponse(UUID id,
                                            UUID transactionTypeId,
                                            String transactionTypeCode,
                                            String receiptNumber,
                                            String status,
                                            String paymentMethod,
                                            String paymentReference,
                                            UUID customerId,
                                            String customerName,
                                            BigDecimal amount,
                                            BigDecimal refundedAmount,
                                            Instant refundedAt,
                                            String notes,
                                            Instant occurredAt,
                                            List<SaleTransactionTimelineEventResponse> timeline) {
}
