package vn.mar.audit.controller;

import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.audit.api.AuditEventQueryService;
import vn.mar.audit.api.AuditEventSearchCommand;
import vn.mar.audit.api.AuditEventSnapshot;
import vn.mar.audit.dto.response.AuditEventResponse;
import vn.mar.audit.mapper.AuditEventMapper;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventController {

    private final AuditEventQueryService auditEventQueryService;
    private final AuditEventMapper auditEventMapper;

    public AuditEventController(
            AuditEventQueryService auditEventQueryService,
            AuditEventMapper auditEventMapper) {
        this.auditEventQueryService = auditEventQueryService;
        this.auditEventMapper = auditEventMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'audit.view')")
    public ResponseEntity<ApiResponse<PageResponse<AuditEventResponse>>> searchEvents(
            @RequestParam(name = "resource_type", required = false) String resourceType,
            @RequestParam(name = "resource_id", required = false) UUID resourceId,
            @RequestParam(name = "resource_key", required = false) String resourceKey,
            @RequestParam(name = "actor_id", required = false) UUID actorId,
            @RequestParam(name = "actor_type", required = false) String actorType,
            @RequestParam(required = false) String action,
            @RequestParam(name = "request_id", required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<AuditEventSnapshot> snapshots = auditEventQueryService.searchEvents(new AuditEventSearchCommand(
                resourceType,
                resourceId,
                resourceKey,
                actorId,
                actorType,
                action,
                requestId,
                from,
                to,
                page,
                size
        ));
        return ResponseEntity.ok(ApiResponse.success(snapshots.map(auditEventMapper::toResponse)));
    }

    @GetMapping("/{auditEventId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'audit.view')")
    public ResponseEntity<ApiResponse<AuditEventResponse>> getEvent(@PathVariable UUID auditEventId) {
        return ResponseEntity.ok(ApiResponse.success(auditEventMapper.toResponse(
                auditEventQueryService.getEvent(auditEventId)
        )));
    }

}
