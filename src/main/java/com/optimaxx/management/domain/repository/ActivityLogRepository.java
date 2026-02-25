package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.ActivityLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {

    List<ActivityLog> findByStoreIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByOccurredAtDesc(UUID storeId,
                                                                                                     String resourceType,
                                                                                                     String resourceId);
}
