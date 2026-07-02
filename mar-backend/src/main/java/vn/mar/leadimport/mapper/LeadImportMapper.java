package vn.mar.leadimport.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.common.error.ErrorDetail;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.dto.response.ImportBatchSummaryResponse;
import vn.mar.leadimport.dto.response.ImportRowErrorResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;

@Component
public class LeadImportMapper {

    public ImportBatchSummaryResponse toSummaryResponse(ImportBatch batch) {
        return new ImportBatchSummaryResponse(
                batch.id(),
                batch.tenantId(),
                batch.importType().name(),
                batch.sourceType().name(),
                batch.status().name(),
                batch.originalFileName(),
                batch.totalRows(),
                batch.validRows(),
                batch.errorRows(),
                batch.duplicateRows(),
                batch.importedAt(),
                batch.createdAt(),
                batch.updatedAt()
        );
    }

    public ImportBatchDetailResponse toDetailResponse(ImportBatch batch) {
        return new ImportBatchDetailResponse(
                batch.id(),
                batch.tenantId(),
                batch.importType().name(),
                batch.sourceType().name(),
                batch.status().name(),
                batch.originalFileName(),
                batch.mappingConfig(),
                batch.totalRows(),
                batch.validRows(),
                batch.errorRows(),
                batch.duplicateRows(),
                batch.importedAt(),
                batch.createdAt(),
                batch.updatedAt()
        );
    }

    public ImportRowErrorResponse toErrorResponse(ImportRow row) {
        List<ErrorDetail> errors = row.errorDetails() == null ? List.of() : row.errorDetails();
        ErrorDetail primaryError = errors.isEmpty() ? null : errors.getFirst();
        return new ImportRowErrorResponse(
                row.id(),
                row.importBatchId(),
                row.rowNumber(),
                primaryError == null ? null : primaryError.field(),
                primaryError == null ? null : primaryError.code(),
                primaryError == null ? null : primaryError.message(),
                errors,
                row.createdAt()
        );
    }

    public Map<String, Object> toAuditData(ImportBatch batch) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batch_id", batch.id().toString());
        data.put("tenant_id", batch.tenantId().toString());
        data.put("import_type", batch.importType().name());
        data.put("source_type", batch.sourceType().name());
        data.put("status", batch.status().name());
        data.put("original_file_name", batch.originalFileName());
        data.put("total_rows", batch.totalRows());
        data.put("valid_count", batch.validRows());
        data.put("error_count", batch.errorRows());
        data.put("duplicate_count", batch.duplicateRows());
        return data;
    }
}
