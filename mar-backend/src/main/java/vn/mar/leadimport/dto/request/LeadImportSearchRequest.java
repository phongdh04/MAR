package vn.mar.leadimport.dto.request;

public record LeadImportSearchRequest(
        String status,
        String sourceType,
        Integer page,
        Integer size
) {
}
