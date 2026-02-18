package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoopClickhouseAuditPublisher implements ClickhouseAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopClickhouseAuditPublisher.class);
    private static final String INSERT_QUERY = "INSERT INTO audit_events FORMAT JSONEachRow";

    private final ClickhouseProperties clickhouseProperties;
    private final ClickhouseAuditRetryProperties retryProperties;
    private final HttpClient httpClient;
    private final Queue<PendingAudit> retryQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong droppedCount = new AtomicLong(0);

    public NoopClickhouseAuditPublisher(ClickhouseProperties clickhouseProperties,
                                        ClickhouseAuditRetryProperties retryProperties) {
        this.clickhouseProperties = clickhouseProperties;
        this.retryProperties = retryProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Override
    public void publish(ActivityLog activityLog) {
        if (activityLog == null || isBlank(clickhouseProperties.url())) {
            return;
        }

        String payload = toJsonEachRow(activityLog);
        if (!sendPayload(payload)) {
            enqueue(payload, 1);
        }
    }

    @Scheduled(fixedDelayString = "${clickhouse.audit.retry.fixed-delay-ms:5000}")
    public void flushRetryQueue() {
        int batchSize = Math.max(retryProperties.getFlushBatchSize(), 1);
        for (int i = 0; i < batchSize; i++) {
            PendingAudit pending = retryQueue.poll();
            if (pending == null) {
                return;
            }

            boolean success = sendPayload(pending.payload());
            if (!success) {
                int nextAttempt = pending.attempts() + 1;
                if (nextAttempt > Math.max(retryProperties.getMaxAttempts(), 1)) {
                    droppedCount.incrementAndGet();
                    log.warn("Dropping ClickHouse audit after {} attempts", pending.attempts());
                } else {
                    enqueue(pending.payload(), nextAttempt);
                }
            }
        }
    }

    public int getPendingQueueSize() {
        return retryQueue.size();
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }

    private void enqueue(String payload, int attempts) {
        int maxQueueSize = Math.max(retryProperties.getMaxQueueSize(), 1);
        if (retryQueue.size() >= maxQueueSize) {
            retryQueue.poll();
            droppedCount.incrementAndGet();
            log.warn("ClickHouse audit retry queue full, dropping oldest event");
        }
        retryQueue.offer(new PendingAudit(payload, attempts));
    }

    private boolean sendPayload(String payload) {
        String endpoint = clickhouseProperties.url() + "?query=" + URLEncoder.encode(INSERT_QUERY, StandardCharsets.UTF_8);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (!isBlank(clickhouseProperties.username())) {
                String password = clickhouseProperties.password() == null ? "" : clickhouseProperties.password();
                String credentials = clickhouseProperties.username() + ":" + password;
                requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("ClickHouse audit publish failed with status {}: {}", response.statusCode(), response.body());
                return false;
            }
            return true;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("ClickHouse audit publish failed: {}", ex.getMessage());
            return false;
        }
    }

    private String toJsonEachRow(ActivityLog logItem) {
        return "{" +
                "\"event_id\":\"" + safe(logItem.getId()) + "\"," +
                "\"timestamp\":\"" + safe(logItem.getOccurredAt()) + "\"," +
                "\"actor_user_id\":\"" + safe(logItem.getActorUserId()) + "\"," +
                "\"actor_role\":\"" + escape(logItem.getActorRole()) + "\"," +
                "\"action\":\"" + escape(logItem.getAction()) + "\"," +
                "\"resource_type\":\"" + escape(logItem.getResourceType()) + "\"," +
                "\"resource_id\":\"" + escape(logItem.getResourceId()) + "\"," +
                "\"before_json\":\"" + escape(logItem.getBeforeJson()) + "\"," +
                "\"after_json\":\"" + escape(logItem.getAfterJson()) + "\"," +
                "\"request_id\":\"" + escape(logItem.getRequestId()) + "\"," +
                "\"ip_address\":\"" + escape(logItem.getIpAddress()) + "\"," +
                "\"user_agent\":\"" + escape(logItem.getUserAgent()) + "\"," +
                "\"store_id\":\"" + safe(logItem.getStoreId()) + "\"" +
                "}";
    }

    private String safe(Object value) {
        return value == null ? "" : escape(String.valueOf(value));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PendingAudit(String payload, int attempts) {
    }
}
