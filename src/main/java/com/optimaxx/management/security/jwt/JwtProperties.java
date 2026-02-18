package com.optimaxx.management.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secretKey, long accessTokenMinutes, long refreshTokenMinutes, String issuer) {
}
