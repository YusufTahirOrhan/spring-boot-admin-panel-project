package com.optimaxx.management.config;

import io.lettuce.core.SslVerifyMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HerokuRedisConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.url")
    LettuceClientConfigurationBuilderCustomizer herokuRedisTlsCustomizer() {
        return builder -> builder.useSsl().verifyPeer(SslVerifyMode.NONE);
    }
}
