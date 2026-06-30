package com.optimaxx.management.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SiteAssetProperties.class)
public class SiteAssetConfig {
}
