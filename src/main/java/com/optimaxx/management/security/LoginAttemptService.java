package com.optimaxx.management.security;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "auth:login:";

    private final LoginProtectionProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginProtectionProperties properties,
                               ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void checkBlocked(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String normalizedUsername = username.toLowerCase();

        if (redisTemplate != null) {
            String lockedUntil = redisTemplate.opsForValue().get(lockKey(normalizedUsername));
            if (lockedUntil != null) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
            }
            return;
        }

        AttemptState state = attempts.get(normalizedUsername);
        if (state == null || state.lockedUntil == null) {
            return;
        }

        if (state.lockedUntil.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
        }

        state.failedCount = 0;
        state.lockedUntil = null;
    }

    public void onFailedAttempt(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String normalizedUsername = username.toLowerCase();
        int maxFailures = Math.max(properties.maxFailures(), 1);
        long lockMinutes = Math.max(properties.lockMinutes(), 1);

        if (redisTemplate != null) {
            Long failedCount = redisTemplate.opsForValue().increment(failKey(normalizedUsername));
            if (failedCount != null && failedCount >= maxFailures) {
                redisTemplate.opsForValue().set(
                        lockKey(normalizedUsername),
                        Instant.now().plus(lockMinutes, ChronoUnit.MINUTES).toString(),
                        Duration.ofMinutes(lockMinutes)
                );
                redisTemplate.delete(failKey(normalizedUsername));
            }
            return;
        }

        AttemptState state = attempts.computeIfAbsent(normalizedUsername, key -> new AttemptState());
        state.failedCount++;

        if (state.failedCount >= maxFailures) {
            state.lockedUntil = Instant.now().plus(lockMinutes, ChronoUnit.MINUTES);
        }
    }

    public void onSuccessfulAttempt(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String normalizedUsername = username.toLowerCase();

        if (redisTemplate != null) {
            redisTemplate.delete(failKey(normalizedUsername));
            redisTemplate.delete(lockKey(normalizedUsername));
            return;
        }

        attempts.remove(normalizedUsername);
    }

    public void clearAll() {
        attempts.clear();
    }

    private String failKey(String username) {
        return KEY_PREFIX + username + ":failed";
    }

    private String lockKey(String username) {
        return KEY_PREFIX + username + ":locked";
    }

    private static class AttemptState {
        private int failedCount;
        private Instant lockedUntil;
    }
}
