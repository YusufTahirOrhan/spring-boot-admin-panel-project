package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.ActivityLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {

    List<ActivityLog> findByStoreIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByOccurredAtDesc(UUID storeId,
                                                                                                     String resourceType,
                                                                                                     String resourceId);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM activity_logs a WHERE a.store_id = :storeId AND a.is_deleted = false AND " +
                "( (a.resource_type = 'SALE_TRANSACTION' AND a.resource_id = :transactionId) OR " +
                "  (a.resource_type = 'INVENTORY' AND (CAST(a.after_json AS jsonb) ->> 'saleTransactionId') = :transactionId) ) " +
                "ORDER BY a.occurred_at DESC", 
        nativeQuery = true)
    List<ActivityLog> findUnifiedTimelineForSaleTransaction(@org.springframework.data.repository.query.Param("storeId") UUID storeId,
                                                            @org.springframework.data.repository.query.Param("transactionId") String transactionId);

    // ── Analytics queries ────────────────────────────────────────────────────

    @Query(value = "SELECT a.actor_user_id, count(*) as action_count " +
                   "FROM activity_logs a " +
                   "WHERE a.store_id = :storeId AND a.is_deleted = false " +
                   "  AND a.occurred_at >= :from AND a.occurred_at <= :to " +
                   "GROUP BY a.actor_user_id", nativeQuery = true)
    List<Object[]> staffActionCounts(@Param("storeId") UUID storeId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    @Query(value = "SELECT * FROM activity_logs a " +
                   "WHERE a.store_id = :storeId AND a.is_deleted = false " +
                   "  AND a.action IN ('USER_DELETED','USER_ROLE_CHANGED','TRANSACTION_CANCELED', " +
                   "                   'TRANSACTION_REFUNDED','STOCK_ADJUSTED') " +
                   "ORDER BY a.occurred_at DESC LIMIT :limitRows", nativeQuery = true)
    List<ActivityLog> findHighRiskEvents(@Param("storeId") UUID storeId,
                                         @Param("limitRows") int limitRows);
}
