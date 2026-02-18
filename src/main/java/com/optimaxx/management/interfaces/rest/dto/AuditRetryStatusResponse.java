package com.optimaxx.management.interfaces.rest.dto;

public record AuditRetryStatusResponse(
        int pendingQueueSize,
        long droppedCount,
        long publishFailureCount,
        long retryAttemptCount,
        long publishedSuccessCount
) {
}
