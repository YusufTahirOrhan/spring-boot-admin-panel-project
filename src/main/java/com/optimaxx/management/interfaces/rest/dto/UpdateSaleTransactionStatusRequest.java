package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.SaleTransactionStatus;

public record UpdateSaleTransactionStatusRequest(SaleTransactionStatus status) {
}
