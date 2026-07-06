package vn.mar.integration.controller;

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
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.integration.api.IntegrationEventQueryService;
import vn.mar.integration.api.IntegrationEventSearchCommand;
import vn.mar.integration.api.IntegrationEventSnapshot;
import vn.mar.integration.dto.response.IntegrationEventResponse;
import vn.mar.integration.mapper.IntegrationEventMapper;

@RestController
@RequestMapping("/api/v1/integrations/webhook-events")
public class IntegrationEventController {

    private final IntegrationEventQueryService integrationEventQueryService;
    private final IntegrationEventMapper integrationEventMapper;

    public IntegrationEventController(
            IntegrationEventQueryService integrationEventQueryService,
            IntegrationEventMapper integrationEventMapper) {
        this.integrationEventQueryService = integrationEventQueryService;
        this.integrationEventMapper = integrationEventMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'integration_log.view')")
    public ResponseEntity<ApiResponse<PageResponse<IntegrationEventResponse>>> searchEvents(
            @RequestParam(name = "source_type", required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(name = "external_id", required = false) String externalId,
            @RequestParam(name = "idempotency_key", required = false) String idempotencyKey,
            @RequestParam(name = "payload_hash", required = false) String payloadHash,
            @RequestParam(name = "error_code", required = false) String errorCode,
            @RequestParam(name = "created_lead_id", required = false) UUID createdLeadId,
            @RequestParam(name = "created_customer_id", required = false) UUID createdCustomerId,
            @RequestParam(name = "created_opportunity_id", required = false) UUID createdOpportunityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<IntegrationEventSnapshot> snapshots = integrationEventQueryService.searchEvents(new IntegrationEventSearchCommand(
                sourceType,
                status,
                externalId,
                idempotencyKey,
                payloadHash,
                errorCode,
                createdLeadId,
                createdCustomerId,
                createdOpportunityId,
                from,
                to,
                page,
                size
        ));
        return ResponseEntity.ok(ApiResponse.success(toResponsePage(snapshots)));
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'integration_log.view')")
    public ResponseEntity<ApiResponse<IntegrationEventResponse>> getEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(integrationEventMapper.toResponse(
                integrationEventQueryService.getEvent(eventId)
        )));
    }

    private PageResponse<IntegrationEventResponse> toResponsePage(PageResponse<IntegrationEventSnapshot> snapshots) {
        return new PageResponse<>(
                snapshots.items().stream()
                        .map(integrationEventMapper::toResponse)
                        .toList(),
                snapshots.page(),
                snapshots.size(),
                snapshots.totalElements(),
                snapshots.totalPages()
        );
    }
}
