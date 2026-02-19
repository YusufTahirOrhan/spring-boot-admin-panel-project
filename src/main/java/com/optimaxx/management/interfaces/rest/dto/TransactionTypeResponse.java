package com.optimaxx.management.interfaces.rest.dto;

import com.optimaxx.management.domain.model.TransactionTypeCategory;
import java.util.UUID;

public record TransactionTypeResponse(UUID id,
                                      String code,
                                      String name,
                                      boolean active,
                                      int sortOrder,
                                      TransactionTypeCategory category,
                                      String metadataJson) {
}
