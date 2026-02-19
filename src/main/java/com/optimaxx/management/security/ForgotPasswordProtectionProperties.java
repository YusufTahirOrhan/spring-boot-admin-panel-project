package com.optimaxx.management.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.forgot-password-protection")
public record ForgotPasswordProtectionProperties(int maxRequests, long windowMinutes) {
}
