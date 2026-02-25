package com.optimaxx.management.security.audit;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.interfaces.rest.dto.ActivityLogEventResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditQueryService {

    private final ActivityLogRepository activityLogRepository;

    public AdminAuditQueryService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogEventResponse> query(UUID actorUserId,
                                                String eventType,
                                                String resourceType,
                                                Instant from,
                                                Instant to,
                                                int page,
                                                int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));

        Specification<ActivityLog> spec = Specification.where((root, query, cb) -> cb.isFalse(root.get("deleted")));

        if (actorUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), eventType.trim()));
        }
        if (resourceType != null && !resourceType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), resourceType.trim()));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }

        return activityLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private ActivityLogEventResponse toResponse(ActivityLog log) {
        return new ActivityLogEventResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorRole(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getBeforeJson(),
                log.getAfterJson(),
                log.getRequestId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getOccurredAt()
        );
    }
}
