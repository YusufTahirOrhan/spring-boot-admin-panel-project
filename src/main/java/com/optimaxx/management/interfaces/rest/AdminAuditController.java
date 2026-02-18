package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AuditRetryStatusResponse;
import com.optimaxx.management.security.audit.NoopClickhouseAuditPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final NoopClickhouseAuditPublisher auditPublisher;

    public AdminAuditController(NoopClickhouseAuditPublisher auditPublisher) {
        this.auditPublisher = auditPublisher;
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
}
