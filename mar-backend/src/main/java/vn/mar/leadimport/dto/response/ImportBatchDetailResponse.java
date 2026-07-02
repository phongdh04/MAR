package vn.mar.leadimport.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ImportBatchDetailResponse(
        UUID batchId,
        UUID tenantId,
        String importType,
        String sourceType,
        String status,
        String originalFileName,
        Map<String, Object> mappingConfig,
        int totalRows,
        int validCount,
        int errorCount,
        int duplicateCount,
        Instant importedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
