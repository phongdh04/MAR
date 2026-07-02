package vn.mar.permission.service;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionScope;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.permission.dto.request.PermissionMatrixChangeRequest;
import vn.mar.permission.dto.request.UpdatePermissionMatrixRequest;
import vn.mar.permission.dto.response.PermissionMatrixResponse;
import vn.mar.permission.event.PermissionMatrixUpdatedEvent;
import vn.mar.permission.mapper.PermissionMatrixMapper;
import vn.mar.role.model.RoleCode;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class PermissionMatrixService {

    private static final Set<String> ADVISOR_BLOCKED_FUNCTION_CODES = Set.of(
            PermissionCodes.IMPORT_MANAGE,
            PermissionCodes.LEAD_IMPORT,
            PermissionCodes.CUSTOMER_MERGE,
            PermissionCodes.DATA_EXPORT
    );

    private final PermissionProfileRepository permissionProfileRepository;
    private final RoleRepository roleRepository;
    private final PermissionMatrixMapper permissionMatrixMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public PermissionMatrixService(
            PermissionProfileRepository permissionProfileRepository,
            RoleRepository roleRepository,
            PermissionMatrixMapper permissionMatrixMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher) {
        this.permissionProfileRepository = permissionProfileRepository;
        this.roleRepository = roleRepository;
        this.permissionMatrixMapper = permissionMatrixMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PermissionMatrixResponse getMatrix() {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        return toMatrixResponse(tenantId, permissionProfileRepository.findByTenantId(tenantId));
    }

    @Transactional
    public PermissionMatrixResponse updateMatrix(UpdatePermissionMatrixRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        String reason = requireReason(request.reason());
        List<PermissionMatrixChangeRequest> changes = requireChanges(request.changes());
        Instant now = timeProvider.now();

        List<PermissionProfile> beforeProfiles = permissionProfileRepository.findByTenantId(tenantId);
        Map<String, Object> beforeData = permissionMatrixMapper.toAuditData(beforeProfiles);
        Set<String> changedRoleCodes = new LinkedHashSet<>();

        for (PermissionMatrixChangeRequest change : changes) {
            PermissionProfile updatedProfile = applyChange(tenantId, actor.actorId(), now, change);
            if (updatedProfile != null) {
                changedRoleCodes.add(updatedProfile.roleCode());
            }
        }

        List<PermissionProfile> afterProfiles = permissionProfileRepository.findByTenantId(tenantId);
        if (!changedRoleCodes.isEmpty()) {
            auditPermissionChange(actor, tenantId, beforeData, permissionMatrixMapper.toAuditData(afterProfiles), reason);
            changedRoleCodes.forEach(roleCode -> eventPublisher.publishEvent(new PermissionMatrixUpdatedEvent(
                    tenantId,
                    roleCode,
                    actor.actorId(),
                    LogContext.requestId()
            )));
        }
        return toMatrixResponse(tenantId, afterProfiles);
    }

    private PermissionProfile applyChange(UUID tenantId, UUID actorId, Instant now, PermissionMatrixChangeRequest change) {
        String roleCode = resolveActiveRole(change.role());
        String functionCode = resolveActiveFunctionCode(change.functionCode());
        PermissionAccessLevel accessLevel = resolveAccessLevel(change.accessLevel());
        PermissionScope scope = resolveScope(change.scope(), accessLevel);
        assertGuardrails(roleCode, functionCode, accessLevel, scope);

        return permissionProfileRepository.findByTenantIdAndRoleCodeAndFunctionCode(tenantId, roleCode, functionCode)
                .map(existing -> updateExisting(existing, accessLevel, scope, actorId, now))
                .orElseGet(() -> insertNew(tenantId, roleCode, functionCode, accessLevel, scope, actorId, now));
    }

    private PermissionProfile updateExisting(
            PermissionProfile existing,
            PermissionAccessLevel accessLevel,
            PermissionScope scope,
            UUID actorId,
            Instant now) {
        if (existing.accessLevel() == accessLevel && existing.scope() == scope) {
            return null;
        }
        PermissionProfile updated = existing.update(accessLevel, scope, actorId, now);
        permissionProfileRepository.update(updated);
        return updated;
    }

    private PermissionProfile insertNew(
            UUID tenantId,
            String roleCode,
            String functionCode,
            PermissionAccessLevel accessLevel,
            PermissionScope scope,
            UUID actorId,
            Instant now) {
        PermissionProfile profile = PermissionProfile.create(
                UUID.randomUUID(),
                tenantId,
                roleCode,
                functionCode,
                accessLevel,
                scope,
                actorId,
                now
        );
        permissionProfileRepository.insert(profile);
        return profile;
    }

    private PermissionMatrixResponse toMatrixResponse(UUID tenantId, List<PermissionProfile> profiles) {
        return permissionMatrixMapper.toMatrixResponse(
                tenantId,
                permissionProfileRepository.findActiveFunctionCodes(),
                profiles
        );
    }

    private String resolveActiveRole(String requestedRole) {
        if (!StringUtils.hasText(requestedRole)) {
            throw validation("role", "REQUIRED", "Role is required");
        }
        String roleCode = requestedRole.trim().toUpperCase(Locale.ROOT);
        try {
            RoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            throw validation("role", "INVALID_ROLE", "Role is invalid");
        }
        if (!roleRepository.existsByRoleCodeAndStatus(roleCode, RoleStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.INVALID_PARENT_STATUS, "Role is inactive or not found");
        }
        return roleCode;
    }

    private String resolveActiveFunctionCode(String requestedFunctionCode) {
        if (!StringUtils.hasText(requestedFunctionCode)) {
            throw validation("function_code", "REQUIRED", "Function code is required");
        }
        String functionCode = requestedFunctionCode.trim().toLowerCase(Locale.ROOT);
        if (!functionCode.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$")) {
            throw validation("function_code", "INVALID_FORMAT", "Function code is invalid");
        }
        if (!permissionProfileRepository.existsActiveFunctionCode(functionCode)) {
            throw validation("function_code", "UNKNOWN_PERMISSION", "Permission code is not active");
        }
        return functionCode;
    }

    private PermissionAccessLevel resolveAccessLevel(String requestedAccessLevel) {
        if (!StringUtils.hasText(requestedAccessLevel)) {
            throw validation("access_level", "REQUIRED", "Access level is required");
        }
        try {
            return PermissionAccessLevel.valueOf(requestedAccessLevel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("access_level", "INVALID_ACCESS_LEVEL", "Access level is invalid");
        }
    }

    private PermissionScope resolveScope(String requestedScope, PermissionAccessLevel accessLevel) {
        if (accessLevel == PermissionAccessLevel.NONE) {
            return PermissionScope.NONE;
        }
        if (requestedScope == null) {
            return PermissionScope.TENANT;
        }
        if (!StringUtils.hasText(requestedScope)) {
            throw validation("scope", "INVALID_SCOPE", "Scope is invalid");
        }
        try {
            return PermissionScope.valueOf(requestedScope.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("scope", "INVALID_SCOPE", "Scope is invalid");
        }
    }

    private void assertGuardrails(
            String roleCode,
            String functionCode,
            PermissionAccessLevel accessLevel,
            PermissionScope scope) {
        if (!accessLevel.grantsAccess()) {
            return;
        }
        if (scope == PermissionScope.TEAM) {
            throw guardrail("TEAM scope is reserved for a later sprint");
        }
        if (RoleCode.ADVISOR.name().equals(roleCode) && ADVISOR_BLOCKED_FUNCTION_CODES.contains(functionCode)) {
            throw guardrail("Advisor cannot receive import, export or merge permission");
        }
        if (RoleCode.MARKETING.name().equals(roleCode) && PermissionCodes.PAYMENT_WRITE.equals(functionCode)) {
            throw guardrail("Marketing cannot receive payment write permission");
        }
    }

    private List<PermissionMatrixChangeRequest> requireChanges(List<PermissionMatrixChangeRequest> changes) {
        if (changes == null || changes.isEmpty()) {
            throw validation("changes", "REQUIRED", "At least one permission change is required");
        }
        return changes;
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw validation("reason", "REQUIRED", "Permission change reason is required");
        }
        return reason.trim();
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void auditPermissionChange(
            CurrentUser actor,
            UUID tenantId,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                tenantId,
                actor.actorId(),
                "USER",
                actor.roleCode(),
                AuditActions.PERMISSION_MATRIX_UPDATED,
                AuditResourceTypes.PERMISSION_MATRIX,
                tenantId,
                "permission-matrix",
                beforeData,
                afterData,
                auditMetadata(actor),
                reason,
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(CurrentUser actor) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (actor.tenantId() != null) {
            metadata.put("actor_tenant_id", actor.tenantId().toString());
        }
        return metadata;
    }

    private BusinessException guardrail(String message) {
        return new BusinessException(ErrorCode.INVALID_PERMISSION_GUARDRAIL, message);
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
