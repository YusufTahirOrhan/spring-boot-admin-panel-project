package com.optimaxx.management.interfaces.rest.dto;

import java.math.BigDecimal;

public record AnalyticsCategoryTrendItem(
        String category,
        BigDecimal totalRevenue
) {}
