package vn.mar.assignment.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mar.assignment.api.AssignOpportunityCommand;
import vn.mar.assignment.api.AssignmentEngineService;
import vn.mar.assignment.api.AssignmentResultSnapshot;
import vn.mar.assignment.entity.AssignmentHistory;
import vn.mar.assignment.entity.AssignmentPoolState;
import vn.mar.assignment.entity.AssignmentRule;
import vn.mar.assignment.entity.AssignmentRuleAdvisor;
import vn.mar.assignment.entity.UnassignedAssignmentItem;
import vn.mar.assignment.model.AssignmentOutcome;
import vn.mar.assignment.model.AssignmentRuleAdvisorStatus;
import vn.mar.assignment.model.AssignmentRuleStatus;
import vn.mar.assignment.model.AssignmentSource;
import vn.mar.assignment.model.AssignmentStrategy;
import vn.mar.assignment.model.UnassignedAssignmentItemStatus;
import vn.mar.assignment.model.UnassignedReasonCode;
import vn.mar.assignment.repository.AssignmentHistoryRepository;
import vn.mar.assignment.repository.AssignmentPoolStateRepository;
import vn.mar.assignment.repository.AssignmentRuleAdvisorRepository;
import vn.mar.assignment.repository.AssignmentRuleRepository;
import vn.mar.assignment.repository.UnassignedAssignmentItemRepository;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.lead.model.LeadTemperature;
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

