package com.optimaxx.management.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ForgotPasswordAttemptService {

    private static final String KEY_PREFIX = "auth:forgot-password:";

    private final ForgotPasswordProtectionProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, WindowState> attempts = new ConcurrentHashMap<>();

    public ForgotPasswordAttemptService(ForgotPasswordProtectionProperties properties,
                                        ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void checkAllowed(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        int maxRequests = Math.max(properties.maxRequests(), 1);
        long windowMinutes = Math.max(properties.windowMinutes(), 1);

        if (redisTemplate != null) {
            Long requestCount = redisTemplate.opsForValue().increment(counterKey(normalizedEmail));
            if (requestCount != null && requestCount == 1L) {
                redisTemplate.expire(counterKey(normalizedEmail), Duration.ofMinutes(windowMinutes));
            }
            if (requestCount != null && requestCount > maxRequests) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset requests. Try again later.");
            }
            return;
        }

        WindowState state = attempts.computeIfAbsent(normalizedEmail, ignored -> new WindowState(System.currentTimeMillis(), 0));
        long now = System.currentTimeMillis();
        long windowMillis = Duration.ofMinutes(windowMinutes).toMillis();

        if (now - state.windowStartedAtMillis() > windowMillis) {
            state.setWindowStartedAtMillis(now);
            state.setCount(0);
        }

        state.setCount(state.count() + 1);
        if (state.count() > maxRequests) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many password reset requests. Try again later.");
        }
    }

    public void clearAll() {
        attempts.clear();
    }

    private String counterKey(String email) {
        return KEY_PREFIX + email + ":count";
    }

    private static class WindowState {
        private long windowStartedAtMillis;
        private int count;

        private WindowState(long windowStartedAtMillis, int count) {
            this.windowStartedAtMillis = windowStartedAtMillis;
            this.count = count;
        }

        public long windowStartedAtMillis() {
            return windowStartedAtMillis;
        }

        public void setWindowStartedAtMillis(long windowStartedAtMillis) {
            this.windowStartedAtMillis = windowStartedAtMillis;
        }

        public int count() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
