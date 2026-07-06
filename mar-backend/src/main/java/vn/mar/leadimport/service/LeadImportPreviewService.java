package vn.mar.leadimport.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.tenant.TenantContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.lead.api.LeadNormalizationIssue;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.api.LeadNormalizationService;
import vn.mar.lead.model.LeadStatus;
import vn.mar.leadimport.dto.request.LeadImportPreviewRequest;
import vn.mar.leadimport.dto.response.LeadImportDuplicateCandidateResponse;
import vn.mar.leadimport.dto.response.LeadImportPreviewResponse;
import vn.mar.leadimport.dto.response.LeadImportPreviewRowErrorResponse;
import vn.mar.leadimport.dto.response.LeadImportValidSampleResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.mapper.LeadImportMapper;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.LeadImportField;
import vn.mar.leadimport.parser.CsvLeadImportParser;
import vn.mar.leadimport.parser.ParsedCsvLeadImport;
import vn.mar.leadimport.parser.ParsedCsvLeadImportRow;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class LeadImportPreviewService {

    private static final long MAX_CSV_BYTES = 1_048_576L;
    private static final int MAX_CSV_ROWS = 500;
    private static final int MAX_VALID_SAMPLES = 5;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/csv",
            "application/csv",
            "application/vnd.ms-excel",
            "text/plain",
            "application/octet-stream"
    );

    private final ImportBatchRepository importBatchRepository;
    private final ImportRowRepository importRowRepository;
    private final LanguageRepository languageRepository;
    private final ProgramRepository programRepository;
    private final BranchRepository branchRepository;
    private final CsvLeadImportParser csvLeadImportParser;
    private final LeadImportMapper leadImportMapper;
    private final LeadNormalizationService leadNormalizationService;
    private final CurrentUserContext currentUserContext;
    private final TimeProvider timeProvider;
    private final AuditService auditService;

    public LeadImportPreviewService(
            ImportBatchRepository importBatchRepository,
            ImportRowRepository importRowRepository,
            LanguageRepository languageRepository,
            ProgramRepository programRepository,
            BranchRepository branchRepository,
            CsvLeadImportParser csvLeadImportParser,
            LeadImportMapper leadImportMapper,
            LeadNormalizationService leadNormalizationService,
            CurrentUserContext currentUserContext,
            TimeProvider timeProvider,
            AuditService auditService) {
        this.importBatchRepository = importBatchRepository;
        this.importRowRepository = importRowRepository;
        this.languageRepository = languageRepository;
        this.programRepository = programRepository;
        this.branchRepository = branchRepository;
        this.csvLeadImportParser = csvLeadImportParser;
        this.leadImportMapper = leadImportMapper;
        this.leadNormalizationService = leadNormalizationService;
        this.currentUserContext = currentUserContext;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    @Transactional
    public LeadImportPreviewResponse previewLeadImport(LeadImportPreviewRequest request, MultipartFile file) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        validateFile(file);
        ParsedCsvLeadImport parsedFile = csvLeadImportParser.parse(file);
        if (parsedFile.rows().size() > MAX_CSV_ROWS) {
            throw invalidFile("file", "MAX_ROWS", "CSV preview supports at most 500 data rows");
        }

        Map<LeadImportField, String> mappings = resolveMappings(request, parsedFile.headers());
        validateDefaultCatalogValues(tenantId, request);
        List<LeadRowPreview> previewRows = buildPreviewRows(tenantId, parsedFile.rows(), mappings, request);
        markDuplicateCandidates(previewRows);

        int totalRows = previewRows.size();
        int errorRows = countRows(previewRows, ImportRowStatus.ERROR);
        int duplicateRows = countRows(previewRows, ImportRowStatus.DUPLICATE);
        int validRows = countRows(previewRows, ImportRowStatus.VALID);
        Instant now = timeProvider.now();
        ImportBatch batch = ImportBatch.createPreview(
                UUID.randomUUID(),
                tenantId,
                ImportSourceType.CSV,
                buildMappingConfig(request, mappings, parsedFile.headers()),
                safeOriginalFileName(file),
                totalRows,
                validRows,
                errorRows,
                duplicateRows,
                actor.actorId(),
                now
        );
        ImportBatch savedBatch = importBatchRepository.save(batch);
        List<ImportRow> rows = previewRows.stream()
                .map(row -> ImportRow.create(
                        UUID.randomUUID(),
                        tenantId,
                        savedBatch.id(),
                        row.rowNumber,
                        row.status(),
                        row.rawRow,
                        row.normalizedRow,
                        row.errorDetails(),
                        now
                ))
                .toList();
        importRowRepository.saveAll(rows);
        auditPreviewCreated(savedBatch, actor);
        return toResponse(savedBatch, previewRows, mappings);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("file", "REQUIRED", "CSV file is required");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw invalidFile("file", "MAX_SIZE", "CSV file must be less than or equal to 1 MB");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)
                && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw invalidFile("file", "INVALID_CONTENT_TYPE", "CSV file content type is invalid");
        }
        String fileName = safeOriginalFileName(file);
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw invalidFile("file", "INVALID_EXTENSION", "CSV file extension is required");
        }
    }

    private Map<LeadImportField, String> resolveMappings(LeadImportPreviewRequest request, List<String> headers) {
        if (request == null || request.columnMappings() == null || request.columnMappings().isEmpty()) {
            throw ValidationException.of("column_mappings", "REQUIRED", "Column mappings are required");
        }
        Map<String, String> headerLookup = new HashMap<>();
        for (String header : headers) {
            headerLookup.put(header.toLowerCase(Locale.ROOT), header);
        }
        List<ErrorDetail> details = new ArrayList<>();
        Map<LeadImportField, String> mappings = new EnumMap<>(LeadImportField.class);
        Map<String, LeadImportField> usedSourceColumns = new HashMap<>();
        for (Map.Entry<String, String> entry : request.columnMappings().entrySet()) {
            String requestedField = entry.getKey() == null ? "" : entry.getKey().trim();
            String sourceColumn = entry.getValue() == null ? "" : entry.getValue().trim();
            LeadImportField field = LeadImportField.fromCode(requestedField).orElse(null);
            if (field == null) {
                details.add(ErrorDetail.of("column_mappings." + requestedField, "UNKNOWN_FIELD", "Import field is not supported"));
                continue;
            }
            if (!StringUtils.hasText(sourceColumn)) {
                details.add(ErrorDetail.of("column_mappings." + field.code(), "REQUIRED", "Source column is required"));
                continue;
            }
            String resolvedColumn = headerLookup.get(sourceColumn.toLowerCase(Locale.ROOT));
            if (resolvedColumn == null) {
                details.add(ErrorDetail.of("column_mappings." + field.code(), "UNKNOWN_COLUMN", "Mapped source column does not exist in CSV header"));
                continue;
            }
            LeadImportField previousField = usedSourceColumns.putIfAbsent(resolvedColumn.toLowerCase(Locale.ROOT), field);
            if (previousField != null && previousField != field) {
                details.add(ErrorDetail.of("column_mappings." + field.code(), "DUPLICATE_COLUMN_MAPPING", "A source column can map to only one import field"));
                continue;
            }
            mappings.put(field, resolvedColumn);
        }
        if (mappings.keySet().stream().noneMatch(LeadImportField::contactIdentifier)) {
            details.add(ErrorDetail.of("column_mappings", "CONTACT_MAPPING_REQUIRED", "At least one of phone, email, or Zalo ID must be mapped"));
        }
        if (!details.isEmpty()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
        }
        return mappings;
    }

    private void validateDefaultCatalogValues(UUID tenantId, LeadImportPreviewRequest request) {
        if (request == null) {
            return;
        }
        List<ErrorDetail> details = new ArrayList<>();
        if (StringUtils.hasText(request.defaultLanguageCode())
                && !languageRepository.existsByTenantIdAndLanguageCodeIgnoreCaseAndStatus(
                        tenantId,
                        request.defaultLanguageCode().trim(),
                        CatalogStatus.ACTIVE)) {
            details.add(ErrorDetail.of("default_language_code", "INVALID_LANGUAGE_CODE", "Default language code is not active"));
        }
        if (StringUtils.hasText(request.defaultProgramCode())
                && !programRepository.existsByTenantIdAndProgramCodeIgnoreCaseAndStatus(
                        tenantId,
                        request.defaultProgramCode().trim(),
                        CatalogStatus.ACTIVE)) {
            details.add(ErrorDetail.of("default_program_code", "INVALID_PROGRAM_CODE", "Default program code is not active"));
        }
        if (StringUtils.hasText(request.defaultBranchCode())
                && !branchRepository.existsByTenantIdAndBranchCodeIgnoreCaseAndStatus(
                        tenantId,
                        request.defaultBranchCode().trim(),
                        BranchStatus.ACTIVE)) {
            details.add(ErrorDetail.of("default_branch_code", "INVALID_BRANCH_CODE", "Default branch code is not active"));
        }
        if (!details.isEmpty()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
        }
    }

    private List<LeadRowPreview> buildPreviewRows(
            UUID tenantId,
            List<ParsedCsvLeadImportRow> rows,
            Map<LeadImportField, String> mappings,
            LeadImportPreviewRequest request) {
        Map<String, Boolean> languageCache = new HashMap<>();
        Map<String, Boolean> programCache = new HashMap<>();
        Map<String, Boolean> branchCache = new HashMap<>();
        List<LeadRowPreview> previews = new ArrayList<>();
        for (ParsedCsvLeadImportRow row : rows) {
            LeadRowPreview preview = new LeadRowPreview(row.rowNumber(), toRawRow(row));
            normalizeRow(preview, row, mappings, request);
            validateCatalogCodes(tenantId, preview, languageCache, programCache, branchCache);
            if (!preview.errors.isEmpty()) {
                preview.normalizedRow.put("lead_status", LeadStatus.INVALID.name());
            }
            previews.add(preview);
        }
        return previews;
    }

    private Map<String, Object> toRawRow(ParsedCsvLeadImportRow row) {
        Map<String, Object> raw = new LinkedHashMap<>();
        row.values().forEach(raw::put);
        return raw;
    }

    private void normalizeRow(
            LeadRowPreview preview,
            ParsedCsvLeadImportRow row,
            Map<LeadImportField, String> mappings,
            LeadImportPreviewRequest request) {
        putIfText(preview.normalizedRow, LeadImportField.FULL_NAME.code(), rawValue(row, mappings.get(LeadImportField.FULL_NAME)));

        String phoneRaw = rawValue(row, mappings.get(LeadImportField.PHONE));
        String emailRaw = rawValue(row, mappings.get(LeadImportField.EMAIL));
        String zaloRaw = rawValue(row, mappings.get(LeadImportField.ZALO_ID));
        LeadNormalizationResult normalizedContact = leadNormalizationService.normalize(
                new LeadNormalizationRequest(phoneRaw, emailRaw, zaloRaw)
        );
        putIfText(preview.normalizedRow, LeadImportField.PHONE.code(), normalizedContact.phoneNormalized());
        putIfText(preview.normalizedRow, LeadImportField.EMAIL.code(), normalizedContact.email());
        putIfText(preview.normalizedRow, LeadImportField.ZALO_ID.code(), normalizedContact.zaloId());
        preview.normalizedRow.put("contactability", normalizedContact.contactability().name());
        preview.normalizedRow.put("lead_status", normalizedContact.leadStatus().name());
        normalizedContact.issues().stream()
                .map(issue -> toRowError(preview.rowNumber, issue))
                .forEach(preview.errors::add);

        putIfText(preview.normalizedRow, LeadImportField.SOURCE.code(),
                firstText(rawValue(row, mappings.get(LeadImportField.SOURCE)), request.defaultSource()));
        putIfText(preview.normalizedRow, LeadImportField.CAMPAIGN.code(), rawValue(row, mappings.get(LeadImportField.CAMPAIGN)));
        putIfText(preview.normalizedRow, LeadImportField.LANGUAGE_CODE.code(),
                firstText(rawValue(row, mappings.get(LeadImportField.LANGUAGE_CODE)), request.defaultLanguageCode()));
        putIfText(preview.normalizedRow, LeadImportField.PROGRAM_CODE.code(),
                firstText(rawValue(row, mappings.get(LeadImportField.PROGRAM_CODE)), request.defaultProgramCode()));
        putIfText(preview.normalizedRow, LeadImportField.BRANCH_CODE.code(),
                firstText(rawValue(row, mappings.get(LeadImportField.BRANCH_CODE)), request.defaultBranchCode()));
    }

    private void validateCatalogCodes(
            UUID tenantId,
            LeadRowPreview preview,
            Map<String, Boolean> languageCache,
            Map<String, Boolean> programCache,
            Map<String, Boolean> branchCache) {
        String languageCode = textValue(preview.normalizedRow.get(LeadImportField.LANGUAGE_CODE.code()));
        if (StringUtils.hasText(languageCode)
                && !languageCache.computeIfAbsent(languageCode.toLowerCase(Locale.ROOT),
                        ignored -> languageRepository.existsByTenantIdAndLanguageCodeIgnoreCaseAndStatus(
                                tenantId,
                                languageCode,
                                CatalogStatus.ACTIVE))) {
            preview.addError(LeadImportField.LANGUAGE_CODE, "INVALID_LANGUAGE_CODE", "Language code is not active");
        }
        String programCode = textValue(preview.normalizedRow.get(LeadImportField.PROGRAM_CODE.code()));
        if (StringUtils.hasText(programCode)
                && !programCache.computeIfAbsent(programCode.toLowerCase(Locale.ROOT),
                        ignored -> programRepository.existsByTenantIdAndProgramCodeIgnoreCaseAndStatus(
                                tenantId,
                                programCode,
                                CatalogStatus.ACTIVE))) {
            preview.addError(LeadImportField.PROGRAM_CODE, "INVALID_PROGRAM_CODE", "Program code is not active");
        }
        String branchCode = textValue(preview.normalizedRow.get(LeadImportField.BRANCH_CODE.code()));
        if (StringUtils.hasText(branchCode)
                && !branchCache.computeIfAbsent(branchCode.toLowerCase(Locale.ROOT),
                        ignored -> branchRepository.existsByTenantIdAndBranchCodeIgnoreCaseAndStatus(
                                tenantId,
                                branchCode,
                                BranchStatus.ACTIVE))) {
            preview.addError(LeadImportField.BRANCH_CODE, "INVALID_BRANCH_CODE", "Branch code is not active");
        }
    }

    private void markDuplicateCandidates(List<LeadRowPreview> rows) {
        Map<String, SeenIdentifier> seen = new HashMap<>();
        for (LeadRowPreview row : rows) {
            if (!row.errors.isEmpty()) {
                continue;
            }
            DuplicateCandidate duplicate = findDuplicate(row, seen);
            if (duplicate != null) {
                row.duplicateCandidate = duplicate;
                row.normalizedRow.put("lead_status", LeadStatus.DUPLICATE_REVIEW.name());
                row.errors.add(ErrorDetail.of(
                        "rows[" + row.rowNumber + "]." + duplicate.fieldCode,
                        "DUPLICATE_CANDIDATE",
                        "Potential duplicate by " + duplicate.matchType
                ));
            } else {
                rememberIdentifier(row, seen, LeadImportField.PHONE, "PHONE_EXACT");
                rememberIdentifier(row, seen, LeadImportField.EMAIL, "EMAIL_EXACT");
                rememberIdentifier(row, seen, LeadImportField.ZALO_ID, "ZALO_ID_EXACT");
            }
        }
    }

    private DuplicateCandidate findDuplicate(LeadRowPreview row, Map<String, SeenIdentifier> seen) {
        for (LeadImportField field : List.of(LeadImportField.PHONE, LeadImportField.EMAIL, LeadImportField.ZALO_ID)) {
            String value = textValue(row.normalizedRow.get(field.code()));
            if (!StringUtils.hasText(value)) {
                continue;
            }
            SeenIdentifier matched = seen.get(identifierKey(field, value));
            if (matched != null) {
                return new DuplicateCandidate(field.code(), matched.matchType, matched.rowNumber, value);
            }
        }
        return null;
    }

    private void rememberIdentifier(
            LeadRowPreview row,
            Map<String, SeenIdentifier> seen,
            LeadImportField field,
            String matchType) {
        String value = textValue(row.normalizedRow.get(field.code()));
        if (StringUtils.hasText(value)) {
            seen.putIfAbsent(identifierKey(field, value), new SeenIdentifier(row.rowNumber, matchType));
        }
    }

    private String identifierKey(LeadImportField field, String value) {
        return field.code() + ":" + value.toLowerCase(Locale.ROOT);
    }

    private int countRows(List<LeadRowPreview> rows, ImportRowStatus status) {
        return (int) rows.stream()
                .filter(row -> row.status() == status)
                .count();
    }

    private Map<String, Object> buildMappingConfig(
            LeadImportPreviewRequest request,
            Map<LeadImportField, String> mappings,
            List<String> headers) {
        Map<String, Object> columnMappings = new LinkedHashMap<>();
        mappings.forEach((field, sourceColumn) -> columnMappings.put(field.code(), sourceColumn));
        Map<String, Object> defaults = new LinkedHashMap<>();
        putIfText(defaults, "source", request.defaultSource());
        putIfText(defaults, "language_code", request.defaultLanguageCode());
        putIfText(defaults, "program_code", request.defaultProgramCode());
        putIfText(defaults, "branch_code", request.defaultBranchCode());
        Map<String, Object> mappingConfig = new LinkedHashMap<>();
        mappingConfig.put("mode", "SYNC_PREVIEW");
        mappingConfig.put("source_type", ImportSourceType.CSV.name());
        mappingConfig.put("column_mappings", columnMappings);
        mappingConfig.put("headers", List.copyOf(headers));
        if (!defaults.isEmpty()) {
            mappingConfig.put("defaults", defaults);
        }
        return mappingConfig;
    }

    private LeadImportPreviewResponse toResponse(
            ImportBatch batch,
            List<LeadRowPreview> previewRows,
            Map<LeadImportField, String> mappings) {
        return new LeadImportPreviewResponse(
                batch.id(),
                batch.status().name(),
                batch.totalRows(),
                batch.validRows(),
                batch.errorRows(),
                batch.duplicateRows(),
                batch.validRows(),
                0,
                previewRows.stream()
                        .filter(row -> row.status() == ImportRowStatus.ERROR)
                        .flatMap(row -> row.errors.stream()
                                .map(error -> toErrorResponse(row, error, mappings)))
                        .toList(),
                previewRows.stream()
                        .filter(row -> row.status() == ImportRowStatus.DUPLICATE)
                        .map(this::toDuplicateResponse)
                        .toList(),
                previewRows.stream()
                        .filter(row -> row.status() == ImportRowStatus.VALID)
                        .limit(MAX_VALID_SAMPLES)
                        .map(row -> new LeadImportValidSampleResponse(row.rowNumber, maskNormalizedRow(row.normalizedRow)))
                        .toList()
        );
    }

    private LeadImportPreviewRowErrorResponse toErrorResponse(
            LeadRowPreview row,
            ErrorDetail error,
            Map<LeadImportField, String> mappings) {
        String fieldCode = extractFieldCode(error.field());
        String rawValue = rawValue(row.rawRow, mappings, fieldCode);
        return new LeadImportPreviewRowErrorResponse(
                row.rowNumber,
                fieldCode,
                error.code(),
                error.message(),
                maskValue(fieldCode, rawValue)
        );
    }

    private LeadImportDuplicateCandidateResponse toDuplicateResponse(LeadRowPreview row) {
        DuplicateCandidate duplicate = row.duplicateCandidate;
        return new LeadImportDuplicateCandidateResponse(
                row.rowNumber,
                duplicate.fieldCode,
                duplicate.matchType,
                duplicate.matchedRowNumber,
                maskValue(duplicate.fieldCode, duplicate.rawValue),
                "REVIEW_BEFORE_CONFIRM"
        );
    }

    private void auditPreviewCreated(ImportBatch batch, CurrentUser actor) {
        auditService.record(new AuditRecordCommand(
                batch.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                AuditActions.IMPORT_BATCH_CREATED,
                AuditResourceTypes.IMPORT_BATCH,
                batch.id(),
                batch.originalFileName(),
                null,
                leadImportMapper.toAuditData(batch),
                Map.of("preview", true, "source_type", ImportSourceType.CSV.name()),
                "Preview lead import CSV",
                LogContext.requestId()
        ));
    }


    private String rawValue(ParsedCsvLeadImportRow row, String sourceColumn) {
        if (!StringUtils.hasText(sourceColumn)) {
            return "";
        }
        return row.values().getOrDefault(sourceColumn, "");
    }

    private String rawValue(Map<String, Object> rawRow, Map<LeadImportField, String> mappings, String fieldCode) {
        LeadImportField field = LeadImportField.fromCode(fieldCode).orElse(null);
        if (field == null) {
            return null;
        }
        String sourceColumn = mappings.get(field);
        if (!StringUtils.hasText(sourceColumn)) {
            return null;
        }
        return textValue(rawRow.get(sourceColumn));
    }

    private String extractFieldCode(String field) {
        if (!StringUtils.hasText(field)) {
            return null;
        }
        int separator = field.lastIndexOf('.');
        return separator >= 0 ? field.substring(separator + 1) : field;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : normalizeText(fallback);
    }

    private void putIfText(Map<String, Object> target, String field, String value) {
        if (StringUtils.hasText(value)) {
            target.put(field, value.trim());
        }
    }

    private String textValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String maskValue(String fieldCode, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (LeadImportField.EMAIL.code().equals(fieldCode)) {
            int atIndex = value.indexOf('@');
            if (atIndex <= 1) {
                return "***" + (atIndex >= 0 ? value.substring(atIndex) : "");
            }
            return value.charAt(0) + "***" + value.substring(atIndex);
        }
        if (LeadImportField.PHONE.code().equals(fieldCode) || LeadImportField.ZALO_ID.code().equals(fieldCode)) {
            String trimmed = value.trim();
            if (trimmed.length() <= 3) {
                return "***";
            }
            return "***" + trimmed.substring(trimmed.length() - 3);
        }
        return "***";
    }

    private Map<String, Object> maskNormalizedRow(Map<String, Object> normalizedRow) {
        Map<String, Object> masked = new LinkedHashMap<>();
        normalizedRow.forEach((field, value) -> {
            if (LeadImportField.PHONE.code().equals(field)
                    || LeadImportField.EMAIL.code().equals(field)
                    || LeadImportField.ZALO_ID.code().equals(field)) {
                masked.put(field, maskValue(field, textValue(value)));
            } else {
                masked.put(field, value);
            }
        });
        return masked;
    }

    private String safeOriginalFileName(MultipartFile file) {
        String filename = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(filename)) {
            return "lead-import.csv";
        }
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }


    private ErrorDetail toRowError(int rowNumber, LeadNormalizationIssue issue) {
        return ErrorDetail.of(
                "rows[" + rowNumber + "]." + issue.field().code(),
                issue.code().name(),
                issue.message()
        );
    }

    private BusinessException invalidFile(String field, String code, String message) {
        return new BusinessException(
                ErrorCode.IMPORT_FILE_INVALID,
                ErrorCode.IMPORT_FILE_INVALID.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }

    private static final class LeadRowPreview {
        private final int rowNumber;
        private final Map<String, Object> rawRow;
        private final Map<String, Object> normalizedRow = new LinkedHashMap<>();
        private final List<ErrorDetail> errors = new ArrayList<>();
        private DuplicateCandidate duplicateCandidate;

        private LeadRowPreview(int rowNumber, Map<String, Object> rawRow) {
            this.rowNumber = rowNumber;
            this.rawRow = rawRow;
        }

        private void addError(LeadImportField field, String code, String message) {
            errors.add(ErrorDetail.of("rows[" + rowNumber + "]." + field.code(), code, message));
        }

        private ImportRowStatus status() {
            if (duplicateCandidate != null) {
                return ImportRowStatus.DUPLICATE;
            }
            return errors.isEmpty() ? ImportRowStatus.VALID : ImportRowStatus.ERROR;
        }

        private List<ErrorDetail> errorDetails() {
            return List.copyOf(errors);
        }
    }

    private record SeenIdentifier(
            int rowNumber,
            String matchType
    ) {
    }

    private record DuplicateCandidate(
            String fieldCode,
            String matchType,
            int matchedRowNumber,
            String rawValue
    ) {
    }
}