@Service
public class DefaultAssignmentEngineService implements AssignmentEngineService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAssignmentEngineService.class);
    private static final String ROLE_ADVISOR = "ADVISOR";
    private static final String ROLE_SALES_LEAD = "SALES_LEAD";

    private final AssignmentRuleRepository assignmentRuleRepository;
    private final AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository;
    private final AssignmentPoolStateRepository assignmentPoolStateRepository;
    private final AssignmentHistoryRepository assignmentHistoryRepository;
    private final UnassignedAssignmentItemRepository unassignedAssignmentItemRepository;
    private final OpportunityAssignmentService opportunityAssignmentService;
    private final SlaTaskManagementService slaTaskManagementService;
    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public DefaultAssignmentEngineService(
            AssignmentRuleRepository assignmentRuleRepository,
            AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository,
            AssignmentPoolStateRepository assignmentPoolStateRepository,
            AssignmentHistoryRepository assignmentHistoryRepository,
            UnassignedAssignmentItemRepository unassignedAssignmentItemRepository,
            OpportunityAssignmentService opportunityAssignmentService,
            SlaTaskManagementService slaTaskManagementService,
            UserRepository userRepository,
            UserBranchRepository userBranchRepository,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            TimeProvider timeProvider) {
        this.assignmentRuleRepository = assignmentRuleRepository;
        this.assignmentRuleAdvisorRepository = assignmentRuleAdvisorRepository;
        this.assignmentPoolStateRepository = assignmentPoolStateRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.unassignedAssignmentItemRepository = unassignedAssignmentItemRepository;
        this.opportunityAssignmentService = opportunityAssignmentService;
        this.slaTaskManagementService = slaTaskManagementService;
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public AssignmentResultSnapshot assignOpportunity(AssignOpportunityCommand command) {
        validateCommand(command);
        CurrentUser actor = currentUserOrSystem(command.requestedBy(), command.tenantId());
        assertCanTriggerAssignment(actor);
        UUID tenantId = resolveTenantId(actor, command.tenantId());
        OpportunityAssignmentSnapshot opportunity = opportunityAssignmentService.getAssignmentSnapshot(tenantId, command.opportunityId());
        assertBranchScope(actor, opportunity.branchId());

        AssignmentRule matchedRule = findMatchingRule(opportunity);
        if (matchedRule != null) {
            List<UUID> ruleCandidates = activeAdvisorIdsForRule(matchedRule, opportunity.branchId());
            if (ruleCandidates.isEmpty()) {
                return createUnassignedResult(
                        actor,
                        opportunity,
                        matchedRule.id(),
                        UnassignedReasonCode.NO_ACTIVE_ADVISOR
                );
            }
            return assignToCandidate(
                    actor,
                    opportunity,
                    matchedRule.id(),
                    matchedRule.assignmentStrategy(),
                    AssignmentSource.RULE,
                    false,
                    ruleCandidates,
                    command.reason()
            );
        }

        List<UUID> fallbackCandidates = activeFallbackAdvisorIds(tenantId, opportunity.branchId());
        if (fallbackCandidates.isEmpty()) {
            return createUnassignedResult(
                    actor,
                    opportunity,
                    null,
                    UnassignedReasonCode.NO_RULE_MATCH_NO_FALLBACK_ADVISOR
            );
        }
        return assignToCandidate(
                actor,
                opportunity,
                null,
                AssignmentStrategy.LEAST_WORKLOAD,
                AssignmentSource.FALLBACK,
                true,
                fallbackCandidates,
                command.reason()
        );
    }

    private AssignmentResultSnapshot assignToCandidate(
            CurrentUser actor,
            OpportunityAssignmentSnapshot opportunity,
            UUID assignmentRuleId,
            AssignmentStrategy strategy,
            AssignmentSource source,
            boolean fallbackApplied,
            List<UUID> candidateIds,
            String reason) {
        Instant now = timeProvider.now();
        UUID selectedAdvisorId = chooseAdvisor(
                opportunity.tenantId(),
                assignmentRuleId,
                strategy,
                candidateIds,
                actor,
                now
        );
        OpportunityAssignmentSnapshot assignedOpportunity = opportunityAssignmentService.assignOwner(
                new AssignOpportunityOwnerCommand(
                        opportunity.tenantId(),
                        opportunity.opportunityId(),
                        selectedAdvisorId,
                        actor.actorId(),
                        assignmentRuleId,
                        source.name(),
                        strategy.name(),
                        normalizeReason(reason, source, assignmentRuleId)
                )
        );
        assignmentHistoryRepository.save(AssignmentHistory.create(
                UUID.randomUUID(),
                opportunity.tenantId(),
                opportunity.opportunityId(),
                opportunity.sourceLeadId(),
                assignmentRuleId,
                opportunity.ownerId(),
                selectedAdvisorId,
                source,
                strategy,
                now,
                actor.actorId(),
                normalizeReason(reason, source, assignmentRuleId)
        ));
        slaTaskManagementService.openFirstResponseTask(new OpenFirstResponseSlaTaskCommand(
                assignedOpportunity.tenantId(),
                assignedOpportunity.opportunityId(),
                assignedOpportunity.sourceLeadId(),
                selectedAdvisorId,
                assignedOpportunity.branchId(),
                resolveLeadTemperature(assignedOpportunity.leadTemperature()),
                now,
                actor.actorId(),
                actor.roleCode()
        ));
        resolveOpenUnassignedItem(actor, opportunity, now);
        return new AssignmentResultSnapshot(
                assignedOpportunity.opportunityId(),
                AssignmentOutcome.ASSIGNED.name(),
                selectedAdvisorId,
                assignmentRuleId,
                strategy.name(),
                fallbackApplied,
                null,
                null
        );
    }

    private AssignmentResultSnapshot createUnassignedResult(
            CurrentUser actor,
            OpportunityAssignmentSnapshot opportunity,
            UUID assignmentRuleId,
            UnassignedReasonCode reasonCode) {
        Instant now = timeProvider.now();
        UnassignedAssignmentItem item = unassignedAssignmentItemRepository
                .findByTenantIdAndOpportunityIdAndStatus(
                        opportunity.tenantId(),
                        opportunity.opportunityId(),
                        UnassignedAssignmentItemStatus.OPEN
                )
                .orElseGet(() -> createUnassignedItem(actor, opportunity, assignmentRuleId, reasonCode, now));
        log.warn(
                "assignment created unassigned item tenantId={} opportunityId={} assignmentRuleId={} reasonCode={}",
                opportunity.tenantId(),
                opportunity.opportunityId(),
                assignmentRuleId,
                reasonCode
        );
        return new AssignmentResultSnapshot(
                opportunity.opportunityId(),
                AssignmentOutcome.UNASSIGNED.name(),
                null,
                assignmentRuleId,
                null,
                assignmentRuleId == null,
                item.id(),
                item.reasonCode().name()
        );
    }

    private UnassignedAssignmentItem createUnassignedItem(
            CurrentUser actor,
            OpportunityAssignmentSnapshot opportunity,
            UUID assignmentRuleId,
            UnassignedReasonCode reasonCode,
            Instant now) {
        UnassignedAssignmentItem item = UnassignedAssignmentItem.create(
                UUID.randomUUID(),
                opportunity.tenantId(),
                opportunity.opportunityId(),
                opportunity.sourceLeadId(),
                assignmentRuleId,
                reasonCode,
                actor.actorId(),
                now
        );
        UnassignedAssignmentItem savedItem = unassignedAssignmentItemRepository.save(item);
        auditUnassignedItemCreated(actor, savedItem);
        return savedItem;
    }

    private AssignmentRule findMatchingRule(OpportunityAssignmentSnapshot opportunity) {
        return assignmentRuleRepository
                .findByTenantIdAndStatusOrderByPriorityAscIdAsc(opportunity.tenantId(), AssignmentRuleStatus.ACTIVE)
                .stream()
                .filter(rule -> rule.matches(opportunity.languageId(), opportunity.programId(), opportunity.branchId()))
                .findFirst()
                .orElse(null);
    }

    private List<UUID> activeAdvisorIdsForRule(AssignmentRule rule, UUID opportunityBranchId) {
        List<UUID> advisorIds = assignmentRuleAdvisorRepository
                .findByTenantIdAndAssignmentRuleIdAndStatus(rule.tenantId(), rule.id(), AssignmentRuleAdvisorStatus.ACTIVE)
                .stream()
                .map(AssignmentRuleAdvisor::advisorId)
                .toList();
        return activeAdvisorIds(rule.tenantId(), advisorIds, opportunityBranchId);
    }

    private List<UUID> activeFallbackAdvisorIds(UUID tenantId, UUID opportunityBranchId) {
        List<UUID> advisorIds = userRepository.findByTenantIdAndStatusAndRoleCode(tenantId, UserStatus.ACTIVE, ROLE_ADVISOR)
                .stream()
                .map(User::id)
                .toList();
        return activeAdvisorIds(tenantId, advisorIds, opportunityBranchId);
    }

    private List<UUID> activeAdvisorIds(UUID tenantId, List<UUID> advisorIds, UUID opportunityBranchId) {
        if (advisorIds.isEmpty()) {
            return List.of();
        }
        List<UUID> activeAdvisorIds = userRepository.findByTenantIdAndIdInAndStatusAndRoleCode(
                        tenantId,
                        advisorIds,
                        UserStatus.ACTIVE,
                        ROLE_ADVISOR
                )
                .stream()
                .map(User::id)
                .toList();
        if (activeAdvisorIds.isEmpty()) {
            return List.of();
        }
        if (opportunityBranchId == null) {
            return activeAdvisorIds.stream().sorted().toList();
        }
        Set<UUID> branchAdvisorIds = userBranchRepository
                .findByTenantIdAndUserIdInAndStatus(tenantId, activeAdvisorIds, UserBranchStatus.ACTIVE)
                .stream()
                .filter(userBranch -> opportunityBranchId.equals(userBranch.branchId()))
                .map(UserBranch::userId)
                .collect(Collectors.toSet());
        return activeAdvisorIds.stream()
                .filter(branchAdvisorIds::contains)
                .sorted()
                .toList();
    }

    private UUID chooseAdvisor(
            UUID tenantId,
            UUID assignmentRuleId,
            AssignmentStrategy strategy,
            List<UUID> candidateIds,
            CurrentUser actor,
            Instant now) {
        List<UUID> orderedCandidates = candidateIds.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (strategy == AssignmentStrategy.LEAST_WORKLOAD) {
            Map<UUID, Long> workloads = opportunityAssignmentService.countActiveOwnedOpportunities(tenantId, orderedCandidates);
            long minWorkload = orderedCandidates.stream()
                    .map(advisorId -> workloads.getOrDefault(advisorId, 0L))
                    .min(Long::compareTo)
                    .orElse(0L);
            List<UUID> tiedCandidates = orderedCandidates.stream()
                    .filter(advisorId -> workloads.getOrDefault(advisorId, 0L) == minWorkload)
                    .toList();
            return pickNextRoundRobin(tenantId, assignmentRuleId, tiedCandidates, actor, now);
        }
        return pickNextRoundRobin(tenantId, assignmentRuleId, orderedCandidates, actor, now);
    }

    private UUID pickNextRoundRobin(
            UUID tenantId,
            UUID assignmentRuleId,
            List<UUID> candidateIds,
            CurrentUser actor,
            Instant now) {
        AssignmentPoolState state = assignmentPoolStateRepository
                .findForUpdate(tenantId, assignmentRuleId)
                .orElseGet(() -> AssignmentPoolState.create(
                        UUID.randomUUID(),
                        tenantId,
                        assignmentRuleId,
                        actor.actorId(),
                        now
                ));
        UUID selectedAdvisorId = nextAdvisorId(state.lastAssignedAdvisorId(), candidateIds);
        state.markAssigned(selectedAdvisorId, actor.actorId(), now);
        assignmentPoolStateRepository.save(state);
        return selectedAdvisorId;
    }

    private UUID nextAdvisorId(UUID lastAssignedAdvisorId, List<UUID> candidateIds) {
        if (lastAssignedAdvisorId == null) {
            return candidateIds.get(0);
        }
        int lastIndex = candidateIds.indexOf(lastAssignedAdvisorId);
        if (lastIndex < 0 || lastIndex + 1 >= candidateIds.size()) {
            return candidateIds.get(0);
        }
        return candidateIds.get(lastIndex + 1);
    }

    private void resolveOpenUnassignedItem(CurrentUser actor, OpportunityAssignmentSnapshot opportunity, Instant now) {
        unassignedAssignmentItemRepository
                .findByTenantIdAndOpportunityIdAndStatus(
                        opportunity.tenantId(),
                        opportunity.opportunityId(),
                        UnassignedAssignmentItemStatus.OPEN
                )
                .ifPresent(item -> {
                    item.resolve(actor.actorId(), now);
                    unassignedAssignmentItemRepository.save(item);
                });
    }

    private void validateCommand(AssignOpportunityCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Assignment command is required");
        }
        if (command.opportunityId() == null) {
            throw validation("opportunity_id", "REQUIRED", "Opportunity id is required");
        }
    }

    private void assertCanTriggerAssignment(CurrentUser actor) {
        if ("SYSTEM".equals(actor.roleCode())) {
            return;
        }
        if (!actor.hasPermission(PermissionCodes.ASSIGNMENT_MANAGE)) {
            throw forbidden("permission", "ASSIGNMENT_MANAGE_DENIED", "Permission is required to trigger assignment");
        }
    }

    private void assertBranchScope(CurrentUser actor, UUID branchId) {
        if ("SYSTEM".equals(actor.roleCode()) || !ROLE_SALES_LEAD.equals(actor.roleCode())) {
            return;
        }
        if (branchId == null) {
            throw forbidden("branch_id", "BRANCH_SCOPE_REQUIRED", "Sales Lead can only assign branch opportunities");
        }
        boolean assigned = userBranchRepository
                .findByTenantIdAndUserIdAndStatus(actor.tenantId(), actor.actorId(), UserBranchStatus.ACTIVE)
                .stream()
                .anyMatch(userBranch -> branchId.equals(userBranch.branchId()));
        if (!assigned) {
            throw forbidden("branch_id", "OUT_OF_SCOPE", "Branch is outside the actor scope");
        }
    }

    private UUID resolveTenantId(CurrentUser actor, UUID requestedTenantId) {
        UUID tenantId = requestedTenantId == null ? actor.tenantId() : requestedTenantId;
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        if (actor.tenantId() != null && !actor.tenantId().equals(tenantId)) {
            throw forbidden("tenant_id", "TENANT_MISMATCH", "Tenant is outside the actor context");
        }
        return tenantId;
    }

    private CurrentUser currentUserOrSystem(UUID fallbackActorId, UUID fallbackTenantId) {
        try {
            return currentUserContext.currentUser();
        } catch (RuntimeException exception) {
            return new CurrentUser(fallbackActorId, fallbackTenantId, "SYSTEM", Set.of(), LogContext.requestId());
        }
    }

    private String normalizeReason(String reason, AssignmentSource source, UUID assignmentRuleId) {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        if (source == AssignmentSource.RULE) {
            return "Assignment rule applied: " + assignmentRuleId;
        }
        return "Fallback assignment applied";
    }

    private LeadTemperature resolveLeadTemperature(String leadTemperature) {
        if (leadTemperature == null || leadTemperature.isBlank()) {
            return LeadTemperature.NORMAL;
        }
        try {
            return LeadTemperature.valueOf(leadTemperature.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Opportunity lead temperature is invalid");
        }
    }

    private void auditUnassignedItemCreated(CurrentUser actor, UnassignedAssignmentItem item) {
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("unassigned_item_id", item.id().toString());
        afterData.put("tenant_id", item.tenantId().toString());
        afterData.put("opportunity_id", item.opportunityId().toString());
        afterData.put("source_lead_id", item.sourceLeadId().toString());
        afterData.put("assignment_rule_id", item.assignmentRuleId() == null ? null : item.assignmentRuleId().toString());
        afterData.put("reason_code", item.reasonCode().name());
        afterData.put("status", item.status().name());
        auditService.record(new AuditRecordCommand(
                item.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                AuditActions.UNASSIGNED_ASSIGNMENT_ITEM_CREATED,
                AuditResourceTypes.UNASSIGNED_ASSIGNMENT_ITEM,
                item.id(),
                item.id().toString(),
                null,
                afterData,
                Map.of("actor_tenant_id", item.tenantId().toString()),
                item.reasonCode().name(),
                LogContext.requestId()
        ));
    }

    private BusinessException forbidden(String field, String code, String message) {
        return new BusinessException(
                ErrorCode.PERMISSION_DENIED,
                message,
                List.of(ErrorDetail.of(field, code, message))
        );
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
