package com.optimaxx.management.application;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.ActivityLogEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsCategoryTrendItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsCategoryTrendResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsHighRiskEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsLowStockItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsRevenueSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsStaffPerformanceItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsStaffPerformanceResponse;
import com.optimaxx.management.security.StoreContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Analytics service for the Admin Analytics Dashboard.
 * All methods are restricted to OWNER and ADMIN roles.
 */
@Service
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AdminAnalyticsService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserRepository userRepository;

    public AdminAnalyticsService(SaleTransactionRepository saleTransactionRepository,
                                 ActivityLogRepository activityLogRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 UserRepository userRepository) {
        this.saleTransactionRepository = saleTransactionRepository;
        this.activityLogRepository = activityLogRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns a revenue summary (total revenue, transaction count, average value)
     * for the given date range, scoped to the current store.
     */
    @Transactional(readOnly = true)
    public AnalyticsRevenueSummaryResponse getRevenueSummary(Instant from, Instant to) {
        UUID storeId = StoreContext.currentStoreId();

        BigDecimal totalRevenue = saleTransactionRepository.sumRevenueInRange(storeId, from, to);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long count = saleTransactionRepository.countInRange(storeId, from, to);

        BigDecimal avg = (count == 0)
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        return new AnalyticsRevenueSummaryResponse(totalRevenue, count, avg, from, to);
    }

    /**
     * Returns revenue broken down by TransactionType category for the given date range.
     */
    @Transactional(readOnly = true)
    public AnalyticsCategoryTrendResponse getCategoryTrend(Instant from, Instant to) {
        UUID storeId = StoreContext.currentStoreId();

        List<Object[]> rows = saleTransactionRepository.revenueByCategory(storeId, from, to);

        List<AnalyticsCategoryTrendItem> items = rows.stream()
                .map(row -> {
                    String category = row[0] == null ? "UNKNOWN" : row[0].toString();
                    BigDecimal revenue = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
                    return new AnalyticsCategoryTrendItem(category, revenue);
                })
                .toList();

        return new AnalyticsCategoryTrendResponse(items, from, to);
    }

    /**
     * Returns per-staff action counts (from ActivityLog) for the given date range,
     * enriched with the username where the user still exists.
     */
    @Transactional(readOnly = true)
    public AnalyticsStaffPerformanceResponse getStaffPerformance(Instant from, Instant to) {
        UUID storeId = StoreContext.currentStoreId();

        List<Object[]> rows = activityLogRepository.staffActionCounts(storeId, from, to);

        List<AnalyticsStaffPerformanceItem> staff = new ArrayList<>();
        for (Object[] row : rows) {
            UUID userId = null;
            try {
                userId = UUID.fromString(row[0].toString());
            } catch (IllegalArgumentException ignored) {
                // Corrupted actor_user_id – skip
                continue;
            }

            long actionCount = ((Number) row[1]).longValue();

            // Best-effort username enrichment – user may have been soft-deleted
            String username = "unknown";
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                username = userOpt.get().getUsername();
            }

            staff.add(new AnalyticsStaffPerformanceItem(userId, username, actionCount));
        }

        // Sort descending by actionCount
        staff.sort((a, b) -> Long.compare(b.actionCount(), a.actionCount()));

        return new AnalyticsStaffPerformanceResponse(staff, from, to);
    }

    /**
     * Returns all inventory items currently at or below their configured minimum quantity threshold.
     */
    @Transactional(readOnly = true)
    public List<AnalyticsLowStockItem> getLowStockAlerts() {
        List<InventoryItem> items = inventoryItemRepository.findLowStockItems();

        return items.stream()
                .map(item -> new AnalyticsLowStockItem(
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        item.getCategory(),
                        item.getQuantity(),
                        item.getMinQuantity(),
                        item.getMinQuantity() - item.getQuantity()
                ))
                .toList();
    }

    /**
     * Returns the most recent high-risk audit events (security-sensitive actions)
     * scoped to the current store.
     *
     * @param limit maximum number of events to return (defaults to 50 at the controller level)
     */
    @Transactional(readOnly = true)
    public AnalyticsHighRiskEventResponse getHighRiskEvents(int limit) {
        UUID storeId = StoreContext.currentStoreId();

        List<ActivityLog> logs = activityLogRepository.findHighRiskEvents(storeId, limit);

        List<ActivityLogEventResponse> events = logs.stream()
                .map(log -> new ActivityLogEventResponse(
                        log.getId(),
                        log.getActorUserId(),
                        log.getActorRole(),
                        log.getAction(),
                        log.getResourceType(),
                        log.getResourceId(),
                        log.getBeforeJson(),
                        log.getAfterJson(),
                        log.getRequestId(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getOccurredAt()
                ))
                .toList();

        return new AnalyticsHighRiskEventResponse(events, events.size());
    }
}
