package vn.mar.leadimport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.mar.common.error.ErrorDetail;
import vn.mar.leadimport.model.ImportRowStatus;

@Entity
@Table(name = "import_rows")
public class ImportRow {

    @Id
    @Column(name = "import_row_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "row_status", nullable = false)
    private ImportRowStatus rowStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_row", columnDefinition = "jsonb")
    private Map<String, Object> rawRow;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_row", columnDefinition = "jsonb")
    private Map<String, Object> normalizedRow;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private List<ErrorDetail> errorDetails;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportRow() {
    }

    private ImportRow(
            UUID id,
            UUID tenantId,
            UUID importBatchId,
            int rowNumber,
            ImportRowStatus rowStatus,
            Map<String, Object> rawRow,
            Map<String, Object> normalizedRow,
            List<ErrorDetail> errorDetails,
            Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.importBatchId = importBatchId;
        this.rowNumber = rowNumber;
        this.rowStatus = rowStatus;
        this.rawRow = rawRow == null ? null : Map.copyOf(rawRow);
        this.normalizedRow = normalizedRow == null ? null : Map.copyOf(normalizedRow);
        this.errorDetails = errorDetails == null ? List.of() : List.copyOf(errorDetails);
        this.createdAt = createdAt;
    }

    public static ImportRow create(
            UUID id,
            UUID tenantId,
            UUID importBatchId,
            int rowNumber,
            ImportRowStatus rowStatus,
            Map<String, Object> rawRow,
            Map<String, Object> normalizedRow,
            List<ErrorDetail> errorDetails,
            Instant createdAt) {
        return new ImportRow(id, tenantId, importBatchId, rowNumber, rowStatus, rawRow, normalizedRow, errorDetails, createdAt);
    }

    public static ImportRow restore(
            UUID id,
            UUID tenantId,
            UUID importBatchId,
            int rowNumber,
            ImportRowStatus rowStatus,
            Map<String, Object> rawRow,
            Map<String, Object> normalizedRow,
            List<ErrorDetail> errorDetails,
            Instant createdAt) {
        return new ImportRow(id, tenantId, importBatchId, rowNumber, rowStatus, rawRow, normalizedRow, errorDetails, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID importBatchId() {
        return importBatchId;
    }

    public int rowNumber() {
        return rowNumber;
    }

    public ImportRowStatus rowStatus() {
        return rowStatus;
    }

    public Map<String, Object> rawRow() {
        return rawRow;
    }

    public Map<String, Object> normalizedRow() {
        return normalizedRow;
    }

    public List<ErrorDetail> errorDetails() {
        return errorDetails;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
