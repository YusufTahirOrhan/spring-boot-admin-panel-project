package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SecurityAuditService {

    private static final String REQUEST_ID_ATTRIBUTE = "request_id";

    private final ActivityLogRepository activityLogRepository;
    private final ClickhouseAuditPublisher clickhouseAuditPublisher;

    public SecurityAuditService(ActivityLogRepository activityLogRepository,
                                ClickhouseAuditPublisher clickhouseAuditPublisher) {
        this.activityLogRepository = activityLogRepository;
        this.clickhouseAuditPublisher = clickhouseAuditPublisher;
    }

    public void log(AuditEventType eventType, User actorUser, String resourceType, String resourceId, String afterJson) {
        HttpServletRequest request = currentRequest();

        ActivityLog activityLog = new ActivityLog();
        activityLog.setActorUserId(actorUser == null ? UUID.randomUUID() : actorUser.getId() == null ? UUID.randomUUID() : actorUser.getId());
        activityLog.setActorRole(actorUser == null || actorUser.getRole() == null ? "SYSTEM" : actorUser.getRole().name());
        activityLog.setAction(eventType.name());
        activityLog.setResourceType(resourceType);
        activityLog.setResourceId(resourceId == null ? "n/a" : resourceId);
        activityLog.setBeforeJson("{}");
        activityLog.setAfterJson(afterJson == null ? "{}" : afterJson);
        activityLog.setRequestId(resolveRequestId(request));
        activityLog.setIpAddress(resolveIpAddress(request));
        activityLog.setUserAgent(resolveUserAgent(request));
        activityLog.setOccurredAt(Instant.now());
        activityLog.setStoreId(actorUser == null || actorUser.getStoreId() == null
                ? com.optimaxx.management.security.StoreContext.currentStoreId()
                : actorUser.getStoreId());
        activityLog.setDeleted(false);

        ActivityLog saved = activityLogRepository.save(activityLog);
        clickhouseAuditPublisher.publish(saved);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }

        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String existingRequestId && !existingRequestId.isBlank()) {
            return existingRequestId;
        }

        String fromHeader = request.getHeader("X-Request-Id");
        String requestId = (fromHeader == null || fromHeader.isBlank()) ? UUID.randomUUID().toString() : fromHeader;
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
