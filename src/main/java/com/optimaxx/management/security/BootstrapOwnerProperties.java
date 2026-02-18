package com.optimaxx.management.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap.owner")
public record BootstrapOwnerProperties(
        boolean enabled,
        String username,
        String email,
        String password
) {
}
