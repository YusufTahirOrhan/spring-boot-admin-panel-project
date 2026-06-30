package com.optimaxx.management.config;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class HerokuEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "herokuUrlEnvironment";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new LinkedHashMap<>();
        addDatabaseProperties(environment, properties);
        addRedisProperties(environment, properties);

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void addDatabaseProperties(ConfigurableEnvironment environment, Map<String, Object> properties) {
        if (StringUtils.hasText(systemEnv("SPRING_DATASOURCE_URL"))) {
            return;
        }

        String jdbcDatabaseUrl = firstText(systemEnv("JDBC_DATABASE_URL"), environment.getProperty("JDBC_DATABASE_URL"));
        if (StringUtils.hasText(jdbcDatabaseUrl)) {
            properties.put("spring.datasource.url", jdbcDatabaseUrl);
            putIfPresent(properties, "spring.datasource.username", firstText(systemEnv("JDBC_DATABASE_USERNAME"), environment.getProperty("JDBC_DATABASE_USERNAME")));
            putIfPresent(properties, "spring.datasource.password", firstText(systemEnv("JDBC_DATABASE_PASSWORD"), environment.getProperty("JDBC_DATABASE_PASSWORD")));
            return;
        }

        String databaseUrl = firstText(systemEnv("DATABASE_URL"), environment.getProperty("DATABASE_URL"));
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String scheme = "postgres".equals(uri.getScheme()) ? "postgresql" : uri.getScheme();
        StringBuilder jdbcUrl = new StringBuilder("jdbc:")
                .append(scheme)
                .append("://")
                .append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        if (StringUtils.hasText(uri.getRawPath())) {
            jdbcUrl.append(uri.getRawPath());
        }

        String query = uri.getRawQuery();
        if (StringUtils.hasText(query)) {
            jdbcUrl.append('?').append(query);
            if (!query.toLowerCase().contains("sslmode=")) {
                jdbcUrl.append("&sslmode=require");
            }
        } else {
            jdbcUrl.append("?sslmode=require");
        }

        properties.put("spring.datasource.url", jdbcUrl.toString());

        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            putIfPresent(properties, "spring.datasource.username", decode(parts[0]));
            if (parts.length > 1) {
                putIfPresent(properties, "spring.datasource.password", decode(parts[1]));
            }
        }
    }

    private void addRedisProperties(ConfigurableEnvironment environment, Map<String, Object> properties) {
        if (StringUtils.hasText(systemEnv("SPRING_DATA_REDIS_URL"))) {
            return;
        }

        String redisUrl = firstText(systemEnv("REDIS_URL"), environment.getProperty("REDIS_URL"));
        if (StringUtils.hasText(redisUrl)) {
            properties.put("spring.data.redis.url", redisUrl);
        }
    }

    private void putIfPresent(Map<String, Object> properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(key, value);
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String systemEnv(String name) {
        return System.getenv(name);
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is not available", exception);
        }
    }
}
