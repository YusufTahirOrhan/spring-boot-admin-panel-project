package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;

public record RefundSaleTransactionRequest(BigDecimal amount,
                                           String reason) {
}
