package com.optimaxx.management.security.audit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClickhouseProperties.class)
public class ClickhouseAuditConfig {
}
