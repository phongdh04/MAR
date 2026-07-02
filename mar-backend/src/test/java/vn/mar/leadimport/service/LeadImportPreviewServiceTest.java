package vn.mar.leadimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.leadimport.dto.request.LeadImportPreviewRequest;
import vn.mar.leadimport.dto.response.LeadImportPreviewResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.mapper.LeadImportMapper;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.parser.CsvLeadImportParser;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class LeadImportPreviewServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private ImportRowRepository importRowRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private LeadImportPreviewService leadImportPreviewService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        leadImportPreviewService = new LeadImportPreviewService(
                importBatchRepository,
                importRowRepository,
                languageRepository,
                programRepository,
                branchRepository,
                new CsvLeadImportParser(),
                new LeadImportMapper(),
                currentUserContext,
                timeProvider,
                auditService
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of("lead.import"),
                "req_preview_unit_001"
        ));
    }

    @Test
    void previewLeadImport_whenCsvValid_shouldCreatePreviewBatchRowsAndAuditWithoutRawRows() {
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeadImportPreviewResponse response = leadImportPreviewService.previewLeadImport(validRequest(), csvFile("""
                Full Name,Phone,Email
                Valid Lead,0900000001,valid@example.com
                Missing Contact,,
                Duplicate Phone,0900000001,duplicate@example.com
                """));

        assertThat(response.status()).isEqualTo(ImportBatchStatus.PREVIEWED.name());
        assertThat(response.totalRows()).isEqualTo(3);
        assertThat(response.validCount()).isEqualTo(1);
        assertThat(response.errorCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isEqualTo(1);
        assertThat(response.duplicateCandidates().getFirst().matchType()).isEqualTo("PHONE_EXACT");
        assertThat(response.duplicateCandidates().getFirst().rawValue()).isEqualTo("***001");
        assertThat(response.errorRows().getFirst().code()).isEqualTo("CONTACT_IDENTIFIER_REQUIRED");
        assertThat(response.validSamples().getFirst().normalizedRow()).containsEntry("phone", "***001");

        ArgumentCaptor<ImportBatch> batchCaptor = ArgumentCaptor.forClass(ImportBatch.class);
        verify(importBatchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().status()).isEqualTo(ImportBatchStatus.PREVIEWED);
        assertThat(batchCaptor.getValue().mappingConfig()).containsKey("column_mappings");

        ArgumentCaptor<Iterable<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        List<ImportRow> savedRows = StreamSupport.stream(rowsCaptor.getValue().spliterator(), false).toList();
        assertThat(savedRows).extracting(ImportRow::rowStatus)
                .containsExactly(ImportRowStatus.VALID, ImportRowStatus.ERROR, ImportRowStatus.DUPLICATE);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.IMPORT_BATCH_CREATED);
        assertThat(auditCaptor.getValue().afterData()).doesNotContainKey("raw_row");
    }

    @Test
    void previewLeadImport_whenMappingMissingContact_shouldRejectWithValidationError() {
        LeadImportPreviewRequest request = new LeadImportPreviewRequest(
                Map.of("full_name", "Full Name"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> leadImportPreviewService.previewLeadImport(request, csvFile("""
                Full Name,Phone
                Missing Mapping,0900000001
                """)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void previewLeadImport_whenCsvHasNoDataRows_shouldRejectWithImportFileInvalid() {
        assertThatThrownBy(() -> leadImportPreviewService.previewLeadImport(validRequest(), csvFile("Full Name,Phone\n")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMPORT_FILE_INVALID);
    }

    @Test
    void previewLeadImport_whenFileTooLarge_shouldRejectWithImportFileInvalid() {
        byte[] payload = new byte[1_048_577];
        MockMultipartFile file = new MockMultipartFile("file", "too-large.csv", "text/csv", payload);

        assertThatThrownBy(() -> leadImportPreviewService.previewLeadImport(validRequest(), file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMPORT_FILE_INVALID);
    }

    @Test
    void previewLeadImport_whenCatalogCodeInactive_shouldKeepBatchAndMarkRowError() {
        when(languageRepository.existsByTenantIdAndLanguageCodeIgnoreCaseAndStatus(
                eq(TENANT_ID),
                eq("EN"),
                eq(CatalogStatus.ACTIVE)
        )).thenReturn(false);
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeadImportPreviewRequest request = new LeadImportPreviewRequest(
                Map.of("full_name", "Full Name", "phone", "Phone", "language_code", "Language"),
                null,
                null,
                null,
                null
        );

        LeadImportPreviewResponse response = leadImportPreviewService.previewLeadImport(request, csvFile("""
                Full Name,Phone,Language
                Unknown Catalog,0900000001,EN
                """));

        assertThat(response.validCount()).isZero();
        assertThat(response.errorCount()).isEqualTo(1);
        assertThat(response.errorRows().getFirst().code()).isEqualTo("INVALID_LANGUAGE_CODE");
    }

    @Test
    void previewLeadImport_whenDefaultProgramOrBranchInactive_shouldRejectRequest() {
        when(programRepository.existsByTenantIdAndProgramCodeIgnoreCaseAndStatus(
                eq(TENANT_ID),
                eq("IELTS"),
                eq(CatalogStatus.ACTIVE)
        )).thenReturn(true);
        when(branchRepository.existsByTenantIdAndBranchCodeIgnoreCaseAndStatus(
                eq(TENANT_ID),
                eq("HCM01"),
                eq(BranchStatus.ACTIVE)
        )).thenReturn(false);
        LeadImportPreviewRequest request = new LeadImportPreviewRequest(
                Map.of("full_name", "Full Name", "phone", "Phone"),
                null,
                null,
                "IELTS",
                "HCM01"
        );

        assertThatThrownBy(() -> leadImportPreviewService.previewLeadImport(request, csvFile("""
                Full Name,Phone
                Valid Lead,0900000001
                """)))
                .isInstanceOf(ValidationException.class);
    }

    private LeadImportPreviewRequest validRequest() {
        return new LeadImportPreviewRequest(
                Map.of("full_name", "Full Name", "phone", "Phone", "email", "Email"),
                null,
                null,
                null,
                null
        );
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "leads.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
