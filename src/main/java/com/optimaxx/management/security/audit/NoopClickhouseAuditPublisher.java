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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopClickhouseAuditPublisher implements ClickhouseAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopClickhouseAuditPublisher.class);
    private static final String INSERT_QUERY = "INSERT INTO audit_events FORMAT JSONEachRow";

    private final ClickhouseProperties clickhouseProperties;
    private final HttpClient httpClient;

    public NoopClickhouseAuditPublisher(ClickhouseProperties clickhouseProperties) {
        this.clickhouseProperties = clickhouseProperties;
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
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("ClickHouse audit publish failed: {}", ex.getMessage());
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
}
