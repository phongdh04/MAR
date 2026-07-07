package vn.mar.integration.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import vn.mar.common.validation.EnumParser;
import vn.mar.integration.api.IntegrationEventQueryService;
import vn.mar.integration.api.IntegrationEventSearchCommand;
import vn.mar.integration.api.IntegrationEventSnapshot;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.mapper.IntegrationEventMapper;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class IntegrationEventQueryApplicationService implements IntegrationEventQueryService {


    private final IntegrationEventRepository integrationEventRepository;
    private final CurrentUserContext currentUserContext;
    private final IntegrationEventMapper integrationEventMapper;
    private final PermissionGuard permissionGuard;

    public IntegrationEventQueryApplicationService(
            IntegrationEventRepository integrationEventRepository,
            CurrentUserContext currentUserContext,
            IntegrationEventMapper integrationEventMapper,
            PermissionGuard permissionGuard) {
        this.integrationEventRepository = integrationEventRepository;
        this.currentUserContext = currentUserContext;
        this.integrationEventMapper = integrationEventMapper;
        this.permissionGuard = permissionGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IntegrationEventSnapshot> searchEvents(IntegrationEventSearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Integration event search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewIntegrationLogs(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        validateRange(command.from(), command.to());

        PageRequest pageable = PageRequestFactory.of(command.page(), command.size(), receivedAtDescSort());
        Page<IntegrationEvent> page = integrationEventRepository.search(
                tenantId,
                resolveSourceType(command.sourceType()),
                resolveStatus(command.status()),
                SearchText.textOrNull(command.externalId()),
                SearchText.textOrNull(command.idempotencyKey()),
                SearchText.textOrNull(command.payloadHash()),
                SearchText.upperOrNull(command.errorCode()),
                command.createdLeadId(),
                command.createdCustomerId(),
                command.createdOpportunityId(),
                command.from(),
                command.to(),
                pageable
        );
        return PageResponse.from(page.map(integrationEventMapper::toIntegrationSnapshot));
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationEventSnapshot getEvent(UUID eventId) {
        if (eventId == null) {
            throw ValidationException.of("event_id", "REQUIRED", "Integration event id is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewIntegrationLogs(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        return integrationEventRepository.findByIdAndTenantId(eventId, tenantId)
                .map(integrationEventMapper::toIntegrationSnapshot)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Integration event was not found",
                        List.of(ErrorDetail.of("event_id", "NOT_FOUND", "Integration event was not found"))
                ));
    }

    private void assertCanViewIntegrationLogs(CurrentUser actor) {
        permissionGuard.requirePermission(actor, PermissionCodes.INTEGRATION_LOG_VIEW, "INTEGRATION_LOG_VIEW_DENIED", "Permission is required to view integration logs");
    }


    private LeadSourceType resolveSourceType(String value) {
        return EnumParser.optionalEnum(LeadSourceType.class, value, "source_type", "INVALID_SOURCE_TYPE", "Source type is invalid");
    }

    private IntegrationEventStatus resolveStatus(String value) {
        return EnumParser.optionalEnum(IntegrationEventStatus.class, value, "status", "INVALID_STATUS", "Integration event status is invalid");
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ValidationException.of("from", "INVALID_RANGE", "From must be before or equal to to");
        }
    }


    private Sort receivedAtDescSort() {
        return Sort.by(Sort.Direction.DESC, "receivedAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }

}
