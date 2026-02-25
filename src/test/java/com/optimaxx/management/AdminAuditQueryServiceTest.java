package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.security.audit.AdminAuditQueryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdminAuditQueryServiceTest {

    @Test
    void shouldQueryAuditEventsWithFilters() {
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);

        ActivityLog log = new ActivityLog();
        log.setActorUserId(UUID.randomUUID());
        log.setActorRole("OWNER");
        log.setAction("LOGIN_SUCCESS");
        log.setResourceType("AUTH");
        log.setResourceId("owner");
        log.setBeforeJson("{}");
        log.setAfterJson("{}");
        log.setRequestId("req-1");
        log.setOccurredAt(Instant.now());
        log.setDeleted(false);

        when(activityLogRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        AdminAuditQueryService service = new AdminAuditQueryService(activityLogRepository);

        var result = service.query(
                null,
                "LOGIN_SUCCESS",
                "AUTH",
                Instant.now().minusSeconds(3600),
                Instant.now(),
                0,
                20
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).action()).isEqualTo("LOGIN_SUCCESS");
    }
}
