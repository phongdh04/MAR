package vn.mar.sla.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.BranchScopeGuard;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.pagination.PageRequestFactory;
import vn.mar.common.tenant.TenantContext;
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
import vn.mar.sla.api.SlaTaskManagementService;
import vn.mar.sla.api.SlaTaskSearchCommand;
import vn.mar.sla.api.SlaTaskSnapshot;
import vn.mar.sla.entity.SlaTask;
import vn.mar.sla.mapper.SlaTaskMapper;
import vn.mar.sla.model.SlaOverdueLevel;
import vn.mar.sla.model.SlaTaskStatus;
import vn.mar.sla.model.SlaTaskType;
import vn.mar.sla.repository.SlaTaskRepository;

@Service
public class SlaTaskService implements SlaTaskManagementService {

    private static final Logger log = LoggerFactory.getLogger(SlaTaskService.class);
    private static final Duration SALES_LEAD_ESCALATION_DELAY = Duration.ofMinutes(15);
    private static final List<SlaTaskStatus> ACTIVE_STATUSES = List.of(SlaTaskStatus.OPEN, SlaTaskStatus.OVERDUE);
    private static final String ROLE_ADVISOR = "ADVISOR";
    private static final String ROLE_SALES_LEAD = "SALES_LEAD";

    private final SlaTaskRepository slaTaskRepository;
    private final SlaPolicyLookupService slaPolicyLookupService;
    private final BranchRepository branchRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final SlaTaskMapper slaTaskMapper;
    private final BranchScopeGuard branchScopeGuard;

    public SlaTaskService(
            SlaTaskRepository slaTaskRepository,
            SlaPolicyLookupService slaPolicyLookupService,
            BranchRepository branchRepository,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            TimeProvider timeProvider,
            SlaTaskMapper slaTaskMapper,
            BranchScopeGuard branchScopeGuard) {
        this.slaTaskRepository = slaTaskRepository;
        this.slaPolicyLookupService = slaPolicyLookupService;
        this.branchRepository = branchRepository;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.slaTaskMapper = slaTaskMapper;
        this.branchScopeGuard = branchScopeGuard;
    }

    @Override
    @Transactional
    public SlaTaskSnapshot openFirstResponseTask(OpenFirstResponseSlaTaskCommand command) {
        validateOpenCommand(command);
        Instant assignedAt = command.assignedAt();
        Instant now = timeProvider.now();
        LeadTemperature leadTemperature = command.leadTemperature() == null
                ? LeadTemperature.NORMAL
                : command.leadTemperature();
        DueTimeCalculationResult dueTime = slaPolicyLookupService.calculateFirstResponseDueAt(
                new DueTimeCalculationCommand(
                        command.tenantId(),
                        command.branchId(),
                        leadTemperature,
                        assignedAt
                )
        );

        Optional<SlaTask> latestTask = slaTaskRepository
                .findFirstByTenantIdAndOpportunityIdAndTaskTypeOrderByCreatedAtDescIdDesc(
                        command.tenantId(),
                        command.opportunityId(),
                        SlaTaskType.FIRST_RESPONSE
                );
        if (latestTask.isPresent() && latestTask.get().status() == SlaTaskStatus.COMPLETED) {
            return slaTaskMapper.toSnapshot(latestTask.get());
        }

        boolean activeTaskExists = latestTask
                .filter(existingTask -> ACTIVE_STATUSES.contains(existingTask.status()))
                .isPresent();
        SlaTask task = activeTaskExists
                ? latestTask.get()
                : SlaTask.openFirstResponse(
                        UUID.randomUUID(),
                        command.tenantId(),
                        command.opportunityId(),
                        command.sourceLeadId(),
                        command.ownerId(),
                        command.branchId(),
                        dueTime.policy().slaPolicyId(),
                        leadTemperature,
                        dueTime.dueAt(),
                        command.actorId(),
                        now
                );

        Map<String, Object> beforeData = activeTaskExists ? toAuditData(task) : null;
        if (activeTaskExists) {
            task.refreshAssignment(
                    command.ownerId(),
                    command.branchId(),
                    dueTime.policy().slaPolicyId(),
                    leadTemperature,
                    dueTime.dueAt(),
                    command.actorId(),
                    now
            );
        }
        SlaTask savedTask = slaTaskRepository.save(task);
        auditTaskChange(
                command.actorId(),
                command.actorRoleCode(),
                activeTaskExists ? AuditActions.SLA_TASK_REFRESHED : AuditActions.SLA_TASK_CREATED,
                savedTask,
                beforeData,
                toAuditData(savedTask),
                activeTaskExists
                        ? "First response SLA task refreshed after assignment"
                        : "First response SLA task opened"
        );
        return slaTaskMapper.toSnapshot(savedTask);
    }

