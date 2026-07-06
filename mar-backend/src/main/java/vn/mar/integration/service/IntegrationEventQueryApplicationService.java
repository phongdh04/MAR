package vn.mar.integration.service;

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
import vn.mar.authz.model.PermissionCodes;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
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

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final IntegrationEventRepository integrationEventRepository;
    private final CurrentUserContext currentUserContext;
    private final IntegrationEventMapper integrationEventMapper;

    public IntegrationEventQueryApplicationService(
            IntegrationEventRepository integrationEventRepository,
            CurrentUserContext currentUserContext,
            IntegrationEventMapper integrationEventMapper) {
        this.integrationEventRepository = integrationEventRepository;
        this.currentUserContext = currentUserContext;
        this.integrationEventMapper = integrationEventMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IntegrationEventSnapshot> searchEvents(IntegrationEventSearchCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Integration event search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewIntegrationLogs(actor);
        UUID tenantId = requireTenantContext(actor);
        validateRange(command.from(), command.to());

        PageRequest pageable = PageRequest.of(
                resolvePage(command.page()),
                resolveSize(command.size()),
                receivedAtDescSort()
        );
        Page<IntegrationEvent> page = integrationEventRepository.search(
                tenantId,
                resolveSourceType(command.sourceType()),
                resolveStatus(command.status()),
                textOrNull(command.externalId()),
                textOrNull(command.idempotencyKey()),
                textOrNull(command.payloadHash()),
                upperOrNull(command.errorCode()),
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
            throw validation("event_id", "REQUIRED", "Integration event id is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewIntegrationLogs(actor);
        UUID tenantId = requireTenantContext(actor);
        return integrationEventRepository.findByIdAndTenantId(eventId, tenantId)
                .map(integrationEventMapper::toIntegrationSnapshot)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Integration event was not found",
                        List.of(ErrorDetail.of("event_id", "NOT_FOUND", "Integration event was not found"))
                ));
    }

    private void assertCanViewIntegrationLogs(CurrentUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.INTEGRATION_LOG_VIEW)) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    "Permission is required to view integration logs",
                    List.of(ErrorDetail.of("permission", "INTEGRATION_LOG_VIEW_DENIED", "Permission is required to view integration logs"))
            );
        }
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private LeadSourceType resolveSourceType(String value) {
        String sourceType = upperOrNull(value);
        if (sourceType == null) {
            return null;
        }
        try {
            return LeadSourceType.valueOf(sourceType);
        } catch (IllegalArgumentException exception) {
            throw validation("source_type", "INVALID_SOURCE_TYPE", "Source type is invalid");
        }
    }

    private IntegrationEventStatus resolveStatus(String value) {
        String status = upperOrNull(value);
        if (status == null) {
            return null;
        }
        try {
            return IntegrationEventStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw validation("status", "INVALID_STATUS", "Integration event status is invalid");
        }
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

    private Sort receivedAtDescSort() {
        return Sort.by(Sort.Direction.DESC, "receivedAt").and(Sort.by(Sort.Direction.DESC, "id"));
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
