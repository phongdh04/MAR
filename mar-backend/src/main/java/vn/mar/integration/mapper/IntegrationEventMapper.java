package vn.mar.integration.mapper;

import org.springframework.stereotype.Component;
import vn.mar.integration.api.IntegrationEventSnapshot;
import vn.mar.integration.api.WebhookIntakeSnapshot;
import vn.mar.integration.dto.response.IntegrationEventResponse;
import vn.mar.integration.dto.response.WebhookIntakeResponse;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.model.IntegrationEventStatus;

@Component
public class IntegrationEventMapper {

    public IntegrationEventSnapshot toIntegrationSnapshot(IntegrationEvent event) {
        return new IntegrationEventSnapshot(
                event.id(),
                event.tenantId(),
                event.sourceType(),
                event.externalId(),
                event.idempotencyKey(),
                event.payloadHash(),
                event.status(),
                event.errorCode(),
                event.errorMessage(),
                event.rawPayloadUri(),
                event.createdLeadId(),
                event.createdCustomerId(),
                event.createdOpportunityId(),
                event.receivedAt(),
                event.processedAt()
        );
    }

    public IntegrationEventResponse toResponse(IntegrationEventSnapshot snapshot) {
        return new IntegrationEventResponse(
                snapshot.eventId(),
                snapshot.tenantId(),
                snapshot.sourceType().name(),
                snapshot.externalId(),
                snapshot.idempotencyKey(),
                snapshot.payloadHash(),
                snapshot.status().name(),
                snapshot.errorCode(),
                snapshot.errorMessage(),
                snapshot.rawPayloadUri(),
                snapshot.createdLeadId(),
                snapshot.createdCustomerId(),
                snapshot.createdOpportunityId(),
                snapshot.receivedAt(),
                snapshot.processedAt()
        );
    }

    public WebhookIntakeSnapshot toSnapshot(IntegrationEvent event) {
        return new WebhookIntakeSnapshot(
                event.id(),
                event.tenantId(),
                event.sourceType(),
                event.externalId(),
                event.idempotencyKey(),
                event.payloadHash(),
                event.status(),
                event.errorCode(),
                event.errorMessage(),
                event.rawPayloadUri(),
                event.createdLeadId(),
                event.createdCustomerId(),
                event.createdOpportunityId(),
                event.receivedAt(),
                event.processedAt(),
                event.status() == IntegrationEventStatus.DUPLICATE
        );
    }

    public WebhookIntakeResponse toResponse(WebhookIntakeSnapshot snapshot) {
        return new WebhookIntakeResponse(
                snapshot.integrationEventId(),
                snapshot.tenantId(),
                snapshot.sourceType().name(),
                snapshot.externalId(),
                snapshot.idempotencyKey(),
                snapshot.payloadHash(),
                snapshot.status().name(),
                snapshot.errorCode(),
                snapshot.errorMessage(),
                snapshot.rawPayloadUri(),
                snapshot.createdLeadId(),
                snapshot.createdCustomerId(),
                snapshot.createdOpportunityId(),
                snapshot.receivedAt(),
                snapshot.processedAt(),
                snapshot.duplicate()
        );
    }
}
