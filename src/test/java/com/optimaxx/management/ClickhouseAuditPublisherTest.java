package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.security.audit.ClickhouseProperties;
import com.optimaxx.management.security.audit.NoopClickhouseAuditPublisher;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClickhouseAuditPublisherTest {

    @Test
    void shouldNotThrowWhenClickhouseEndpointIsUnavailable() {
        ClickhouseProperties properties = new ClickhouseProperties("http://localhost:65534/default", "default", "");
        NoopClickhouseAuditPublisher publisher = new NoopClickhouseAuditPublisher(properties);

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

        assertThatCode(() -> publisher.publish(activityLog)).doesNotThrowAnyException();
    }
}
