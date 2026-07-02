package vn.mar.leadimport.dto.response;

import java.util.Map;

public record LeadImportValidSampleResponse(
        int rowNumber,
        Map<String, Object> normalizedRow
) {
}
