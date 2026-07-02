package vn.mar.leadimport.dto.response;

public record LeadImportDuplicateCandidateResponse(
        int rowNumber,
        String field,
        String matchType,
        int matchedRowNumber,
        String rawValue,
        String recommendedAction
) {
}
