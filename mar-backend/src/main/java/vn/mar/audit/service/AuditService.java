package vn.mar.audit.service;

import org.springframework.stereotype.Service;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.common.time.TimeProvider;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final TimeProvider timeProvider;

    public AuditService(AuditEventRepository auditEventRepository, TimeProvider timeProvider) {
        this.auditEventRepository = auditEventRepository;
        this.timeProvider = timeProvider;
    }

    public void record(AuditRecordCommand command) {
        auditEventRepository.save(AuditEvent.create(command, timeProvider.now()));
    }
}
