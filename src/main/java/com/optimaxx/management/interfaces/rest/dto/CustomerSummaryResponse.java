package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CustomerSummaryResponse(UUID customerId,
                                      String firstName,
                                      String lastName,
                                      long salesCount,
                                      BigDecimal totalSalesAmount,
                                      long repairCount,
                                      Map<String, Long> repairStatusCounts,
                                      long prescriptionCount,
                                      Instant lastSaleAt,
                                      Instant lastRepairAt,
                                      Instant lastPrescriptionAt) {
}
