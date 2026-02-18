package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;

public interface ClickhouseAuditPublisher {

    void publish(ActivityLog activityLog);
}
