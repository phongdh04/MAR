package vn.mar.leadimport.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.mapper.LeadImportMapper;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;

@Service
public class LeadImportFixtureService {

    public static final String DEFAULT_FIXTURE_FILE_NAME = "qa-lead-import-fixture.csv";

    private final ImportBatchRepository importBatchRepository;
    private final ImportRowRepository importRowRepository;
    private final LeadImportMapper leadImportMapper;
    private final TimeProvider timeProvider;
    private final AuditService auditService;

    public LeadImportFixtureService(
            ImportBatchRepository importBatchRepository,
            ImportRowRepository importRowRepository,
            LeadImportMapper leadImportMapper,
            TimeProvider timeProvider,
            AuditService auditService) {
        this.importBatchRepository = importBatchRepository;
        this.importRowRepository = importRowRepository;
        this.leadImportMapper = leadImportMapper;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    @Transactional
    public ImportBatchDetailResponse seedDefaultLeadFixture(UUID tenantId, UUID actorId) {
        return seedFixture(defaultCommand(tenantId, actorId));
    }

    @Transactional
    public ImportBatchDetailResponse seedFixture(LeadImportFixtureCommand command) {
        validateCommand(command);
        return importBatchRepository.findFirstByTenantIdAndImportTypeAndOriginalFileNameOrderByCreatedAtDesc(
                        command.tenantId(),
                        ImportType.LEAD,
                        command.originalFileName()
                )
                .map(leadImportMapper::toDetailResponse)
                .orElseGet(() -> createFixture(command));
    }

    private ImportBatchDetailResponse createFixture(LeadImportFixtureCommand command) {
        Instant now = timeProvider.now();
        ImportBatch batch = ImportBatch.createFixture(
                UUID.randomUUID(),
                command.tenantId(),
                command.sourceType(),
                command.status(),
                command.mappingConfig(),
                command.originalFileName(),
                command.totalRows(),
                command.validRows(),
                command.errorRows(),
                command.duplicateRows(),
                command.actorId(),
                now
        );
        ImportBatch savedBatch = importBatchRepository.save(batch);
        List<ImportRow> rows = command.rows()
                .stream()
                .map(row -> ImportRow.create(
                        UUID.randomUUID(),
                        savedBatch.tenantId(),
                        savedBatch.id(),
                        row.rowNumber(),
                        row.rowStatus(),
                        row.rawRow(),
                        row.normalizedRow(),
                        row.errorDetails(),
                        now
                ))
                .toList();
        importRowRepository.saveAll(rows);
        auditFixtureCreated(savedBatch, command.actorId());
        return leadImportMapper.toDetailResponse(savedBatch);
    }

    private void validateCommand(LeadImportFixtureCommand command) {
        if (command == null) {
            throw validation("fixture", "REQUIRED", "Import fixture command is required");
        }
        if (command.tenantId() == null) {
            throw validation("tenant_id", "REQUIRED", "Tenant is required");
        }
        if (command.actorId() == null) {
            throw validation("actor_id", "REQUIRED", "Actor is required");
        }
        if (command.sourceType() == null) {
            throw validation("source_type", "REQUIRED", "Source type is required");
        }
        if (command.status() == null) {
            throw validation("status", "REQUIRED", "Import batch status is required");
        }
        if (!StringUtils.hasText(command.originalFileName())) {
            throw validation("original_file_name", "REQUIRED", "Original file name is required");
        }
        if (command.mappingConfig() == null || command.mappingConfig().isEmpty()) {
            throw validation("mapping_config", "REQUIRED", "Mapping config is required");
        }
        validateCounts(command);
        validateRows(command);
    }

    private void validateCounts(LeadImportFixtureCommand command) {
        if (command.totalRows() < 0) {
            throw validation("total_rows", "MIN_VALUE", "Total rows must be greater than or equal to 0");
        }
        if (command.validRows() < 0) {
            throw validation("valid_count", "MIN_VALUE", "Valid count must be greater than or equal to 0");
        }
        if (command.errorRows() < 0) {
            throw validation("error_count", "MIN_VALUE", "Error count must be greater than or equal to 0");
        }
        if (command.duplicateRows() < 0) {
            throw validation("duplicate_count", "MIN_VALUE", "Duplicate count must be greater than or equal to 0");
        }
        int summarizedRows = command.validRows() + command.errorRows() + command.duplicateRows();
        if (summarizedRows > command.totalRows()) {
            throw new BusinessException(ErrorCode.IMPORT_ROW_VALIDATION_ERROR, "Import row counts exceed total rows");
        }
    }

    private void validateRows(LeadImportFixtureCommand command) {
        if (command.rows() == null || command.rows().isEmpty()) {
            throw validation("rows", "REQUIRED", "Import fixture rows are required");
        }
        for (LeadImportFixtureRowCommand row : command.rows()) {
            if (row.rowNumber() < 1) {
                throw validation("row_number", "MIN_VALUE", "Row number must be greater than 0");
            }
            if (row.rowStatus() == null) {
                throw validation("row_status", "REQUIRED", "Row status is required");
            }
            if (row.rowStatus() == ImportRowStatus.ERROR && (row.errorDetails() == null || row.errorDetails().isEmpty())) {
                throw validation("error_details", "REQUIRED", "Error rows must include error details");
            }
        }
    }

    private LeadImportFixtureCommand defaultCommand(UUID tenantId, UUID actorId) {
        Map<String, Object> mappingConfig = new LinkedHashMap<>();
        mappingConfig.put("columns", Map.of(
                "full_name", "Full Name",
                "phone", "Phone",
                "email", "Email",
                "language_code", "Language",
                "source", "Source"
        ));
        mappingConfig.put("fixture", true);

        return new LeadImportFixtureCommand(
                tenantId,
                actorId,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                DEFAULT_FIXTURE_FILE_NAME,
                mappingConfig,
                3,
                1,
                1,
                1,
                List.of(
                        new LeadImportFixtureRowCommand(
                                1,
                                ImportRowStatus.VALID,
                                Map.of("Full Name", "Fixture Valid Lead", "Phone", "0900000001", "Language", "English"),
                                Map.of("full_name", "Fixture Valid Lead", "phone", "0900000001", "language_code", "EN"),
                                List.of()
                        ),
                        new LeadImportFixtureRowCommand(
                                2,
                                ImportRowStatus.ERROR,
                                Map.of("Full Name", "Fixture Missing Contact", "Phone", "", "Email", ""),
                                Map.of("full_name", "Fixture Missing Contact"),
                                List.of(ErrorDetail.of("phone", "CONTACT_IDENTIFIER_REQUIRED", "Lead must include at least one contact identifier"))
                        ),
                        new LeadImportFixtureRowCommand(
                                3,
                                ImportRowStatus.DUPLICATE,
                                Map.of("Full Name", "Fixture Duplicate Lead", "Phone", "0900000001"),
                                Map.of("full_name", "Fixture Duplicate Lead", "phone", "0900000001"),
                                List.of(ErrorDetail.of("phone", "DUPLICATE_CANDIDATE", "Potential duplicate contact"))
                        )
                )
        );
    }

    private void auditFixtureCreated(ImportBatch batch, UUID actorId) {
        auditService.record(new AuditRecordCommand(
                batch.tenantId(),
                actorId,
                "SYSTEM",
                "FIXTURE",
                AuditActions.IMPORT_BATCH_CREATED,
                AuditResourceTypes.IMPORT_BATCH,
                batch.id(),
                batch.originalFileName(),
                null,
                leadImportMapper.toAuditData(batch),
                Map.of("fixture", true),
                "Seed lead import fixture",
                LogContext.requestId()
        ));
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
