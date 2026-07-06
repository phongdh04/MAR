package vn.mar.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.assignment.api.AssignOpportunityCommand;
import vn.mar.assignment.api.AssignmentResultSnapshot;
import vn.mar.assignment.entity.AssignmentHistory;
import vn.mar.assignment.entity.AssignmentPoolState;
import vn.mar.assignment.entity.AssignmentRule;
import vn.mar.assignment.entity.AssignmentRuleAdvisor;
import vn.mar.assignment.entity.UnassignedAssignmentItem;
import vn.mar.assignment.model.AssignmentOutcome;
import vn.mar.assignment.model.AssignmentRuleStatus;
import vn.mar.assignment.model.AssignmentStrategy;
import vn.mar.assignment.model.UnassignedAssignmentItemStatus;
import vn.mar.assignment.model.UnassignedReasonCode;
import vn.mar.assignment.repository.AssignmentHistoryRepository;
import vn.mar.assignment.repository.AssignmentPoolStateRepository;
import vn.mar.assignment.repository.AssignmentRuleAdvisorRepository;
import vn.mar.assignment.repository.AssignmentRuleRepository;
import vn.mar.assignment.repository.UnassignedAssignmentItemRepository;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.common.time.TimeProvider;
import vn.mar.opportunity.api.AssignOpportunityOwnerCommand;
import vn.mar.opportunity.api.OpportunityAssignmentService;
import vn.mar.opportunity.api.OpportunityAssignmentSnapshot;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.OpenFirstResponseSlaTaskCommand;
import vn.mar.sla.api.SlaTaskManagementService;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith(MockitoExtension.class)
class DefaultAssignmentEngineServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID LANGUAGE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BRANCH_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RULE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ADVISOR_A_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ADVISOR_B_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private AssignmentRuleRepository assignmentRuleRepository;

    @Mock
    private AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository;

    @Mock
    private AssignmentPoolStateRepository assignmentPoolStateRepository;

    @Mock
    private AssignmentHistoryRepository assignmentHistoryRepository;

    @Mock
    private UnassignedAssignmentItemRepository unassignedAssignmentItemRepository;

    @Mock
    private OpportunityAssignmentService opportunityAssignmentService;

    @Mock
    private SlaTaskManagementService slaTaskManagementService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private DefaultAssignmentEngineService assignmentEngineService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        assignmentEngineService = new DefaultAssignmentEngineService(
                assignmentRuleRepository,
                assignmentRuleAdvisorRepository,
                assignmentPoolStateRepository,
                assignmentHistoryRepository,
                unassignedAssignmentItemRepository,
                opportunityAssignmentService,
                slaTaskManagementService,
                userRepository,
                userBranchRepository,
                currentUserContext,
                auditService,
                timeProvider
        );
    }

    @Test
    void assignOpportunity_whenRuleMatchesLanguageProgramBranch_shouldUseLeastWorkloadAdvisor() {
        allowAssignmentManage();
        OpportunityAssignmentSnapshot opportunity = opportunity(null, BRANCH_ID);
        AssignmentRule rule = assignmentRule(RULE_ID, LANGUAGE_ID, PROGRAM_ID, BRANCH_ID, AssignmentStrategy.LEAST_WORKLOAD);
        stubOpportunity(opportunity);
        when(assignmentRuleRepository.findByTenantIdAndStatusOrderByPriorityAscIdAsc(TENANT_ID, AssignmentRuleStatus.ACTIVE))
                .thenReturn(List.of(rule));
        when(assignmentRuleAdvisorRepository.findByTenantIdAndAssignmentRuleIdAndStatus(eq(TENANT_ID), eq(RULE_ID), any()))
                .thenReturn(List.of(ruleAdvisor(ADVISOR_A_ID), ruleAdvisor(ADVISOR_B_ID)));
        when(userRepository.findByTenantIdAndIdInAndStatusAndRoleCode(eq(TENANT_ID), any(), eq(UserStatus.ACTIVE), eq("ADVISOR")))
                .thenReturn(List.of(advisor(ADVISOR_A_ID), advisor(ADVISOR_B_ID)));
        when(userBranchRepository.findByTenantIdAndUserIdInAndStatus(eq(TENANT_ID), any(), eq(UserBranchStatus.ACTIVE)))
                .thenReturn(List.of(userBranch(ADVISOR_A_ID), userBranch(ADVISOR_B_ID)));
        when(opportunityAssignmentService.countActiveOwnedOpportunities(TENANT_ID, List.of(ADVISOR_A_ID, ADVISOR_B_ID)))
                .thenReturn(Map.of(ADVISOR_A_ID, 5L, ADVISOR_B_ID, 1L));
        when(assignmentPoolStateRepository.findForUpdate(TENANT_ID, RULE_ID)).thenReturn(Optional.empty());
        when(assignmentPoolStateRepository.save(any(AssignmentPoolState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityAssignmentService.assignOwner(any(AssignOpportunityOwnerCommand.class)))
                .thenReturn(opportunity(ADVISOR_B_ID, BRANCH_ID));
        when(assignmentHistoryRepository.save(any(AssignmentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResultSnapshot result = assignmentEngineService.assignOpportunity(command());

        assertThat(result.outcome()).isEqualTo(AssignmentOutcome.ASSIGNED.name());
        assertThat(result.assignedOwnerId()).isEqualTo(ADVISOR_B_ID);
        assertThat(result.assignmentRuleId()).isEqualTo(RULE_ID);
        assertThat(result.assignmentStrategy()).isEqualTo(AssignmentStrategy.LEAST_WORKLOAD.name());
        assertThat(result.fallbackApplied()).isFalse();
        ArgumentCaptor<AssignOpportunityOwnerCommand> commandCaptor = ArgumentCaptor.forClass(AssignOpportunityOwnerCommand.class);
        verify(opportunityAssignmentService).assignOwner(commandCaptor.capture());
        assertThat(commandCaptor.getValue().ownerId()).isEqualTo(ADVISOR_B_ID);
        assertThat(commandCaptor.getValue().assignmentRuleId()).isEqualTo(RULE_ID);
        verify(assignmentHistoryRepository).save(any(AssignmentHistory.class));
        ArgumentCaptor<OpenFirstResponseSlaTaskCommand> slaCommandCaptor =
                ArgumentCaptor.forClass(OpenFirstResponseSlaTaskCommand.class);
        verify(slaTaskManagementService).openFirstResponseTask(slaCommandCaptor.capture());
        assertThat(slaCommandCaptor.getValue().opportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(slaCommandCaptor.getValue().ownerId()).isEqualTo(ADVISOR_B_ID);
        assertThat(slaCommandCaptor.getValue().leadTemperature().name()).isEqualTo("HOT");
    }

    @Test
    void assignOpportunity_whenNoRuleMatches_shouldUseFallbackPool() {
        allowAssignmentManage();
        OpportunityAssignmentSnapshot opportunity = opportunity(null, null);
        stubOpportunity(opportunity);
        when(assignmentRuleRepository.findByTenantIdAndStatusOrderByPriorityAscIdAsc(TENANT_ID, AssignmentRuleStatus.ACTIVE))
                .thenReturn(List.of());
        when(userRepository.findByTenantIdAndStatusAndRoleCode(TENANT_ID, UserStatus.ACTIVE, "ADVISOR"))
                .thenReturn(List.of(advisor(ADVISOR_A_ID), advisor(ADVISOR_B_ID)));
        when(userRepository.findByTenantIdAndIdInAndStatusAndRoleCode(eq(TENANT_ID), any(), eq(UserStatus.ACTIVE), eq("ADVISOR")))
                .thenReturn(List.of(advisor(ADVISOR_A_ID), advisor(ADVISOR_B_ID)));
        when(opportunityAssignmentService.countActiveOwnedOpportunities(TENANT_ID, List.of(ADVISOR_A_ID, ADVISOR_B_ID)))
                .thenReturn(Map.of(ADVISOR_A_ID, 0L, ADVISOR_B_ID, 0L));
        when(assignmentPoolStateRepository.findForUpdate(TENANT_ID, null)).thenReturn(Optional.empty());
        when(assignmentPoolStateRepository.save(any(AssignmentPoolState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityAssignmentService.assignOwner(any(AssignOpportunityOwnerCommand.class)))
                .thenReturn(opportunity(ADVISOR_A_ID, null));
        when(assignmentHistoryRepository.save(any(AssignmentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResultSnapshot result = assignmentEngineService.assignOpportunity(command());

        assertThat(result.outcome()).isEqualTo(AssignmentOutcome.ASSIGNED.name());
        assertThat(result.assignedOwnerId()).isEqualTo(ADVISOR_A_ID);
        assertThat(result.assignmentRuleId()).isNull();
        assertThat(result.fallbackApplied()).isTrue();
        verify(slaTaskManagementService).openFirstResponseTask(any(OpenFirstResponseSlaTaskCommand.class));
    }

    @Test
    void assignOpportunity_whenMatchedRuleHasNoActiveAdvisor_shouldCreateUnassignedItem() {
        allowAssignmentManage();
        OpportunityAssignmentSnapshot opportunity = opportunity(null, BRANCH_ID);
        AssignmentRule rule = assignmentRule(RULE_ID, LANGUAGE_ID, PROGRAM_ID, BRANCH_ID, AssignmentStrategy.ROUND_ROBIN);
        stubOpportunity(opportunity);
        when(assignmentRuleRepository.findByTenantIdAndStatusOrderByPriorityAscIdAsc(TENANT_ID, AssignmentRuleStatus.ACTIVE))
                .thenReturn(List.of(rule));
        when(assignmentRuleAdvisorRepository.findByTenantIdAndAssignmentRuleIdAndStatus(eq(TENANT_ID), eq(RULE_ID), any()))
                .thenReturn(List.of());
        when(unassignedAssignmentItemRepository.findByTenantIdAndOpportunityIdAndStatus(
                TENANT_ID,
                OPPORTUNITY_ID,
                UnassignedAssignmentItemStatus.OPEN
        )).thenReturn(Optional.empty());
        when(unassignedAssignmentItemRepository.save(any(UnassignedAssignmentItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentResultSnapshot result = assignmentEngineService.assignOpportunity(command());

        assertThat(result.outcome()).isEqualTo(AssignmentOutcome.UNASSIGNED.name());
        assertThat(result.assignmentRuleId()).isEqualTo(RULE_ID);
        assertThat(result.unassignedReasonCode()).isEqualTo(UnassignedReasonCode.NO_ACTIVE_ADVISOR.name());
        assertThat(result.unassignedItemId()).isNotNull();
        verify(opportunityAssignmentService, never()).assignOwner(any());
        verify(slaTaskManagementService, never()).openFirstResponseTask(any());
    }

    private void allowAssignmentManage() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(PermissionCodes.ASSIGNMENT_MANAGE),
                "req_assignment_unit_001"
        ));
    }

    private void stubOpportunity(OpportunityAssignmentSnapshot opportunity) {
        when(opportunityAssignmentService.getAssignmentSnapshot(TENANT_ID, OPPORTUNITY_ID)).thenReturn(opportunity);
    }

    private AssignOpportunityCommand command() {
        return new AssignOpportunityCommand(TENANT_ID, OPPORTUNITY_ID, ACTOR_ID, "Unit test assignment");
    }

    private OpportunityAssignmentSnapshot opportunity(UUID ownerId, UUID branchId) {
        return new OpportunityAssignmentSnapshot(
                OPPORTUNITY_ID,
                TENANT_ID,
                LEAD_ID,
                LANGUAGE_ID,
                PROGRAM_ID,
                branchId,
                ownerId,
                "NEW",
                "HOT",
                NOW,
                NOW
        );
    }

    private AssignmentRule assignmentRule(
            UUID ruleId,
            UUID languageId,
            UUID programId,
            UUID branchId,
            AssignmentStrategy strategy) {
        return AssignmentRule.create(
                ruleId,
                TENANT_ID,
                "HN English admission",
                10,
                languageId,
                programId,
                branchId,
                strategy,
                AssignmentRuleStatus.ACTIVE,
                ACTOR_ID,
                NOW
        );
    }

    private AssignmentRuleAdvisor ruleAdvisor(UUID advisorId) {
        return AssignmentRuleAdvisor.create(UUID.randomUUID(), TENANT_ID, RULE_ID, advisorId, ACTOR_ID, NOW);
    }

    private User advisor(UUID advisorId) {
        return User.create(
                advisorId,
                TENANT_ID,
                advisorId + "@mar.test",
                "Advisor " + advisorId,
                null,
                "ADVISOR",
                UserStatus.ACTIVE,
                ACTOR_ID,
                NOW
        );
    }

    private UserBranch userBranch(UUID advisorId) {
        return UserBranch.create(UUID.randomUUID(), TENANT_ID, advisorId, BRANCH_ID, ACTOR_ID, NOW);
    }
}
