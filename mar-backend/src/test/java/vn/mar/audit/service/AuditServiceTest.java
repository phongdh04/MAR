package vn.mar.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.common.time.TimeProvider;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID RESOURCE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        auditService = new AuditService(auditEventRepository, timeProvider, new AuditPayloadSanitizer());
    }

    @Test
    void record_whenPayloadContainsSensitiveKeys_shouldSanitizeBeforeSaving() {
        auditService.record(new AuditRecordCommand(
                TENANT_ID,
                ACTOR_ID,
                "USER",
                "ADMIN",
                AuditActions.USER_UPDATED,
                AuditResourceTypes.USER,
                RESOURCE_ID,
                "user@example.com",
                Map.of("status", "ACTIVE", "password_hash", "hash-value"),
                Map.of("status", "INACTIVE", "access_token", "token-value"),
                Map.of("request_id", "req_audit_service_001", "secret", "secret-value"),
                "Update user",
                "req_audit_service_001"
        ));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent savedEvent = captor.getValue();
        assertThat(savedEvent.createdAt()).isEqualTo(NOW);
        assertThat(savedEvent.beforeData()).containsEntry("password_hash", "[REDACTED]");
        assertThat(savedEvent.afterData()).containsEntry("access_token", "[REDACTED]");
        assertThat(savedEvent.metadata()).containsEntry("secret", "[REDACTED]");
        assertThat(savedEvent.requestId()).isEqualTo("req_audit_service_001");
    }
}
