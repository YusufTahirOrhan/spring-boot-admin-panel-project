package com.optimaxx.management.security.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clickhouse")
public record ClickhouseProperties(String url, String username, String password) {
}
