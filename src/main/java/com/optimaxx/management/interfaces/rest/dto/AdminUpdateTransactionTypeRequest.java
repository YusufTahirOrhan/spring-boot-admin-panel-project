package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.TransactionTypeCategory;

public record AdminUpdateTransactionTypeRequest(String name,
                                                Boolean active,
                                                Integer sortOrder,
                                                TransactionTypeCategory category,
                                                String metadataJson) {
}
