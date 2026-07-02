package vn.mar.leadimport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;

@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @Column(name = "import_batch_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false)
    private ImportType importType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ImportSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImportBatchStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mapping_config", columnDefinition = "jsonb")
    private Map<String, Object> mappingConfig;

    @Column(name = "file_metadata_id")
    private UUID fileMetadataId;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "error_rows", nullable = false)
    private int errorRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "imported_at")
    private Instant importedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ImportBatch() {
    }

    private ImportBatch(
            UUID id,
            UUID tenantId,
            ImportType importType,
            ImportSourceType sourceType,
            ImportBatchStatus status,
            Map<String, Object> mappingConfig,
            UUID fileMetadataId,
            String originalFileName,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            Instant importedAt,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.importType = importType;
        this.sourceType = sourceType;
        this.status = status;
        this.mappingConfig = mappingConfig == null ? null : Map.copyOf(mappingConfig);
        this.fileMetadataId = fileMetadataId;
        this.originalFileName = originalFileName;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.errorRows = errorRows;
        this.duplicateRows = duplicateRows;
        this.importedAt = importedAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static ImportBatch createFixture(
            UUID id,
            UUID tenantId,
            ImportSourceType sourceType,
            ImportBatchStatus status,
            Map<String, Object> mappingConfig,
            String originalFileName,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            UUID actorId,
            Instant now) {
        return new ImportBatch(
                id,
                tenantId,
                ImportType.LEAD,
                sourceType,
                status,
                mappingConfig,
                null,
                originalFileName,
                totalRows,
                validRows,
                errorRows,
                duplicateRows,
                null,
                now,
                actorId,
                now,
                actorId
        );
    }

    public static ImportBatch createPreview(
            UUID id,
            UUID tenantId,
            ImportSourceType sourceType,
            Map<String, Object> mappingConfig,
            String originalFileName,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            UUID actorId,
            Instant now) {
        return new ImportBatch(
                id,
                tenantId,
                ImportType.LEAD,
                sourceType,
                ImportBatchStatus.PREVIEWED,
                mappingConfig,
                null,
                originalFileName,
                totalRows,
                validRows,
                errorRows,
                duplicateRows,
                null,
                now,
                actorId,
                now,
                actorId
        );
    }

    public static ImportBatch restore(
            UUID id,
            UUID tenantId,
            ImportType importType,
            ImportSourceType sourceType,
            ImportBatchStatus status,
            Map<String, Object> mappingConfig,
            UUID fileMetadataId,
            String originalFileName,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            Instant importedAt,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new ImportBatch(
                id,
                tenantId,
                importType,
                sourceType,
                status,
                mappingConfig,
                fileMetadataId,
                originalFileName,
                totalRows,
                validRows,
                errorRows,
                duplicateRows,
                importedAt,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public ImportType importType() {
        return importType;
    }

    public ImportSourceType sourceType() {
        return sourceType;
    }

    public ImportBatchStatus status() {
        return status;
    }

    public Map<String, Object> mappingConfig() {
        return mappingConfig;
    }

    public UUID fileMetadataId() {
        return fileMetadataId;
    }

    public String originalFileName() {
        return originalFileName;
    }

    public int totalRows() {
        return totalRows;
    }

    public int validRows() {
        return validRows;
    }

    public int errorRows() {
        return errorRows;
    }

    public int duplicateRows() {
        return duplicateRows;
    }

    public Instant importedAt() {
        return importedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
