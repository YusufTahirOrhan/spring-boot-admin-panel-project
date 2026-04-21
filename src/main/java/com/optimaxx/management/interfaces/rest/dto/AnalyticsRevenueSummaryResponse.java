package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsRevenueSummaryResponse(
        BigDecimal totalRevenue,
        long transactionCount,
        BigDecimal avgTransactionValue,
        Instant startDate,
        Instant endDate
) {}
