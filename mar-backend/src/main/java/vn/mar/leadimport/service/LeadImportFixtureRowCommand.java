package vn.mar.leadimport.service;

import java.util.List;
import java.util.Map;
import vn.mar.common.error.ErrorDetail;
import vn.mar.leadimport.model.ImportRowStatus;

public record LeadImportFixtureRowCommand(
        int rowNumber,
        ImportRowStatus rowStatus,
        Map<String, Object> rawRow,
        Map<String, Object> normalizedRow,
        List<ErrorDetail> errorDetails
) {
}
