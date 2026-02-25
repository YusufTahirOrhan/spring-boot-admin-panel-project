package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;

public record SalePaymentMethodSummaryResponse(String paymentMethod,
                                               long transactionCount,
                                               BigDecimal grossAmount,
                                               BigDecimal refundedAmount,
                                               BigDecimal netAmount) {
}
