package vn.mar.customer.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.customer.api.DuplicateCaseSnapshot;
import vn.mar.customer.dto.response.DuplicateCaseResponse;
import vn.mar.customer.entity.DuplicateCase;

@Component
public class DuplicateCaseMapper {

    public DuplicateCaseSnapshot toSnapshot(DuplicateCase duplicateCase) {
        return new DuplicateCaseSnapshot(
                duplicateCase.id(),
                duplicateCase.tenantId(),
                duplicateCase.sourceCustomerId(),
                duplicateCase.matchedCustomerId(),
                duplicateCase.matchType(),
                duplicateCase.confidence(),
                duplicateCase.status(),
                duplicateCase.reviewReason(),
                duplicateCase.resolutionAction(),
                duplicateCase.resolvedBy(),
                duplicateCase.resolvedAt(),
                duplicateCase.resolutionReason(),
                duplicateCase.createdAt(),
                duplicateCase.updatedAt()
        );
    }

    public DuplicateCaseResponse toResponse(DuplicateCaseSnapshot snapshot) {
        return new DuplicateCaseResponse(
                snapshot.duplicateCaseId(),
                snapshot.tenantId(),
                snapshot.sourceCustomerId(),
                snapshot.matchedCustomerId(),
                snapshot.matchType().name(),
                snapshot.confidence().name(),
                snapshot.status().name(),
                snapshot.reviewReason(),
                snapshot.resolutionAction() == null ? null : snapshot.resolutionAction().name(),
                snapshot.resolvedBy(),
                snapshot.resolvedAt(),
                snapshot.resolutionReason(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(DuplicateCase duplicateCase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("duplicate_case_id", duplicateCase.id().toString());
        data.put("tenant_id", duplicateCase.tenantId().toString());
        data.put("source_customer_id", duplicateCase.sourceCustomerId().toString());
        data.put("matched_customer_id", duplicateCase.matchedCustomerId().toString());
        data.put("match_type", duplicateCase.matchType().name());
        data.put("confidence", duplicateCase.confidence().name());
        data.put("status", duplicateCase.status().name());
        data.put("review_reason", duplicateCase.reviewReason());
        if (duplicateCase.resolutionAction() != null) {
            data.put("resolution_action", duplicateCase.resolutionAction().name());
        }
        if (duplicateCase.resolvedBy() != null) {
            data.put("resolved_by", duplicateCase.resolvedBy().toString());
        }
        if (duplicateCase.resolvedAt() != null) {
            data.put("resolved_at", duplicateCase.resolvedAt().toString());
        }
        if (duplicateCase.resolutionReason() != null) {
            data.put("resolution_reason", duplicateCase.resolutionReason());
        }
        return data;
    }
}
