package vn.mar.audit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mar.audit.api.AuditEventQueryService;
import vn.mar.audit.api.AuditEventSearchCommand;
import vn.mar.audit.api.AuditEventSnapshot;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.mapper.AuditEventMapper;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.pagination.PageRequestFactory;
import vn.mar.common.search.SearchText;
import vn.mar.common.tenant.TenantContext;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class AuditQueryService implements AuditEventQueryService {


    private final AuditEventRepository auditEventRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditEventMapper auditEventMapper;
    private final PermissionGuard permissionGuard;

    public AuditQueryService(
            AuditEventRepository auditEventRepository,
            CurrentUserContext currentUserContext,
            AuditEventMapper auditEventMapper,
            PermissionGuard permissionGuard) {
        this.auditEventRepository = auditEventRepository;
        this.currentUserContext = currentUserContext;
        this.auditEventMapper = auditEventMapper;
        this.permissionGuard = permissionGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEventSnapshot> searchEvents(AuditEventSearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Audit event search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewAudit(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        validateRange(command.from(), command.to());
        PageRequest pageable = PageRequestFactory.of(command.page(), command.size(), createdAtDescSort());
        Page<AuditEvent> page = auditEventRepository.search(
                tenantId,
                SearchText.upperOrNull(command.resourceType()),
                command.resourceId(),
                SearchText.textOrNull(command.resourceKey()),
                command.actorId(),
                SearchText.upperOrNull(command.actorType()),
                SearchText.upperOrNull(command.action()),
                SearchText.textOrNull(command.requestId()),
                command.from(),
                command.to(),
                pageable
        );
        return PageResponse.from(page.map(auditEventMapper::toSnapshot));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEventSnapshot getEvent(UUID auditEventId) {
        if (auditEventId == null) {
            throw ValidationException.of("audit_event_id", "REQUIRED", "Audit event id is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewAudit(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        return auditEventRepository.findByIdAndTenantId(auditEventId, tenantId)
                .map(auditEventMapper::toSnapshot)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Audit event was not found",
                        List.of(ErrorDetail.of("audit_event_id", "NOT_FOUND", "Audit event was not found"))
                ));
    }

    private void assertCanViewAudit(CurrentUser actor) {
        permissionGuard.requirePermission(actor, PermissionCodes.AUDIT_VIEW, "AUDIT_VIEW_DENIED", "Permission is required to view audit events");
    }


    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ValidationException.of("from", "INVALID_RANGE", "From must be before or equal to to");
        }
    }


    private Sort createdAtDescSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }

}
