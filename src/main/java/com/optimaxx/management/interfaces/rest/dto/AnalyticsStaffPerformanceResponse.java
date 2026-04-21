package com.optimaxx.management.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsStaffPerformanceResponse(
        List<AnalyticsStaffPerformanceItem> staff,
        Instant startDate,
        Instant endDate
) {}
