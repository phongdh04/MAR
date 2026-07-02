package vn.mar.leadimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.leadimport.dto.request.ImportRowErrorSearchRequest;
import vn.mar.leadimport.dto.request.LeadImportSearchRequest;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.dto.response.ImportBatchSummaryResponse;
import vn.mar.leadimport.dto.response.ImportRowErrorResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.mapper.LeadImportMapper;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class LeadImportQueryServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ROW_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private ImportRowRepository importRowRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    private LeadImportQueryService leadImportQueryService;

    @BeforeEach
    void setUp() {
        leadImportQueryService = new LeadImportQueryService(
                importBatchRepository,
                importRowRepository,
                new LeadImportMapper(),
                currentUserContext
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of("import.view"),
                "req_import_unit_001"
        ));
    }

    @Test
    void searchLeadImports_whenFiltersValid_shouldReturnTenantScopedPage() {
        when(importBatchRepository.search(
                eq(TENANT_ID),
                eq(ImportType.LEAD),
                eq(ImportBatchStatus.DRAFT),
                eq(ImportSourceType.CSV),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(batch()), PageRequest.of(0, 20), 1));

        PageResponse<ImportBatchSummaryResponse> response = leadImportQueryService.searchLeadImports(
                new LeadImportSearchRequest("DRAFT", "CSV", null, null)
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().batchId()).isEqualTo(BATCH_ID);
        assertThat(response.items().getFirst().errorCount()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void searchLeadImports_whenStatusInvalid_shouldRejectWithValidationError() {
        assertThatThrownBy(() -> leadImportQueryService.searchLeadImports(
                new LeadImportSearchRequest("DONE", null, null, null)
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void getLeadImportBatch_whenCrossTenant_shouldThrowImportBatchNotFound() {
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadImportQueryService.getLeadImportBatch(BATCH_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMPORT_BATCH_NOT_FOUND);

        verify(importBatchRepository).findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD);
        verify(importBatchRepository, never()).findById(BATCH_ID);
    }

    @Test
    void getLeadImportBatch_whenFound_shouldReturnMappingConfigAndCounts() {
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.of(batch()));

        ImportBatchDetailResponse response = leadImportQueryService.getLeadImportBatch(BATCH_ID);

        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        assertThat(response.mappingConfig()).containsKey("columns");
        assertThat(response.totalRows()).isEqualTo(3);
    }

    @Test
    void getLeadImportErrors_whenFound_shouldReturnOnlyErrorRowsWithoutRawPayload() {
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.of(batch()));
        when(importRowRepository.findByTenantIdAndImportBatchIdAndRowStatusOrderByRowNumberAsc(
                eq(TENANT_ID),
                eq(BATCH_ID),
                eq(ImportRowStatus.ERROR),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(errorRow()), PageRequest.of(0, 20), 1));

        PageResponse<ImportRowErrorResponse> response = leadImportQueryService.getLeadImportErrors(
                BATCH_ID,
                new ImportRowErrorSearchRequest(null, null)
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().rowNumber()).isEqualTo(2);
        assertThat(response.items().getFirst().field()).isEqualTo("phone");
        assertThat(response.items().getFirst().code()).isEqualTo("CONTACT_IDENTIFIER_REQUIRED");
    }

    private ImportBatch batch() {
        return ImportBatch.restore(
                BATCH_ID,
                TENANT_ID,
                ImportType.LEAD,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                Map.of("columns", Map.of("phone", "Phone")),
                null,
                "qa-lead-import-fixture.csv",
                3,
                1,
                1,
                1,
                null,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private ImportRow errorRow() {
        return ImportRow.restore(
                ROW_ID,
                TENANT_ID,
                BATCH_ID,
                2,
                ImportRowStatus.ERROR,
                Map.of("Phone", ""),
                Map.of("full_name", "Fixture Missing Contact"),
                List.of(ErrorDetail.of("phone", "CONTACT_IDENTIFIER_REQUIRED", "Lead must include contact")),
                NOW
        );
    }
}
