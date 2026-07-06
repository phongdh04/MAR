package vn.mar.sla.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.time.TimeProvider;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.CompleteFirstResponseSlaTaskCommand;
import vn.mar.sla.api.DueTimeCalculationCommand;
import vn.mar.sla.api.DueTimeCalculationResult;
import vn.mar.sla.api.OpenFirstResponseSlaTaskCommand;
import vn.mar.sla.api.SlaOverdueScanSnapshot;
import vn.mar.sla.api.SlaPolicyLookupService;
import vn.mar.sla.api.SlaPolicySnapshot;
import vn.mar.sla.api.SlaTaskSearchCommand;
import vn.mar.sla.api.SlaTaskSnapshot;
import vn.mar.sla.entity.SlaTask;
import vn.mar.sla.mapper.SlaTaskMapper;
import vn.mar.sla.model.SlaOverdueLevel;
import vn.mar.sla.model.SlaTaskStatus;
import vn.mar.sla.model.SlaTaskType;
import vn.mar.sla.repository.SlaTaskRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith(MockitoExtension.class)
class SlaTaskServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID OWNER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID BRANCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SLA_POLICY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTIVITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final Instant DUE_AT = Instant.parse("2026-07-06T01:15:00Z");

    @Mock
    private SlaTaskRepository slaTaskRepository;

    @Mock
    private SlaPolicyLookupService slaPolicyLookupService;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private SlaTaskService slaTaskService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        slaTaskService = new SlaTaskService(
                slaTaskRepository,
                slaPolicyLookupService,
                branchRepository,
                userBranchRepository,
                currentUserContext,
                auditService,
                timeProvider,
                new SlaTaskMapper()
        );
    }

    @Test
    void openFirstResponseTask_whenNoExistingTask_shouldCreateTaskFromSlaPolicyDueTime() {
        when(slaPolicyLookupService.calculateFirstResponseDueAt(any(DueTimeCalculationCommand.class)))
                .thenReturn(dueTime());
        when(slaTaskRepository.findFirstByTenantIdAndOpportunityIdAndTaskTypeOrderByCreatedAtDescIdDesc(
                TENANT_ID,
                OPPORTUNITY_ID,
                SlaTaskType.FIRST_RESPONSE
        )).thenReturn(Optional.empty());
        when(slaTaskRepository.save(any(SlaTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SlaTaskSnapshot snapshot = slaTaskService.openFirstResponseTask(openCommand());

        assertThat(snapshot.opportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(snapshot.ownerId()).isEqualTo(OWNER_ID);
        assertThat(snapshot.taskType()).isEqualTo(SlaTaskType.FIRST_RESPONSE.name());
        assertThat(snapshot.status()).isEqualTo(SlaTaskStatus.OPEN.name());
        assertThat(snapshot.dueAt()).isEqualTo(DUE_AT);
        assertThat(snapshot.slaPolicyId()).isEqualTo(SLA_POLICY_ID);
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.SLA_TASK_CREATED);
    }

    @Test
    void completeFirstResponseTask_whenOpenTaskExists_shouldCompleteAndRecordSlaHit() {
        SlaTask task = openTask(DUE_AT);
        when(slaTaskRepository.findFirstByTenantIdAndOpportunityIdAndTaskTypeAndStatusInOrderByCreatedAtDescIdDesc(
                eq(TENANT_ID),
                eq(OPPORTUNITY_ID),
                eq(SlaTaskType.FIRST_RESPONSE),
                any()
        )).thenReturn(Optional.of(task));
        when(slaTaskRepository.save(any(SlaTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<SlaTaskSnapshot> completed = slaTaskService.completeFirstResponseTask(new CompleteFirstResponseSlaTaskCommand(
                TENANT_ID,
                OPPORTUNITY_ID,
                ACTIVITY_ID,
                NOW.plusSeconds(300),
                ACTOR_ID,
                "ADVISOR"
        ));

        assertThat(completed).isPresent();
        assertThat(completed.get().status()).isEqualTo(SlaTaskStatus.COMPLETED.name());
        assertThat(completed.get().completedActivityId()).isEqualTo(ACTIVITY_ID);
        assertThat(completed.get().slaHit()).isTrue();
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.SLA_TASK_COMPLETED);
    }

    @Test
    void completeFirstResponseTask_whenActivityAfterDueAt_shouldCompleteAndRecordSlaMiss() {
        SlaTask task = openTask(NOW.minusSeconds(60));
        when(slaTaskRepository.findFirstByTenantIdAndOpportunityIdAndTaskTypeAndStatusInOrderByCreatedAtDescIdDesc(
                eq(TENANT_ID),
                eq(OPPORTUNITY_ID),
                eq(SlaTaskType.FIRST_RESPONSE),
                any()
        )).thenReturn(Optional.of(task));
        when(slaTaskRepository.save(any(SlaTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<SlaTaskSnapshot> completed = slaTaskService.completeFirstResponseTask(new CompleteFirstResponseSlaTaskCommand(
                TENANT_ID,
                OPPORTUNITY_ID,
                ACTIVITY_ID,
                NOW,
                ACTOR_ID,
                "ADVISOR"
        ));

        assertThat(completed).isPresent();
        assertThat(completed.get().status()).isEqualTo(SlaTaskStatus.COMPLETED.name());
        assertThat(completed.get().slaHit()).isFalse();
        assertThat(completed.get().completedAt()).isEqualTo(NOW);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.SLA_TASK_COMPLETED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("First response SLA completed after due time");
    }

    @Test
    void scanOverdueTasks_whenOpenAndAdvisorOverdueTasksExist_shouldMarkAndEscalateIdempotently() {
        allowAdmin(PermissionCodes.SLA_TASK_MANAGE);
        SlaTask openDueTask = openTask(NOW.minusSeconds(60));
        SlaTask advisorOverdueTask = openTask(NOW.minusSeconds(1800));
        advisorOverdueTask.markOverdue(ACTOR_ID, NOW.minusSeconds(1200));
        when(slaTaskRepository.findAdvisorOverdueReadyForSalesLead(
                TENANT_ID,
                SlaTaskStatus.OVERDUE,
                SlaOverdueLevel.ADVISOR,
                NOW.minusSeconds(900)
        )).thenReturn(List.of(advisorOverdueTask));
        when(slaTaskRepository.findByTenantIdAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                TENANT_ID,
                SlaTaskStatus.OPEN,
                NOW
        )).thenReturn(List.of(openDueTask));
        when(slaTaskRepository.save(any(SlaTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SlaOverdueScanSnapshot snapshot = slaTaskService.scanOverdueTasks();

        assertThat(snapshot.markedOverdueCount()).isEqualTo(1);
        assertThat(snapshot.escalatedSalesLeadCount()).isEqualTo(1);
        assertThat(openDueTask.status()).isEqualTo(SlaTaskStatus.OVERDUE);
        assertThat(openDueTask.overdueLevel()).isEqualTo(SlaOverdueLevel.ADVISOR);
        assertThat(advisorOverdueTask.overdueLevel()).isEqualTo(SlaOverdueLevel.SALES_LEAD);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService, times(2)).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordCommand::action)
                .containsExactly(
                        AuditActions.SLA_TASK_ESCALATED_TO_SALES_LEAD,
                        AuditActions.SLA_TASK_OVERDUE_MARKED
                );
    }

    @Test
    void searchTasks_whenAdvisorRequestsOtherOwner_shouldReject() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                OWNER_ID,
                TENANT_ID,
                "ADVISOR",
                Set.of(PermissionCodes.SLA_TASK_VIEW),
                "req_sla_task_unit_001"
        ));

        assertThatThrownBy(() -> slaTaskService.searchTasks(new SlaTaskSearchCommand(
                OTHER_OWNER_ID,
                null,
                null,
                null,
                0,
                20
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    private void allowAdmin(String... permissions) {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(permissions),
                "req_sla_task_unit_001"
        ));
    }

    private OpenFirstResponseSlaTaskCommand openCommand() {
        return new OpenFirstResponseSlaTaskCommand(
                TENANT_ID,
                OPPORTUNITY_ID,
                LEAD_ID,
                OWNER_ID,
                BRANCH_ID,
                LeadTemperature.HOT,
                NOW,
                ACTOR_ID,
                "ADMIN"
        );
    }

    private DueTimeCalculationResult dueTime() {
        return new DueTimeCalculationResult(
                DUE_AT,
                new SlaPolicySnapshot(
                        SLA_POLICY_ID,
                        TENANT_ID,
                        BRANCH_ID,
                        "HOT",
                        15,
                        "Asia/Ho_Chi_Minh",
                        "TENANT"
                ),
                "TENANT",
                false
        );
    }

    private SlaTask openTask(Instant dueAt) {
        return SlaTask.openFirstResponse(
                UUID.randomUUID(),
                TENANT_ID,
                OPPORTUNITY_ID,
                LEAD_ID,
                OWNER_ID,
                BRANCH_ID,
                SLA_POLICY_ID,
                LeadTemperature.HOT,
                dueAt,
                ACTOR_ID,
                NOW.minusSeconds(1800)
        );
    }
}
