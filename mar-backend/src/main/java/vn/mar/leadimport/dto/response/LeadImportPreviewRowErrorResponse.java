package vn.mar.leadimport.dto.response;

public record LeadImportPreviewRowErrorResponse(
        int rowNumber,
        String field,
        String code,
        String message,
        String rawValue
) {
}
