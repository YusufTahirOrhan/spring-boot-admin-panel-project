package com.optimaxx.management.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapOwnerProperties.class)
public class BootstrapOwnerConfig {
}
