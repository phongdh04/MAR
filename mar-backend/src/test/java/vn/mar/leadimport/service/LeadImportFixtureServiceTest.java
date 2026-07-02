package vn.mar.leadimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
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

@ExtendWith(MockitoExtension.class)
class LeadImportFixtureServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private ImportRowRepository importRowRepository;

    @Mock
    private AuditService auditService;

    private LeadImportFixtureService leadImportFixtureService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        leadImportFixtureService = new LeadImportFixtureService(
                importBatchRepository,
                importRowRepository,
                new LeadImportMapper(),
                timeProvider,
                auditService
        );
    }

    @Test
    void seedDefaultLeadFixture_whenMissing_shouldCreateBatchRowsAndAuditWithoutRawRows() {
        when(importBatchRepository.findFirstByTenantIdAndImportTypeAndOriginalFileNameOrderByCreatedAtDesc(
                TENANT_ID,
                ImportType.LEAD,
                LeadImportFixtureService.DEFAULT_FIXTURE_FILE_NAME
        )).thenReturn(Optional.empty());
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importRowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportBatchDetailResponse response = leadImportFixtureService.seedDefaultLeadFixture(TENANT_ID, ACTOR_ID);

        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.totalRows()).isEqualTo(3);
        assertThat(response.errorCount()).isEqualTo(1);

        ArgumentCaptor<Iterable<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(3);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.IMPORT_BATCH_CREATED);
        assertThat(auditCaptor.getValue().afterData()).doesNotContainKey("raw_row");
    }

    @Test
    void seedFixture_whenExisting_shouldReturnExistingAndNotCreateRowsAgain() {
        when(importBatchRepository.findFirstByTenantIdAndImportTypeAndOriginalFileNameOrderByCreatedAtDesc(
                TENANT_ID,
                ImportType.LEAD,
                LeadImportFixtureService.DEFAULT_FIXTURE_FILE_NAME
        )).thenReturn(Optional.of(existingBatch()));

        ImportBatchDetailResponse response = leadImportFixtureService.seedDefaultLeadFixture(TENANT_ID, ACTOR_ID);

        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        verify(importBatchRepository, never()).save(any());
        verify(importRowRepository, never()).saveAll(any());
    }

    @Test
    void seedFixture_whenNegativeCount_shouldRejectWithValidationError() {
        LeadImportFixtureCommand command = validCommand(-1, 0, 0, 0);

        assertThatThrownBy(() -> leadImportFixtureService.seedFixture(command))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void seedFixture_whenCountsExceedTotal_shouldRejectWithImportRowValidationError() {
        LeadImportFixtureCommand command = validCommand(1, 1, 1, 0);

        assertThatThrownBy(() -> leadImportFixtureService.seedFixture(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
    }

    @Test
    void seedFixture_whenErrorRowHasNoDetails_shouldRejectWithValidationError() {
        LeadImportFixtureCommand command = new LeadImportFixtureCommand(
                TENANT_ID,
                ACTOR_ID,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                "fixture.csv",
                Map.of("columns", Map.of("phone", "Phone")),
                1,
                0,
                1,
                0,
                List.of(new LeadImportFixtureRowCommand(
                        1,
                        ImportRowStatus.ERROR,
                        Map.of("Phone", ""),
                        Map.of(),
                        List.of()
                ))
        );

        assertThatThrownBy(() -> leadImportFixtureService.seedFixture(command))
                .isInstanceOf(ValidationException.class);
    }

    private LeadImportFixtureCommand validCommand(int totalRows, int validRows, int errorRows, int duplicateRows) {
        return new LeadImportFixtureCommand(
                TENANT_ID,
                ACTOR_ID,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                "fixture.csv",
                Map.of("columns", Map.of("phone", "Phone")),
                totalRows,
                validRows,
                errorRows,
                duplicateRows,
                List.of(new LeadImportFixtureRowCommand(
                        1,
                        ImportRowStatus.VALID,
                        Map.of("Phone", "0900000001"),
                        Map.of("phone", "0900000001"),
                        List.of()
                ))
        );
    }

    private ImportBatch existingBatch() {
        return ImportBatch.restore(
                BATCH_ID,
                TENANT_ID,
                ImportType.LEAD,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                Map.of("columns", Map.of("phone", "Phone")),
                null,
                LeadImportFixtureService.DEFAULT_FIXTURE_FILE_NAME,
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
}
