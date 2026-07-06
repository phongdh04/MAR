package vn.mar.audit.repository;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import vn.mar.audit.entity.AuditEvent;

public final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEvent> search(
            UUID tenantId,
            String resourceType,
            UUID resourceId,
            String resourceKey,
            UUID actorId,
            String actorType,
            String action,
            String requestId,
            Instant fromCreatedAt,
            Instant toCreatedAt) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            if (resourceType != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }
            if (resourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceId"), resourceId));
            }
            if (resourceKey != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceKey"), resourceKey));
            }
            if (actorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), actorId));
            }
            if (actorType != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorType"), actorType));
            }
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (requestId != null) {
                predicates.add(criteriaBuilder.equal(root.get("requestId"), requestId));
            }
            if (fromCreatedAt != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromCreatedAt));
            }
            if (toCreatedAt != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toCreatedAt));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
