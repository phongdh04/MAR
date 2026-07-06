package vn.mar.integration.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID> {

    Optional<IntegrationEvent> findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(
            UUID tenantId,
            LeadSourceType sourceType,
            String externalId,
            IntegrationEventStatus status);

    Optional<IntegrationEvent> findFirstByTenantIdAndSourceTypeAndIdempotencyKeyAndStatusNotOrderByReceivedAtAsc(
            UUID tenantId,
            LeadSourceType sourceType,
            String idempotencyKey,
            IntegrationEventStatus status);

    Optional<IntegrationEvent> findFirstByTenantIdAndSourceTypeAndPayloadHashAndStatusNotOrderByReceivedAtAsc(
            UUID tenantId,
            LeadSourceType sourceType,
            String payloadHash,
            IntegrationEventStatus status);
}
