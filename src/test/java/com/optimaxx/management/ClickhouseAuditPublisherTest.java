package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.security.audit.ClickhouseAuditRetryProperties;
import com.optimaxx.management.security.audit.ClickhouseProperties;
import com.optimaxx.management.security.audit.ResilientClickhouseAuditPublisher;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClickhouseAuditPublisherTest {

    @Test
    void shouldIgnorePublishWhenClickhouseUrlIsBlank() {
        ClickhouseProperties properties = new ClickhouseProperties("", "", "");
        ClickhouseAuditRetryProperties retryProperties = new ClickhouseAuditRetryProperties();
        ResilientClickhouseAuditPublisher publisher = new ResilientClickhouseAuditPublisher(properties, retryProperties, new SimpleMeterRegistry());

        publisher.publish(createActivityLog());
        publisher.flushRetryQueue();

        assertThat(publisher.getPendingQueueSize()).isZero();
        assertThat(publisher.getPublishFailureCount()).isZero();
        assertThat(publisher.getRetryAttemptCount()).isZero();
    }

    @Test
    void shouldNotThrowWhenClickhouseEndpointIsUnavailable() {
        ClickhouseProperties properties = new ClickhouseProperties("http://localhost:65534/default", "default", "");
        ClickhouseAuditRetryProperties retryProperties = new ClickhouseAuditRetryProperties();
        ResilientClickhouseAuditPublisher publisher = new ResilientClickhouseAuditPublisher(properties, retryProperties, new SimpleMeterRegistry());

        ActivityLog activityLog = createActivityLog();

        assertThatCode(() -> publisher.publish(activityLog)).doesNotThrowAnyException();
    }

    @Test
    void shouldQueueFailedAuditForRetry() {
        ClickhouseProperties properties = new ClickhouseProperties("http://localhost:65534/default", "default", "");
        ClickhouseAuditRetryProperties retryProperties = new ClickhouseAuditRetryProperties();
        retryProperties.setMaxQueueSize(10);
        retryProperties.setMaxAttempts(2);
        retryProperties.setFlushBatchSize(5);

        ResilientClickhouseAuditPublisher publisher = new ResilientClickhouseAuditPublisher(properties, retryProperties, new SimpleMeterRegistry());

        publisher.publish(createActivityLog());

        assertThat(publisher.getPendingQueueSize()).isGreaterThan(0);

        publisher.flushRetryQueue();

        assertThat(publisher.getDroppedCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldFormatTimestampForClickhouseJsonEachRow() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            ClickhouseProperties properties = new ClickhouseProperties("http://localhost:" + server.getAddress().getPort(), "", "");
            ClickhouseAuditRetryProperties retryProperties = new ClickhouseAuditRetryProperties();
            ResilientClickhouseAuditPublisher publisher = new ResilientClickhouseAuditPublisher(properties, retryProperties, new SimpleMeterRegistry());

            ActivityLog activityLog = createActivityLog();
            activityLog.setOccurredAt(Instant.parse("2026-06-29T17:28:23.290Z"));

            publisher.publish(activityLog);

            assertThat(capturedBody.get()).contains("\"timestamp\":\"2026-06-29 17:28:23.290\"");
            assertThat(capturedBody.get()).doesNotContain("2026-06-29T17:28:23.290Z");
        } finally {
            server.stop(0);
        }
    }

    private ActivityLog createActivityLog() {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setActorUserId(UUID.randomUUID());
        activityLog.setActorRole("OWNER");
        activityLog.setAction("LOGIN_SUCCESS");
        activityLog.setResourceType("AUTH");
        activityLog.setResourceId("owner");
        activityLog.setBeforeJson("{}");
        activityLog.setAfterJson("{}");
        activityLog.setRequestId(UUID.randomUUID().toString());
        activityLog.setOccurredAt(Instant.now());
        activityLog.setStoreId(UUID.randomUUID());
        return activityLog;
    }
}
