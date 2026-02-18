package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    private final ActivityLogRepository activityLogRepository;
    private final ClickhouseAuditPublisher clickhouseAuditPublisher;

    public SecurityAuditService(ActivityLogRepository activityLogRepository,
                                ClickhouseAuditPublisher clickhouseAuditPublisher) {
        this.activityLogRepository = activityLogRepository;
        this.clickhouseAuditPublisher = clickhouseAuditPublisher;
    }

    public void log(AuditEventType eventType, User actorUser, String resourceType, String resourceId, String afterJson) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setActorUserId(actorUser == null ? UUID.randomUUID() : actorUser.getId() == null ? UUID.randomUUID() : actorUser.getId());
        activityLog.setActorRole(actorUser == null || actorUser.getRole() == null ? "SYSTEM" : actorUser.getRole().name());
        activityLog.setAction(eventType.name());
        activityLog.setResourceType(resourceType);
        activityLog.setResourceId(resourceId == null ? "n/a" : resourceId);
        activityLog.setBeforeJson("{}");
        activityLog.setAfterJson(afterJson == null ? "{}" : afterJson);
        activityLog.setRequestId(UUID.randomUUID().toString());
        activityLog.setIpAddress(null);
        activityLog.setUserAgent(null);
        activityLog.setOccurredAt(Instant.now());
        activityLog.setStoreId(actorUser == null || actorUser.getStoreId() == null ? UUID.randomUUID() : actorUser.getStoreId());
        activityLog.setDeleted(false);

        ActivityLog saved = activityLogRepository.save(activityLog);
        clickhouseAuditPublisher.publish(saved);
    }
}
