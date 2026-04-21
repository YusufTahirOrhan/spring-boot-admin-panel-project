package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.AdminAnalyticsService;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsCategoryTrendResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsHighRiskEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsLowStockItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsRevenueSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsStaffPerformanceResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Analytics Dashboard endpoints.
 * All endpoints require OWNER or ADMIN role.
 * SecurityConfig already gates /api/v1/admin/**, but @PreAuthorize here
 * makes the access control explicit and method-level testable.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    /**
     * GET /api/v1/admin/analytics/revenue
     * Returns revenue summary for the given ISO-8601 date range.
     *
     * @param startDate inclusive start (ISO-8601 Instant, e.g. 2026-01-01T00:00:00Z)
     * @param endDate   inclusive end   (ISO-8601 Instant)
     */
    @GetMapping("/revenue")
    public AnalyticsRevenueSummaryResponse getRevenueSummary(
            @RequestParam("startDate") Instant startDate,
            @RequestParam("endDate") Instant endDate) {
        return adminAnalyticsService.getRevenueSummary(startDate, endDate);
    }

    /**
     * GET /api/v1/admin/analytics/category-trend
     * Returns revenue grouped by TransactionType category for the given date range.
     */
    @GetMapping("/category-trend")
    public AnalyticsCategoryTrendResponse getCategoryTrend(
            @RequestParam("startDate") Instant startDate,
            @RequestParam("endDate") Instant endDate) {
        return adminAnalyticsService.getCategoryTrend(startDate, endDate);
    }

    /**
     * GET /api/v1/admin/analytics/staff-performance
     * Returns action counts per staff member for the given date range.
     */
    @GetMapping("/staff-performance")
    public AnalyticsStaffPerformanceResponse getStaffPerformance(
            @RequestParam("startDate") Instant startDate,
            @RequestParam("endDate") Instant endDate) {
        return adminAnalyticsService.getStaffPerformance(startDate, endDate);
    }

    /**
     * GET /api/v1/admin/analytics/low-stock
     * Returns all inventory items currently at or below their minQuantity threshold.
     */
    @GetMapping("/low-stock")
    public List<AnalyticsLowStockItem> getLowStockAlerts() {
        return adminAnalyticsService.getLowStockAlerts();
    }

    /**
     * GET /api/v1/admin/analytics/high-risk-events
     * Returns the most recent high-risk audit events (e.g. deletions, role changes, refunds).
     *
     * @param limit maximum events to return (default 50, max 200)
     */
    @GetMapping("/high-risk-events")
    public AnalyticsHighRiskEventResponse getHighRiskEvents(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        int safeLimit = Math.min(limit, 200);
        return adminAnalyticsService.getHighRiskEvents(safeLimit);
    }
}
