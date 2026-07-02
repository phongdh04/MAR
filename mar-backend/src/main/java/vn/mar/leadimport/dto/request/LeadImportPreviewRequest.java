package vn.mar.leadimport.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record LeadImportPreviewRequest(
        @NotEmpty(message = "Column mappings are required")
        Map<String, String> columnMappings,
        String defaultSource,
        String defaultLanguageCode,
        String defaultProgramCode,
        String defaultBranchCode
) {
}
