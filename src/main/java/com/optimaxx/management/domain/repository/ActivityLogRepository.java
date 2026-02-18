package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.ActivityLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
}
