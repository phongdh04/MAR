package vn.mar.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import vn.mar.authz.model.PermissionCodes;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.customer.api.MergeHistorySnapshot;
import vn.mar.customer.api.UnmergeCustomerCommand;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.entity.MergeHistory;
import vn.mar.customer.mapper.MergeHistoryMapper;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.model.DuplicateResolutionAction;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.customer.repository.MergeHistoryRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class CustomerMergeServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SOURCE_CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID TARGET_CUSTOMER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID DUPLICATE_CASE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID MERGE_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-07-03T02:00:00Z");

    @Mock
    private MergeHistoryRepository mergeHistoryRepository;

    @Mock
    private DuplicateCaseRepository duplicateCaseRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private CustomerMergeService customerMergeService;

    @BeforeEach
    void setUp() {
        customerMergeService = new CustomerMergeService(
                mergeHistoryRepository,
                duplicateCaseRepository,
                new MergeHistoryMapper(),
                () -> NOW,
                currentUserContext,
                auditService
        );
    }

    @Test
    void recordMerge_whenValid_shouldSaveMergeHistoryAndAuditCustomerMerged() {
        when(mergeHistoryRepository.save(any(MergeHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MergeHistorySnapshot result = customerMergeService.recordMerge(
                duplicateCase(DuplicateCaseStatus.MERGED),
                sourceCustomer(),
                targetCustomer(),
                adminUser(),
                "Confirmed same learner"
        );

        assertThat(result.sourceCustomerId()).isEqualTo(SOURCE_CUSTOMER_ID);
        assertThat(result.targetCustomerId()).isEqualTo(TARGET_CUSTOMER_ID);
        assertThat(result.duplicateCaseId()).isEqualTo(DUPLICATE_CASE_ID);
        assertThat(result.canUnmerge()).isTrue();

        ArgumentCaptor<MergeHistory> mergeHistoryCaptor = ArgumentCaptor.forClass(MergeHistory.class);
        verify(mergeHistoryRepository).save(mergeHistoryCaptor.capture());
        assertThat(mergeHistoryCaptor.getValue().mergeSnapshot())
                .containsKeys("duplicate_case", "source_customer", "target_customer");

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.CUSTOMER_MERGED);
        assertThat(auditCaptor.getValue().resourceId()).isEqualTo(TARGET_CUSTOMER_ID);
    }

    @Test
    void unmerge_whenAdminAllowed_shouldMarkMergeHistoryAndDuplicateCaseThenAudit() {
        mockCurrentUser("ADMIN", PermissionCodes.CUSTOMER_MERGE);
        MergeHistory mergeHistory = mergeHistory(true, null);
        DuplicateCase duplicateCase = duplicateCase(DuplicateCaseStatus.MERGED);
        when(mergeHistoryRepository.findByIdAndTenantIdAndTargetCustomerId(MERGE_ID, TENANT_ID, TARGET_CUSTOMER_ID))
                .thenReturn(Optional.of(mergeHistory));
        when(mergeHistoryRepository.save(any(MergeHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.of(duplicateCase));

        MergeHistorySnapshot result = customerMergeService.unmerge(new UnmergeCustomerCommand(
                TARGET_CUSTOMER_ID,
                MERGE_ID,
                "Merged wrong guardian account"
        ));

        assertThat(result.unmergedBy()).isEqualTo(ACTOR_ID);
        assertThat(result.unmergedAt()).isEqualTo(NOW);
        assertThat(duplicateCase.status()).isEqualTo(DuplicateCaseStatus.UNMERGED);
        verify(duplicateCaseRepository).save(duplicateCase);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.CUSTOMER_UNMERGED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Merged wrong guardian account");
    }

    @Test
    void unmerge_whenCannotUnmerge_shouldReturnUnmergeNotAllowedAndNotSave() {
        mockCurrentUser("ADMIN", PermissionCodes.CUSTOMER_MERGE);
        when(mergeHistoryRepository.findByIdAndTenantIdAndTargetCustomerId(MERGE_ID, TENANT_ID, TARGET_CUSTOMER_ID))
                .thenReturn(Optional.of(mergeHistory(false, null)));

        assertThatThrownBy(() -> customerMergeService.unmerge(new UnmergeCustomerCommand(
                TARGET_CUSTOMER_ID,
                MERGE_ID,
                "Merged wrong account"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNMERGE_NOT_ALLOWED);

        verify(mergeHistoryRepository, never()).save(any(MergeHistory.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void unmerge_whenAlreadyUnmerged_shouldReturnUnmergeNotAllowed() {
        mockCurrentUser("ADMIN", PermissionCodes.CUSTOMER_MERGE);
        when(mergeHistoryRepository.findByIdAndTenantIdAndTargetCustomerId(MERGE_ID, TENANT_ID, TARGET_CUSTOMER_ID))
                .thenReturn(Optional.of(mergeHistory(true, NOW)));

        assertThatThrownBy(() -> customerMergeService.unmerge(new UnmergeCustomerCommand(
                TARGET_CUSTOMER_ID,
                MERGE_ID,
                "Try again"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNMERGE_NOT_ALLOWED);
    }

    @Test
    void unmerge_whenSalesLeadHasPermission_shouldStillRequireAdminRole() {
        mockCurrentUser("SALES_LEAD", PermissionCodes.CUSTOMER_MERGE);

        assertThatThrownBy(() -> customerMergeService.unmerge(new UnmergeCustomerCommand(
                TARGET_CUSTOMER_ID,
                MERGE_ID,
                "Wrong merge"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);

        verify(mergeHistoryRepository, never()).findByIdAndTenantId(any(), any());
    }

    private void mockCurrentUser(String roleCode, String... permissionCodes) {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                roleCode,
                Set.of(permissionCodes),
                "req-test"
        ));
    }

    private CurrentUser adminUser() {
        return new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(PermissionCodes.CUSTOMER_MERGE),
                "req-test"
        );
    }

    private MergeHistory mergeHistory(boolean canUnmerge, Instant unmergedAt) {
        return MergeHistory.restore(
                MERGE_ID,
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                TARGET_CUSTOMER_ID,
                DUPLICATE_CASE_ID,
                ACTOR_ID,
                NOW.minusSeconds(3600),
                "Confirmed same learner",
                Map.of("source_customer_id", SOURCE_CUSTOMER_ID.toString()),
                canUnmerge,
                unmergedAt == null ? null : ACTOR_ID,
                unmergedAt
        );
    }

    private DuplicateCase duplicateCase(DuplicateCaseStatus status) {
        return DuplicateCase.restore(
                DUPLICATE_CASE_ID,
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                TARGET_CUSTOMER_ID,
                DuplicateMatchType.NEAR_MATCH,
                DuplicateConfidence.LOW,
                status,
                "Similar name",
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : DuplicateResolutionAction.MERGE,
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : ACTOR_ID,
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : NOW.minusSeconds(3600),
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : "Confirmed",
                NOW.minusSeconds(7200),
                NOW.minusSeconds(3600)
        );
    }

    private CustomerProfile sourceCustomer() {
        return customer(SOURCE_CUSTOMER_ID, "0900000001", "student@example.com");
    }

    private CustomerProfile targetCustomer() {
        return customer(TARGET_CUSTOMER_ID, "0900000002", "student@example.com");
    }

    private CustomerProfile customer(UUID customerId, String phone, String email) {
        return CustomerProfile.restore(
                customerId,
                TENANT_ID,
                "Student",
                phone,
                email,
                null,
                null,
                null,
                null,
                NOW.minusSeconds(7200),
                NOW.minusSeconds(3600)
        );
    }
}
