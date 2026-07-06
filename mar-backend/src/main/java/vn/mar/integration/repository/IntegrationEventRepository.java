package vn.mar.integration.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID>, JpaSpecificationExecutor<IntegrationEvent> {

    Optional<IntegrationEvent> findByIdAndTenantId(UUID id, UUID tenantId);

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

    default Page<IntegrationEvent> search(
            UUID tenantId,
            LeadSourceType sourceType,
            IntegrationEventStatus status,
            String externalId,
            String idempotencyKey,
            String payloadHash,
            String errorCode,
            UUID createdLeadId,
            UUID createdCustomerId,
            UUID createdOpportunityId,
            java.time.Instant fromReceivedAt,
            java.time.Instant toReceivedAt,
            Pageable pageable) {
        return findAll(IntegrationEventSpecifications.search(
                tenantId,
                sourceType,
                status,
                externalId,
                idempotencyKey,
                payloadHash,
                errorCode,
                createdLeadId,
                createdCustomerId,
                createdOpportunityId,
                fromReceivedAt,
                toReceivedAt
        ), pageable);
    }
}
