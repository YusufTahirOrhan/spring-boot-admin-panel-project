package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleTransactionSummaryResponse(long transactionCount,
                                             BigDecimal grossAmount,
                                             BigDecimal refundedAmount,
                                             BigDecimal netAmount,
                                             List<SalePaymentMethodSummaryResponse> paymentMethodBreakdown) {
}