    @Override
    @Transactional
    public Optional<SlaTaskSnapshot> completeFirstResponseTask(CompleteFirstResponseSlaTaskCommand command) {
        validateCompleteCommand(command);
        Optional<SlaTask> activeTask = slaTaskRepository
                .findFirstByTenantIdAndOpportunityIdAndTaskTypeAndStatusInOrderByCreatedAtDescIdDesc(
                        command.tenantId(),
                        command.opportunityId(),
                        SlaTaskType.FIRST_RESPONSE,
                        ACTIVE_STATUSES
                );
        if (activeTask.isEmpty()) {
            return Optional.empty();
        }
        SlaTask task = activeTask.get();
        Map<String, Object> beforeData = toAuditData(task);
        task.complete(command.activityId(), command.occurredAt(), command.actorId(), timeProvider.now());
        SlaTask savedTask = slaTaskRepository.save(task);
        auditTaskChange(
                command.actorId(),
                command.actorRoleCode(),
                AuditActions.SLA_TASK_COMPLETED,
                savedTask,
                beforeData,
                toAuditData(savedTask),
                Boolean.TRUE.equals(savedTask.slaHit())
                        ? "First response SLA hit"
                        : "First response SLA completed after due time"
        );
        return Optional.of(slaTaskMapper.toSnapshot(savedTask));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SlaTaskSnapshot> searchTasks(SlaTaskSearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "SLA task search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertCanViewTasks(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        SlaTaskStatus status = parseStatus(command.status());
        SlaTaskType taskType = parseTaskType(command.taskType());
        UUID branchId = command.branchId();
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }
        UUID ownerId = resolveSearchOwner(actor, command.ownerId());
        PageRequest pageable = PageRequestFactory.of(command.page(), command.size(), dueFirstSort());
        Page<SlaTask> page;
        if (isSalesLead(actor)) {
            List<UUID> branchIds = resolveSalesLeadBranchScope(actor, branchId);
            if (branchIds.isEmpty()) {
                return PageResponse.empty(pageable);
            }
            page = slaTaskRepository.searchBranchScoped(tenantId, branchIds, ownerId, status, taskType, pageable);
        } else {
            page = slaTaskRepository.search(tenantId, ownerId, branchId, status, taskType, pageable);
        }
        return PageResponse.from(page.map(slaTaskMapper::toSnapshot));
    }

    @Override
    @Transactional
    public SlaOverdueScanSnapshot scanOverdueTasks() {
        CurrentUser actor = currentUserContext.currentUser();
        assertCanManageTasks(actor);
        UUID tenantId = TenantContext.requireTenantId(actor);
        Instant now = timeProvider.now();
        List<UUID> branchIds = isSalesLead(actor) ? resolveSalesLeadBranchScope(actor, null) : null;
        int escalatedSalesLeadCount = escalateReadyAdvisorOverdueTasks(actor, tenantId, branchIds, now);
        int markedOverdueCount = markOpenDueTasks(actor, tenantId, branchIds, now);
        log.info(
                "sla overdue scan completed tenantId={} markedOverdueCount={} escalatedSalesLeadCount={}",
                tenantId,
                markedOverdueCount,
                escalatedSalesLeadCount
        );
        return new SlaOverdueScanSnapshot(now, markedOverdueCount, escalatedSalesLeadCount);
    }

