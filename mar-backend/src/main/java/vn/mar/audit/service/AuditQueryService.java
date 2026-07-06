package vn.mar.audit.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.api.AuditEventQueryService;
import vn.mar.audit.api.AuditEventSearchCommand;
import vn.mar.audit.api.AuditEventSnapshot;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.mapper.AuditEventMapper;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class AuditQueryService implements AuditEventQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AuditEventRepository auditEventRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditEventMapper auditEventMapper;

    public AuditQueryService(
            AuditEventRepository auditEventRepository,
            CurrentUserContext currentUserContext,
            AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.currentUserContext = currentUserContext;
        this.auditEventMapper = auditEventMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditEventSnapshot> searchEvents(AuditEventSearchCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Audit event search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewAudit(actor);
        UUID tenantId = requireTenantContext(actor);
        validateRange(command.from(), command.to());
        PageRequest pageable = PageRequest.of(
                resolvePage(command.page()),
                resolveSize(command.size()),
                createdAtDescSort()
        );
        Page<AuditEvent> page = auditEventRepository.search(
                tenantId,
                upperOrNull(command.resourceType()),
                command.resourceId(),
                textOrNull(command.resourceKey()),
                command.actorId(),
                upperOrNull(command.actorType()),
                upperOrNull(command.action()),
                textOrNull(command.requestId()),
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
            throw validation("audit_event_id", "REQUIRED", "Audit event id is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewAudit(actor);
        UUID tenantId = requireTenantContext(actor);
        return auditEventRepository.findByIdAndTenantId(auditEventId, tenantId)
                .map(auditEventMapper::toSnapshot)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Audit event was not found",
                        List.of(ErrorDetail.of("audit_event_id", "NOT_FOUND", "Audit event was not found"))
                ));
    }

    private void assertCanViewAudit(CurrentUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.AUDIT_VIEW)) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    "Permission is required to view audit events",
                    List.of(ErrorDetail.of("permission", "AUDIT_VIEW_DENIED", "Permission is required to view audit events"))
            );
        }
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw validation("from", "INVALID_RANGE", "From must be before or equal to to");
        }
    }

    private int resolvePage(Integer requestedPage) {
        if (requestedPage == null) {
            return DEFAULT_PAGE;
        }
        if (requestedPage < 0) {
            throw validation("page", "MIN_VALUE", "Page must be greater than or equal to 0");
        }
        return requestedPage;
    }

    private int resolveSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_SIZE;
        }
        if (requestedSize < 1 || requestedSize > MAX_SIZE) {
            throw validation("size", "INVALID_SIZE", "Size must be between 1 and 100");
        }
        return requestedSize;
    }

    private Sort createdAtDescSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private String upperOrNull(String value) {
        String text = textOrNull(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private String textOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
