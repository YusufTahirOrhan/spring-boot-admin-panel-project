package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.ClickhouseAuditPublisher;
import com.optimaxx.management.security.audit.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SecurityAuditServiceTest {

    @AfterEach
    void cleanContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldCaptureRequestContextIntoAuditLog() {
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        ClickhouseAuditPublisher clickhouseAuditPublisher = Mockito.mock(ClickhouseAuditPublisher.class);

        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAuditService securityAuditService = new SecurityAuditService(activityLogRepository, clickhouseAuditPublisher);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit-Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        User actor = new User();
        actor.setUsername("owner");
        actor.setRole(UserRole.OWNER);
        actor.setStoreId(UUID.randomUUID());

        securityAuditService.log(AuditEventType.LOGIN_SUCCESS, actor, "AUTH", "owner", "{\"status\":\"success\"}");

        verify(activityLogRepository).save(any(ActivityLog.class));
        verify(clickhouseAuditPublisher).publish(any(ActivityLog.class));
        verify(request).setAttribute("request_id", "req-123");
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderMissing() {
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        ClickhouseAuditPublisher clickhouseAuditPublisher = Mockito.mock(ClickhouseAuditPublisher.class);

        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityAuditService securityAuditService = new SecurityAuditService(activityLogRepository, clickhouseAuditPublisher);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        securityAuditService.log(AuditEventType.LOGIN_FAILED, null, "AUTH", "unknown", "{}");

        verify(request).setAttribute(Mockito.eq("request_id"), Mockito.argThat(value -> {
            assertThat(String.valueOf(value)).isNotBlank();
            return true;
        }));
    }
}
