package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.optimaxx.management.security.ForgotPasswordAttemptService;
import com.optimaxx.management.security.ForgotPasswordProtectionProperties;
import com.optimaxx.management.security.LoginAttemptService;
import com.optimaxx.management.security.LoginProtectionProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

class RedisAttemptFallbackTest {

    @Test
    void loginAttemptsFallBackToMemoryWhenRedisIsUnavailable() {
        LoginAttemptService service = new LoginAttemptService(
                new LoginProtectionProperties(2, 1),
                redisProviderThrowingOnValueOperations()
        );

        service.onFailedAttempt("owner");
        service.onFailedAttempt("owner");

        assertThatThrownBy(() -> service.checkBlocked("owner"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void forgotPasswordAttemptsFallBackToMemoryWhenRedisIsUnavailable() {
        ForgotPasswordAttemptService service = new ForgotPasswordAttemptService(
                new ForgotPasswordProtectionProperties(1, 5),
                redisProviderThrowingOnValueOperations()
        );

        service.checkAllowed("owner@optimaxx.local");

        assertThatThrownBy(() -> service.checkAllowed("owner@optimaxx.local"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many password reset requests");
    }

    private ObjectProvider<StringRedisTemplate> redisProviderThrowingOnValueOperations() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> redisProvider = Mockito.mock(ObjectProvider.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);

        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(Mockito.anyString())).thenThrow(new RuntimeException("redis unavailable"));
        when(valueOperations.increment(Mockito.anyString())).thenThrow(new RuntimeException("redis unavailable"));
        return redisProvider;
    }
}
