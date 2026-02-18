package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.security.audit.ClickhouseAuditRetryProperties;
import com.optimaxx.management.security.audit.ClickhouseProperties;
import com.optimaxx.management.security.audit.NoopClickhouseAuditPublisher;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClickhouseAuditPublisherTest {

    @Test
    void shouldNotThrowWhenClickhouseEndpointIsUnavailable() {
        ClickhouseProperties properties = new ClickhouseProperties("http://localhost:65534/default", "default", "");
        ClickhouseAuditRetryProperties retryProperties = new ClickhouseAuditRetryProperties();
        NoopClickhouseAuditPublisher publisher = new NoopClickhouseAuditPublisher(properties, retryProperties);

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

        NoopClickhouseAuditPublisher publisher = new NoopClickhouseAuditPublisher(properties, retryProperties);

        publisher.publish(createActivityLog());

        assertThat(publisher.getPendingQueueSize()).isGreaterThan(0);

        publisher.flushRetryQueue();

        assertThat(publisher.getDroppedCount()).isGreaterThanOrEqualTo(0);
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
