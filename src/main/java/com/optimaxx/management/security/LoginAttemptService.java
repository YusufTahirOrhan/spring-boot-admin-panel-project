package com.optimaxx.management.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginAttemptService {

    private final LoginProtectionProperties properties;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginProtectionProperties properties) {
        this.properties = properties;
    }

    public void checkBlocked(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        AttemptState state = attempts.get(username.toLowerCase());
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

        AttemptState state = attempts.computeIfAbsent(username.toLowerCase(), key -> new AttemptState());
        state.failedCount++;

        if (state.failedCount >= Math.max(properties.maxFailures(), 1)) {
            state.lockedUntil = Instant.now().plus(Math.max(properties.lockMinutes(), 1), ChronoUnit.MINUTES);
        }
    }

    public void onSuccessfulAttempt(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        attempts.remove(username.toLowerCase());
    }

    public void clearAll() {
        attempts.clear();
    }

    private static class AttemptState {
        private int failedCount;
        private Instant lockedUntil;
    }
}
