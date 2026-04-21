package com.optimaxx.management.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsCategoryTrendResponse(
        List<AnalyticsCategoryTrendItem> items,
        Instant startDate,
        Instant endDate
) {}
