package com.optimaxx.management.interfaces.rest.dto;

import java.util.UUID;

public record AnalyticsStaffPerformanceItem(
        UUID userId,
        String username,
        long actionCount
) {}
