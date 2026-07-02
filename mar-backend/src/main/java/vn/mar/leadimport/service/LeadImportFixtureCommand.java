package vn.mar.leadimport.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportSourceType;

public record LeadImportFixtureCommand(
        UUID tenantId,
        UUID actorId,
        ImportSourceType sourceType,
        ImportBatchStatus status,
        String originalFileName,
        Map<String, Object> mappingConfig,
        int totalRows,
        int validRows,
        int errorRows,
        int duplicateRows,
        List<LeadImportFixtureRowCommand> rows
) {
}
