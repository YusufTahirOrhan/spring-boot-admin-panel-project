package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.ActivityLogEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AuditRetryStatusResponse;
import com.optimaxx.management.security.audit.AdminAuditQueryService;
import com.optimaxx.management.security.audit.ResilientClickhouseAuditPublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final ResilientClickhouseAuditPublisher auditPublisher;
    private final AdminAuditQueryService adminAuditQueryService;

    public AdminAuditController(ResilientClickhouseAuditPublisher auditPublisher,
                                AdminAuditQueryService adminAuditQueryService) {
        this.auditPublisher = auditPublisher;
        this.adminAuditQueryService = adminAuditQueryService;
    }

    @GetMapping("/retry-status")
    public AuditRetryStatusResponse getRetryStatus() {
        return new AuditRetryStatusResponse(
                auditPublisher.getPendingQueueSize(),
                auditPublisher.getDroppedCount(),
                auditPublisher.getPublishFailureCount(),
                auditPublisher.getRetryAttemptCount(),
                auditPublisher.getPublishedSuccessCount()
        );
    }

    @GetMapping("/events")
    public Page<ActivityLogEventResponse> getEvents(@RequestParam(value = "actorUserId", required = false) UUID actorUserId,
                                                    @RequestParam(value = "eventType", required = false) String eventType,
                                                    @RequestParam(value = "resourceType", required = false) String resourceType,
                                                    @RequestParam(value = "from", required = false) Instant from,
                                                    @RequestParam(value = "to", required = false) Instant to,
                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return adminAuditQueryService.query(actorUserId, eventType, resourceType, from, to, page, size);
    }
}
