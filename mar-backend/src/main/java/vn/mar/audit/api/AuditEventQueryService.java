package vn.mar.audit.api;

import java.util.UUID;
import vn.mar.common.pagination.PageResponse;

public interface AuditEventQueryService {

    PageResponse<AuditEventSnapshot> searchEvents(AuditEventSearchCommand command);

    AuditEventSnapshot getEvent(UUID auditEventId);
}
