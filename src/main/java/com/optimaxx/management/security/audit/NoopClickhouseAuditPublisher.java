package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class NoopClickhouseAuditPublisher implements ClickhouseAuditPublisher {

    @Override
    public void publish(ActivityLog activityLog) {
        // TODO: Replace with a concrete ClickHouse writer integration.
    }
}
