package vn.mar.customer.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.customer.api.MergeHistorySnapshot;
import vn.mar.customer.dto.response.MergeHistoryResponse;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.entity.MergeHistory;

@Component
public class MergeHistoryMapper {

    public MergeHistorySnapshot toSnapshot(MergeHistory mergeHistory) {
        return new MergeHistorySnapshot(
                mergeHistory.id(),
                mergeHistory.tenantId(),
                mergeHistory.sourceCustomerId(),
                mergeHistory.targetCustomerId(),
                mergeHistory.duplicateCaseId(),
                mergeHistory.mergedBy(),
                mergeHistory.mergedAt(),
                mergeHistory.reason(),
                mergeHistory.mergeSnapshot(),
                mergeHistory.canUnmerge(),
                mergeHistory.unmergedBy(),
                mergeHistory.unmergedAt()
        );
    }

    public MergeHistoryResponse toResponse(MergeHistorySnapshot snapshot) {
        return new MergeHistoryResponse(
                snapshot.mergeId(),
                snapshot.tenantId(),
                snapshot.sourceCustomerId(),
                snapshot.targetCustomerId(),
                snapshot.duplicateCaseId(),
                snapshot.mergedBy(),
                snapshot.mergedAt(),
                snapshot.reason(),
                snapshot.canUnmerge(),
                snapshot.unmergedBy(),
                snapshot.unmergedAt()
        );
    }

    public Map<String, Object> toAuditData(MergeHistory mergeHistory) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("merge_id", mergeHistory.id().toString());
        data.put("tenant_id", mergeHistory.tenantId().toString());
        data.put("source_customer_id", mergeHistory.sourceCustomerId().toString());
        data.put("target_customer_id", mergeHistory.targetCustomerId().toString());
        if (mergeHistory.duplicateCaseId() != null) {
            data.put("duplicate_case_id", mergeHistory.duplicateCaseId().toString());
        }
        data.put("merged_by", mergeHistory.mergedBy().toString());
        data.put("merged_at", mergeHistory.mergedAt().toString());
        data.put("reason", mergeHistory.reason());
        data.put("can_unmerge", mergeHistory.canUnmerge());
        if (mergeHistory.unmergedBy() != null) {
            data.put("unmerged_by", mergeHistory.unmergedBy().toString());
        }
        if (mergeHistory.unmergedAt() != null) {
            data.put("unmerged_at", mergeHistory.unmergedAt().toString());
        }
        return data;
    }

    public Map<String, Object> toMergeSnapshot(
            DuplicateCase duplicateCase,
            CustomerProfile sourceCustomer,
            CustomerProfile targetCustomer) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("duplicate_case", duplicateCaseSnapshot(duplicateCase));
        snapshot.put("source_customer", customerSnapshot(sourceCustomer));
        snapshot.put("target_customer", customerSnapshot(targetCustomer));
        return snapshot;
    }

    private Map<String, Object> duplicateCaseSnapshot(DuplicateCase duplicateCase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("duplicate_case_id", duplicateCase.id().toString());
        data.put("source_customer_id", duplicateCase.sourceCustomerId().toString());
        data.put("matched_customer_id", duplicateCase.matchedCustomerId().toString());
        data.put("match_type", duplicateCase.matchType().name());
        data.put("confidence", duplicateCase.confidence().name());
        data.put("status", duplicateCase.status().name());
        return data;
    }

    private Map<String, Object> customerSnapshot(CustomerProfile customer) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer_id", customer.id().toString());
        data.put("full_name", customer.fullName());
        data.put("primary_phone", customer.primaryPhone());
        data.put("primary_email", customer.primaryEmail());
        data.put("zalo_id", customer.zaloId());
        data.put("guardian_name", customer.guardianName());
        data.put("guardian_phone", customer.guardianPhone());
        if (customer.preferredChannel() != null) {
            data.put("preferred_channel", customer.preferredChannel().name());
        }
        return data;
    }
}
