package com.optimaxx.management.security.audit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({ClickhouseProperties.class, ClickhouseAuditRetryProperties.class})
public class ClickhouseAuditConfig {
}
