package vn.mar.audit.service;

import org.springframework.stereotype.Service;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.common.time.TimeProvider;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final TimeProvider timeProvider;
    private final AuditPayloadSanitizer auditPayloadSanitizer;

    public AuditService(
            AuditEventRepository auditEventRepository,
            TimeProvider timeProvider,
            AuditPayloadSanitizer auditPayloadSanitizer) {
        this.auditEventRepository = auditEventRepository;
        this.timeProvider = timeProvider;
        this.auditPayloadSanitizer = auditPayloadSanitizer;
    }

    public void record(AuditRecordCommand command) {
        auditEventRepository.save(AuditEvent.create(sanitize(command), timeProvider.now()));
    }

    private AuditRecordCommand sanitize(AuditRecordCommand command) {
        return new AuditRecordCommand(
                command.tenantId(),
                command.actorId(),
                command.actorType(),
                command.actorRole(),
                command.action(),
                command.resourceType(),
                command.resourceId(),
                command.resourceKey(),
                auditPayloadSanitizer.sanitize(command.beforeData()),
                auditPayloadSanitizer.sanitize(command.afterData()),
                auditPayloadSanitizer.sanitize(command.metadata()),
                command.reason(),
                command.requestId()
        );
    }
}
