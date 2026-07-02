package vn.mar.leadimport.dto.response;

import java.util.List;
import java.util.UUID;

public record LeadImportPreviewResponse(
        UUID batchId,
        String status,
        int totalRows,
        int validCount,
        int errorCount,
        int duplicateCount,
        int rowsToCreate,
        int rowsToUpdateOrLink,
        List<LeadImportPreviewRowErrorResponse> errorRows,
        List<LeadImportDuplicateCandidateResponse> duplicateCandidates,
        List<LeadImportValidSampleResponse> validSamples
) {
}
