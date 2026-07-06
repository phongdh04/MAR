package vn.mar.integration.repository;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

public final class IntegrationEventSpecifications {

    private IntegrationEventSpecifications() {
    }

    public static Specification<IntegrationEvent> search(
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
            Instant fromReceivedAt,
            Instant toReceivedAt) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            if (sourceType != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), sourceType));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (externalId != null) {
                predicates.add(criteriaBuilder.equal(root.get("externalId"), externalId));
            }
            if (idempotencyKey != null) {
                predicates.add(criteriaBuilder.equal(root.get("idempotencyKey"), idempotencyKey));
            }
            if (payloadHash != null) {
                predicates.add(criteriaBuilder.equal(root.get("payloadHash"), payloadHash));
            }
            if (errorCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("errorCode"), errorCode));
            }
            if (createdLeadId != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdLeadId"), createdLeadId));
            }
            if (createdCustomerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdCustomerId"), createdCustomerId));
            }
            if (createdOpportunityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdOpportunityId"), createdOpportunityId));
            }
            if (fromReceivedAt != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("receivedAt"), fromReceivedAt));
            }
            if (toReceivedAt != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("receivedAt"), toReceivedAt));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