    private int markOpenDueTasks(CurrentUser actor, UUID tenantId, List<UUID> branchIds, Instant now) {
        List<SlaTask> tasks = branchIds == null
                ? slaTaskRepository.findByTenantIdAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                        tenantId,
                        SlaTaskStatus.OPEN,
                        now
                )
                : branchIds.isEmpty()
                        ? List.of()
                        : slaTaskRepository.findByTenantIdAndBranchIdInAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                                tenantId,
                                branchIds,
                                SlaTaskStatus.OPEN,
                                now
                        );
        for (SlaTask task : tasks) {
            Map<String, Object> beforeData = toAuditData(task);
            task.markOverdue(actor.actorId(), now);
            slaTaskRepository.save(task);
            auditTaskChange(
                    actor.actorId(),
                    actor.roleCode(),
                    AuditActions.SLA_TASK_OVERDUE_MARKED,
                    task,
                    beforeData,
                    toAuditData(task),
                    "First response SLA overdue alert sent to Advisor"
            );
        }
        return tasks.size();
    }

    private int escalateReadyAdvisorOverdueTasks(CurrentUser actor, UUID tenantId, List<UUID> branchIds, Instant now) {
        Instant escalationCutoff = now.minus(SALES_LEAD_ESCALATION_DELAY);
        List<SlaTask> tasks = branchIds == null
                ? slaTaskRepository.findAdvisorOverdueReadyForSalesLead(
                        tenantId,
                        SlaTaskStatus.OVERDUE,
                        SlaOverdueLevel.ADVISOR,
                        escalationCutoff
                )
                : branchIds.isEmpty()
                        ? List.of()
                        : slaTaskRepository.findBranchScopedAdvisorOverdueReadyForSalesLead(
                                tenantId,
                                branchIds,
                                SlaTaskStatus.OVERDUE,
                                SlaOverdueLevel.ADVISOR,
                                escalationCutoff
                        );
        for (SlaTask task : tasks) {
            Map<String, Object> beforeData = toAuditData(task);
            task.escalateToSalesLead(actor.actorId(), now);
            slaTaskRepository.save(task);
            auditTaskChange(
                    actor.actorId(),
                    actor.roleCode(),
                    AuditActions.SLA_TASK_ESCALATED_TO_SALES_LEAD,
                    task,
                    beforeData,
                    toAuditData(task),
                    "First response SLA overdue alert escalated to Sales Lead"
            );
        }
        return tasks.size();
    }

    private void validateOpenCommand(OpenFirstResponseSlaTaskCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "SLA task open command is required");
        }
        requireId(command.tenantId(), "tenant_id", "Tenant id is required");
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
        requireId(command.sourceLeadId(), "source_lead_id", "Source lead id is required");
        requireId(command.ownerId(), "owner_id", "Owner id is required");
        requireInstant(command.assignedAt(), "assigned_at", "Assigned time is required");
    }

    private void validateCompleteCommand(CompleteFirstResponseSlaTaskCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "SLA task complete command is required");
        }
        requireId(command.tenantId(), "tenant_id", "Tenant id is required");
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
        requireId(command.activityId(), "activity_id", "Activity id is required");
        requireInstant(command.occurredAt(), "occurred_at", "Activity occurred time is required");
    }

    private UUID resolveSearchOwner(CurrentUser actor, UUID requestedOwnerId) {
        if (isAdvisor(actor)) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(actor.actorId())) {
                throw BusinessException.forbidden("owner_id", "OWN_SCOPE_REQUIRED", "Advisor can only view own SLA tasks");
            }
            return actor.actorId();
        }
        return requestedOwnerId;
    }

    private List<UUID> resolveSalesLeadBranchScope(CurrentUser actor, UUID requestedBranchId) {
        return branchScopeGuard.resolveAssignedBranchesForSalesLead(actor, requestedBranchId);
    }

    private SlaTaskStatus parseStatus(String requestedStatus) {
        if (!StringUtils.hasText(requestedStatus)) {
            return null;
        }
        try {
            return SlaTaskStatus.valueOf(normalizeEnum(requestedStatus));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("status", "INVALID_STATUS", "SLA task status is invalid");
        }
    }

    private SlaTaskType parseTaskType(String requestedTaskType) {
        if (!StringUtils.hasText(requestedTaskType)) {
            return null;
        }
        try {
            return SlaTaskType.valueOf(normalizeEnum(requestedTaskType));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("task_type", "INVALID_TASK_TYPE", "SLA task type is invalid");
        }
    }

    private void ensureBranchBelongsToTenant(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("branch_id", "Branch was not found"));
    }

    private void assertCanViewTasks(CurrentUser actor) {
        if (actor == null
                || (!actor.hasPermission(PermissionCodes.SLA_TASK_VIEW)
                && !actor.hasPermission(PermissionCodes.SLA_TASK_MANAGE))) {
            throw BusinessException.forbidden("permission", "SLA_TASK_VIEW_DENIED", "Permission is required to view SLA tasks");
        }
    }

    private void assertCanManageTasks(CurrentUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.SLA_TASK_MANAGE)) {
            throw BusinessException.forbidden("permission", "SLA_TASK_MANAGE_DENIED", "Permission is required to manage SLA tasks");
        }
    }


    private UUID requireId(UUID id, String field, String message) {
        if (id == null) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return id;
    }

    private Instant requireInstant(Instant instant, String field, String message) {
        if (instant == null) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return instant;
    }


    private Sort dueFirstSort() {
        return Sort.by(Sort.Direction.ASC, "dueAt").and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private boolean isAdvisor(CurrentUser actor) {
        return actor != null && ROLE_ADVISOR.equals(actor.roleCode());
    }

    private boolean isSalesLead(CurrentUser actor) {
        return actor != null && ROLE_SALES_LEAD.equals(actor.roleCode());
    }

    private String normalizeEnum(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> toAuditData(SlaTask task) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sla_task_id", task.id().toString());
        data.put("tenant_id", task.tenantId().toString());
        data.put("opportunity_id", task.opportunityId().toString());
        data.put("source_lead_id", task.sourceLeadId().toString());
        data.put("owner_id", task.ownerId().toString());
        data.put("branch_id", task.branchId() == null ? null : task.branchId().toString());
        data.put("sla_policy_id", task.slaPolicyId() == null ? null : task.slaPolicyId().toString());
        data.put("task_type", task.taskType().name());
        data.put("status", task.status().name());
        data.put("lead_temperature", task.leadTemperature().name());
        data.put("due_at", task.dueAt().toString());
        data.put("completed_at", task.completedAt() == null ? null : task.completedAt().toString());
        data.put("completed_activity_id", task.completedActivityId() == null ? null : task.completedActivityId().toString());
        data.put("sla_hit", task.slaHit());
        data.put("overdue_marked_at", task.overdueMarkedAt() == null ? null : task.overdueMarkedAt().toString());
        data.put("overdue_level", task.overdueLevel().name());
        data.put("escalated_to", task.escalatedTo() == null ? null : task.escalatedTo().toString());
        data.put("sales_lead_escalated_at", task.salesLeadEscalatedAt() == null ? null : task.salesLeadEscalatedAt().toString());
        return data;
    }

    private void auditTaskChange(
            UUID actorId,
            String actorRoleCode,
            String action,
            SlaTask task,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actor_tenant_id", task.tenantId().toString());
        metadata.put("opportunity_id", task.opportunityId().toString());
        metadata.put("owner_id", task.ownerId().toString());
        metadata.put("overdue_level", task.overdueLevel().name());
        auditService.record(new AuditRecordCommand(
                task.tenantId(),
                actorId,
                actorId == null ? "SYSTEM" : "USER",
                StringUtils.hasText(actorRoleCode) ? actorRoleCode : "SYSTEM",
                action,
                AuditResourceTypes.SLA_TASK,
                task.id(),
                task.id().toString(),
                beforeData,
                afterData,
                metadata,
                reason,
                LogContext.requestId()
        ));
    }



}
