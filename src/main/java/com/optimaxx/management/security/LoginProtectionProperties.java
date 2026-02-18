package com.optimaxx.management.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.login-protection")
public record LoginProtectionProperties(int maxFailures, long lockMinutes) {
}
