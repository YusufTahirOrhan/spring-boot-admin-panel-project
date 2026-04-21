package com.optimaxx.management.interfaces.rest.dto;

import java.util.List;

public record AnalyticsHighRiskEventResponse(
        List<ActivityLogEventResponse> events,
        int total
) {}
