package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptVerificationResponse(boolean valid,
                                          String receiptNumber,
                                          String invoiceNumber,
                                          BigDecimal amount,
                                          String status,
                                          Instant occurredAt) {
}
