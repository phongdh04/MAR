package vn.mar.branch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import vn.mar.branch.dto.request.CreateBranchRequest;
import vn.mar.branch.dto.request.UpdateBranchRequest;
import vn.mar.branch.dto.response.BranchDetailResponse;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.mapper.BranchMapper;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BRANCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private BranchService branchService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        branchService = new BranchService(
                branchRepository,
                new BranchMapper(),
                timeProvider,
                currentUserContext,
                auditService
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of("branch.manage"),
                "req_branch_unit_001"
        ));
    }

    @Test
    void createBranch_whenValid_shouldDefaultActiveAndAuditCreated() {
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchDetailResponse response = branchService.createBranch(new CreateBranchRequest(
                null,
                "Ha Noi - Cau Giay",
                "Ha Noi",
                null,
                "Cau Giay",
                null
        ));

        assertThat(response.branchName()).isEqualTo("Ha Noi - Cau Giay");
        assertThat(response.city()).isEqualTo("Ha Noi");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.branchCode()).startsWith("HA_NOI_CAU_GIAY-");

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.BRANCH_CREATED);
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createBranch_whenActiveNameDuplicated_shouldRejectWithConflict() {
        when(branchRepository.existsByTenantIdAndBranchNameIgnoreCaseAndStatus(
                TENANT_ID,
                "Ha Noi - Cau Giay",
                BranchStatus.ACTIVE
        )).thenReturn(true);

        assertThatThrownBy(() -> branchService.createBranch(new CreateBranchRequest(
                null,
                "Ha Noi - Cau Giay",
                "Ha Noi",
                null,
                null,
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_ACTIVE_BRANCH);
    }

    @Test
    void getBranch_whenBranchBelongsToAnotherTenant_shouldThrowNotFound() {
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.getBranch(BRANCH_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(branchRepository).findByIdAndTenantId(BRANCH_ID, TENANT_ID);
        verify(branchRepository, never()).findById(BRANCH_ID);
    }

    @Test
    void updateBranch_whenStatusChangedToInactive_shouldAuditStatusChanged() {
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(activeBranch()));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchDetailResponse response = branchService.updateBranch(BRANCH_ID, new UpdateBranchRequest(
                null,
                null,
                null,
                null,
                "INACTIVE",
                "Close pilot location"
        ));

        assertThat(response.status()).isEqualTo("INACTIVE");
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.BRANCH_STATUS_CHANGED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Close pilot location");
    }

    @Test
    void updateBranch_whenStatusInvalid_shouldRejectWithValidationError() {
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(activeBranch()));

        assertThatThrownBy(() -> branchService.updateBranch(BRANCH_ID, new UpdateBranchRequest(
                null,
                null,
                null,
                null,
                "DELETED",
                null
        )))
                .isInstanceOf(ValidationException.class);
    }

    private Branch activeBranch() {
        return Branch.restore(
                BRANCH_ID,
                TENANT_ID,
                "HN01",
                "Ha Noi - Cau Giay",
                "Ha Noi",
                null,
                "Cau Giay",
                BranchStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
