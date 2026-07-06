package vn.mar.assignment.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.assignment.api.AssignmentRuleSearchCommand;
import vn.mar.assignment.api.AssignmentRuleSnapshot;
import vn.mar.assignment.api.UnassignedAssignmentItemSearchCommand;
import vn.mar.assignment.api.UnassignedAssignmentItemSnapshot;
import vn.mar.assignment.dto.request.CreateAssignmentRuleRequest;
import vn.mar.assignment.dto.request.UpdateAssignmentRuleRequest;
import vn.mar.assignment.entity.AssignmentRule;
import vn.mar.assignment.entity.AssignmentRuleAdvisor;
import vn.mar.assignment.mapper.AssignmentMapper;
import vn.mar.assignment.model.AssignmentRuleAdvisorStatus;
import vn.mar.assignment.model.AssignmentRuleStatus;
import vn.mar.assignment.model.AssignmentStrategy;
import vn.mar.assignment.model.UnassignedAssignmentItemStatus;
import vn.mar.assignment.repository.AssignmentRuleAdvisorRepository;
import vn.mar.assignment.repository.AssignmentRuleRepository;
import vn.mar.assignment.repository.UnassignedAssignmentItemRepository;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.BranchScopeGuard;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.pagination.PageRequestFactory;
import vn.mar.common.tenant.TenantContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@Service
public class AssignmentRuleService {

    private static final String ROLE_ADVISOR = "ADVISOR";

    private final AssignmentRuleRepository assignmentRuleRepository;
    private final AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository;
    private final UnassignedAssignmentItemRepository unassignedAssignmentItemRepository;
    private final LanguageRepository languageRepository;
    private final ProgramRepository programRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final AssignmentMapper assignmentMapper;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final PermissionGuard permissionGuard;
    private final BranchScopeGuard branchScopeGuard;

    public AssignmentRuleService(
            AssignmentRuleRepository assignmentRuleRepository,
            AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository,
            UnassignedAssignmentItemRepository unassignedAssignmentItemRepository,
            LanguageRepository languageRepository,
            ProgramRepository programRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            UserBranchRepository userBranchRepository,
            AssignmentMapper assignmentMapper,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            TimeProvider timeProvider,
            PermissionGuard permissionGuard,
            BranchScopeGuard branchScopeGuard) {
        this.assignmentRuleRepository = assignmentRuleRepository;
        this.assignmentRuleAdvisorRepository = assignmentRuleAdvisorRepository;
        this.unassignedAssignmentItemRepository = unassignedAssignmentItemRepository;
        this.languageRepository = languageRepository;
        this.programRepository = programRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.assignmentMapper = assignmentMapper;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.permissionGuard = permissionGuard;
        this.branchScopeGuard = branchScopeGuard;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssignmentRuleSnapshot> searchRules(AssignmentRuleSearchCommand command) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.ASSIGNMENT_VIEW, PermissionCodes.ASSIGNMENT_MANAGE), "ASSIGNMENT_VIEW_DENIED", "Permission is required to view assignment rules");
        UUID tenantId = TenantContext.requireTenantId(actor);
        UUID branchId = command == null ? null : command.branchId();
        assertBranchScope(actor, branchId, false);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }
        AssignmentRuleStatus status = resolveStatus(command == null ? null : command.status(), false, null);
        PageRequest pageable = PageRequestFactory.of(
                command == null ? null : command.page(),
                command == null ? null : command.size(),
                Sort.by(Sort.Direction.ASC, "priority").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<AssignmentRule> rules = assignmentRuleRepository.search(tenantId, status, branchId, pageable);
        Map<UUID, List<UUID>> advisorIdsByRule = activeAdvisorIdsByRule(tenantId, rules.getContent());
        return PageResponse.from(rules.map(rule -> assignmentMapper.toSnapshot(
                rule,
                advisorIdsByRule.getOrDefault(rule.id(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public AssignmentRuleSnapshot getRule(UUID assignmentRuleId) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.ASSIGNMENT_VIEW, PermissionCodes.ASSIGNMENT_MANAGE), "ASSIGNMENT_VIEW_DENIED", "Permission is required to view assignment rules");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AssignmentRule rule = findRule(tenantId, assignmentRuleId);
        assertBranchScope(actor, rule.branchId(), false);
        return assignmentMapper.toSnapshot(rule, activeAdvisorIds(tenantId, rule.id()));
    }

    @Transactional
    public AssignmentRuleSnapshot createRule(CreateAssignmentRuleRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.ASSIGNMENT_MANAGE, "ASSIGNMENT_MANAGE_DENIED", "Permission is required to manage assignment rules");
        UUID tenantId = TenantContext.requireTenantId(actor);
        RuleDefinition definition = resolveCreateDefinition(tenantId, request);
        assertBranchScope(actor, definition.branchId(), true);
        assertPriorityAvailableForCreate(tenantId, definition.priority(), definition.status());
        validateAdvisorPool(tenantId, definition.branchId(), definition.status(), definition.advisorIds());

        Instant now = timeProvider.now();
        AssignmentRule rule = AssignmentRule.create(
                UUID.randomUUID(),
                tenantId,
                definition.ruleName(),
                definition.priority(),
                definition.languageId(),
                definition.programId(),
                definition.branchId(),
                definition.assignmentStrategy(),
                definition.status(),
                actor.actorId(),
                now
        );
        AssignmentRule savedRule = assignmentRuleRepository.save(rule);
        List<UUID> activeAdvisorIds = replaceAdvisors(actor, savedRule, definition.advisorIds(), now);
        auditRuleChange(AuditActions.ASSIGNMENT_RULE_CREATED, actor, savedRule, null, activeAdvisorIds, null);
        return assignmentMapper.toSnapshot(savedRule, activeAdvisorIds);
    }

    @Transactional
    public AssignmentRuleSnapshot updateRule(UUID assignmentRuleId, UpdateAssignmentRuleRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.ASSIGNMENT_MANAGE, "ASSIGNMENT_MANAGE_DENIED", "Permission is required to manage assignment rules");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AssignmentRule rule = findRule(tenantId, assignmentRuleId);
        assertBranchScope(actor, rule.branchId(), true);

        RuleDefinition definition = resolveUpdateDefinition(tenantId, request);
        assertBranchScope(actor, definition.branchId(), true);
        assertPriorityAvailableForUpdate(tenantId, assignmentRuleId, definition.priority(), definition.status());
        validateAdvisorPool(tenantId, definition.branchId(), definition.status(), definition.advisorIds());

        Instant now = timeProvider.now();
        Map<String, Object> beforeData = assignmentMapper.toAuditData(rule, activeAdvisorIds(tenantId, rule.id()));
        rule.update(
                definition.ruleName(),
                definition.priority(),
                definition.languageId(),
                definition.programId(),
                definition.branchId(),
                definition.assignmentStrategy(),
                definition.status(),
                actor.actorId(),
                now
        );
        AssignmentRule savedRule = assignmentRuleRepository.save(rule);
        List<UUID> activeAdvisorIds = replaceAdvisors(actor, savedRule, definition.advisorIds(), now);
        auditRuleChange(AuditActions.ASSIGNMENT_RULE_UPDATED, actor, savedRule, beforeData, activeAdvisorIds, normalizeOptional(request.reason()));
        return assignmentMapper.toSnapshot(savedRule, activeAdvisorIds);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnassignedAssignmentItemSnapshot> searchUnassignedItems(UnassignedAssignmentItemSearchCommand command) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.ASSIGNMENT_VIEW, PermissionCodes.ASSIGNMENT_MANAGE), "ASSIGNMENT_VIEW_DENIED", "Permission is required to view unassigned queue");
        UUID tenantId = TenantContext.requireTenantId(actor);
        UUID branchId = command == null ? null : command.branchId();
        assertBranchScope(actor, branchId, false);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }
        UnassignedAssignmentItemStatus status = resolveUnassignedStatus(command == null ? null : command.status());
        PageRequest pageable = PageRequestFactory.of(
                command == null ? null : command.page(),
                command == null ? null : command.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(unassignedAssignmentItemRepository
                .search(tenantId, status, branchId, pageable)
                .map(assignmentMapper::toSnapshot));
    }

    private RuleDefinition resolveCreateDefinition(UUID tenantId, CreateAssignmentRuleRequest request) {
        if (request == null) {
            throw ValidationException.of("request", "REQUIRED", "Assignment rule request is required");
        }
        AssignmentRuleStatus status = resolveStatus(request.status(), false, AssignmentRuleStatus.ACTIVE);
        return resolveDefinition(
                tenantId,
                request.ruleName(),
                request.priority(),
                request.languageId(),
                request.programId(),
                request.branchId(),
                request.assignmentStrategy(),
                status,
                request.advisorIds()
        );
    }

    private RuleDefinition resolveUpdateDefinition(UUID tenantId, UpdateAssignmentRuleRequest request) {
        if (request == null) {
            throw ValidationException.of("request", "REQUIRED", "Assignment rule request is required");
        }
        AssignmentRuleStatus status = resolveStatus(request.status(), true, null);
        return resolveDefinition(
                tenantId,
                request.ruleName(),
                request.priority(),
                request.languageId(),
                request.programId(),
                request.branchId(),
                request.assignmentStrategy(),
                status,
                request.advisorIds()
        );
    }

    private RuleDefinition resolveDefinition(
            UUID tenantId,
            String ruleName,
            Integer priority,
            UUID languageId,
            UUID programId,
            UUID branchId,
            String assignmentStrategy,
            AssignmentRuleStatus status,
            List<UUID> advisorIds) {
        String normalizedName = requireRuleName(ruleName);
        int normalizedPriority = requirePriority(priority);
        AssignmentStrategy strategy = resolveStrategy(assignmentStrategy);
        CatalogSelection selection = resolveCatalogSelection(tenantId, languageId, programId);
        UUID resolvedBranchId = resolveBranchId(tenantId, branchId);
        Set<UUID> normalizedAdvisorIds = normalizeAdvisorIds(advisorIds);
        return new RuleDefinition(
                normalizedName,
                normalizedPriority,
                selection.languageId(),
                selection.programId(),
                resolvedBranchId,
                strategy,
                status,
                normalizedAdvisorIds
        );
    }

    private CatalogSelection resolveCatalogSelection(UUID tenantId, UUID languageId, UUID programId) {
        UUID resolvedLanguageId = languageId;
        UUID resolvedProgramId = programId;
        if (programId != null) {
            Program program = programRepository.findByIdAndTenantId(programId, tenantId)
                    .orElseThrow(() -> BusinessException.notFound("program_id", "Program not found"));
            if (program.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("program_id", "Program is inactive");
            }
            if (resolvedLanguageId != null && !resolvedLanguageId.equals(program.languageId())) {
                throw ValidationException.of("program_id", "LANGUAGE_MISMATCH", "Program does not belong to selected language");
            }
            resolvedLanguageId = program.languageId();
            resolvedProgramId = program.id();
        }
        if (resolvedLanguageId != null) {
            Language language = languageRepository.findByIdAndTenantId(resolvedLanguageId, tenantId)
                    .orElseThrow(() -> BusinessException.notFound("language_id", "Language not found"));
            if (language.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("language_id", "Language is inactive");
            }
        }
        return new CatalogSelection(resolvedLanguageId, resolvedProgramId);
    }

    private UUID resolveBranchId(UUID tenantId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("branch_id", "Branch not found"));
        if (branch.status() != BranchStatus.ACTIVE) {
            throw inactiveParent("branch_id", "Branch is inactive");
        }
        return branch.id();
    }

    private void validateAdvisorPool(
            UUID tenantId,
            UUID branchId,
            AssignmentRuleStatus status,
            Set<UUID> advisorIds) {
        if (status == AssignmentRuleStatus.ACTIVE && advisorIds.isEmpty()) {
            throw ValidationException.of("advisor_ids", "REQUIRED", "Active assignment rule requires at least one advisor");
        }
        if (advisorIds.isEmpty()) {
            return;
        }
        List<User> activeAdvisors = userRepository.findByTenantIdAndIdInAndStatusAndRoleCode(
                tenantId,
                advisorIds,
                UserStatus.ACTIVE,
                ROLE_ADVISOR
        );
        if (activeAdvisors.size() != advisorIds.size()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARENT_STATUS,
                    "Advisor pool contains inactive, non-advisor, or cross-tenant user",
                    List.of(ErrorDetail.of("advisor_ids", "INVALID_ADVISOR", "Advisor pool contains inactive, non-advisor, or cross-tenant user"))
            );
        }
        if (branchId != null) {
            Set<UUID> assignedAdvisorIds = userBranchRepository
                    .findByTenantIdAndUserIdInAndStatus(tenantId, advisorIds, UserBranchStatus.ACTIVE)
                    .stream()
                    .filter(userBranch -> branchId.equals(userBranch.branchId()))
                    .map(UserBranch::userId)
                    .collect(Collectors.toSet());
            if (!assignedAdvisorIds.containsAll(advisorIds)) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Every advisor in a branch rule must be assigned to that branch",
                        List.of(ErrorDetail.of("advisor_ids", "BRANCH_ASSIGNMENT_REQUIRED", "Every advisor in a branch rule must be assigned to that branch"))
                );
            }
        }
    }

    private List<UUID> replaceAdvisors(CurrentUser actor, AssignmentRule rule, Set<UUID> requestedAdvisorIds, Instant now) {
        List<AssignmentRuleAdvisor> currentAdvisors = assignmentRuleAdvisorRepository.findByTenantIdAndAssignmentRuleId(
                rule.tenantId(),
                rule.id()
        );
        Map<UUID, AssignmentRuleAdvisor> activeByAdvisorId = currentAdvisors.stream()
                .filter(advisor -> advisor.status() == AssignmentRuleAdvisorStatus.ACTIVE)
                .collect(Collectors.toMap(AssignmentRuleAdvisor::advisorId, advisor -> advisor, (left, right) -> left, LinkedHashMap::new));
        List<AssignmentRuleAdvisor> changed = new ArrayList<>();
        for (AssignmentRuleAdvisor advisor : activeByAdvisorId.values()) {
            if (!requestedAdvisorIds.contains(advisor.advisorId())) {
                advisor.inactivate(actor.actorId(), now);
                changed.add(advisor);
            }
        }
        for (UUID advisorId : requestedAdvisorIds) {
            if (!activeByAdvisorId.containsKey(advisorId)) {
                changed.add(AssignmentRuleAdvisor.create(
                        UUID.randomUUID(),
                        rule.tenantId(),
                        rule.id(),
                        advisorId,
                        actor.actorId(),
                        now
                ));
            }
        }
        if (!changed.isEmpty()) {
            assignmentRuleAdvisorRepository.saveAll(changed);
        }
        return requestedAdvisorIds.stream()
                .sorted()
                .toList();
    }

    private Map<UUID, List<UUID>> activeAdvisorIdsByRule(UUID tenantId, List<AssignmentRule> rules) {
        if (rules.isEmpty()) {
            return Map.of();
        }
        List<UUID> ruleIds = rules.stream().map(AssignmentRule::id).toList();
        return assignmentRuleAdvisorRepository
                .findByTenantIdAndAssignmentRuleIdInAndStatus(tenantId, ruleIds, AssignmentRuleAdvisorStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(
                        AssignmentRuleAdvisor::assignmentRuleId,
                        LinkedHashMap::new,
                        Collectors.mapping(AssignmentRuleAdvisor::advisorId, Collectors.toList())
                ));
    }

    private List<UUID> activeAdvisorIds(UUID tenantId, UUID assignmentRuleId) {
        return assignmentRuleAdvisorRepository
                .findByTenantIdAndAssignmentRuleIdAndStatus(tenantId, assignmentRuleId, AssignmentRuleAdvisorStatus.ACTIVE)
                .stream()
                .map(AssignmentRuleAdvisor::advisorId)
                .sorted()
                .toList();
    }

    private AssignmentRule findRule(UUID tenantId, UUID assignmentRuleId) {
        if (assignmentRuleId == null) {
            throw ValidationException.of("assignment_rule_id", "REQUIRED", "Assignment rule id is required");
        }
        return assignmentRuleRepository.findByIdAndTenantId(assignmentRuleId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("assignment_rule_id", "Assignment rule not found"));
    }

    private void assertPriorityAvailableForCreate(UUID tenantId, int priority, AssignmentRuleStatus status) {
        if (status == AssignmentRuleStatus.ACTIVE
                && assignmentRuleRepository.existsByTenantIdAndPriorityAndStatus(tenantId, priority, AssignmentRuleStatus.ACTIVE)) {
            throw duplicatePriority(priority);
        }
    }

    private void assertPriorityAvailableForUpdate(UUID tenantId, UUID ruleId, int priority, AssignmentRuleStatus status) {
        if (status == AssignmentRuleStatus.ACTIVE
                && assignmentRuleRepository.existsByTenantIdAndPriorityAndStatusAndIdNot(tenantId, priority, AssignmentRuleStatus.ACTIVE, ruleId)) {
            throw duplicatePriority(priority);
        }
    }

    private void ensureBranchBelongsToTenant(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("branch_id", "Branch not found"));
    }

    private void assertBranchScope(CurrentUser actor, UUID branchId, boolean managing) {
        String message = managing
                ? "Sales Lead can only manage branch assignment rules"
                : "Sales Lead must specify a branch to view assignment data";
        branchScopeGuard.requireAssignedBranchForSalesLead(actor, branchId, message);
    }

    private String requireRuleName(String ruleName) {
        if (!StringUtils.hasText(ruleName)) {
            throw ValidationException.of("rule_name", "REQUIRED", "Rule name is required");
        }
        return ruleName.trim();
    }

    private int requirePriority(Integer priority) {
        if (priority == null) {
            throw ValidationException.of("priority", "REQUIRED", "Priority is required");
        }
        if (priority < 0) {
            throw ValidationException.of("priority", "MIN_VALUE", "Priority must be greater than or equal to 0");
        }
        return priority;
    }

    private Set<UUID> normalizeAdvisorIds(Collection<UUID> advisorIds) {
        if (advisorIds == null) {
            throw ValidationException.of("advisor_ids", "REQUIRED", "Advisor ids are required");
        }
        Set<UUID> normalized = new LinkedHashSet<>();
        for (UUID advisorId : advisorIds) {
            if (advisorId == null) {
                throw ValidationException.of("advisor_ids", "INVALID", "Advisor id must not be null");
            }
            normalized.add(advisorId);
        }
        return normalized;
    }

    private AssignmentStrategy resolveStrategy(String value) {
        if (!StringUtils.hasText(value)) {
            throw ValidationException.of("assignment_strategy", "REQUIRED", "Assignment strategy is required");
        }
        try {
            return AssignmentStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("assignment_strategy", "INVALID", "Assignment strategy is invalid");
        }
    }

    private AssignmentRuleStatus resolveStatus(String value, boolean required, AssignmentRuleStatus defaultStatus) {
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw ValidationException.of("status", "REQUIRED", "Assignment rule status is required");
            }
            return defaultStatus;
        }
        try {
            return AssignmentRuleStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("status", "INVALID", "Assignment rule status is invalid");
        }
    }

    private UnassignedAssignmentItemStatus resolveUnassignedStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return UnassignedAssignmentItemStatus.OPEN;
        }
        try {
            return UnassignedAssignmentItemStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("status", "INVALID", "Unassigned item status is invalid");
        }
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BusinessException duplicatePriority(int priority) {
        return new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Active assignment rule priority already exists",
                List.of(ErrorDetail.of("priority", String.valueOf(priority), "Active assignment rule priority already exists"))
        );
    }

    private BusinessException inactiveParent(String field, String message) {
        return new BusinessException(
                ErrorCode.INVALID_PARENT_STATUS,
                message,
                List.of(ErrorDetail.of(field, "INACTIVE", message))
        );
    }




    private void auditRuleChange(
            String action,
            CurrentUser actor,
            AssignmentRule rule,
            Map<String, Object> beforeData,
            List<UUID> advisorIds,
            String reason) {
        auditService.record(new AuditRecordCommand(
                rule.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                AuditResourceTypes.ASSIGNMENT_RULE,
                rule.id(),
                rule.id().toString(),
                beforeData,
                assignmentMapper.toAuditData(rule, advisorIds),
                Map.of("actor_tenant_id", actor.tenantId().toString()),
                reason,
                LogContext.requestId()
        ));
    }

    private record CatalogSelection(UUID languageId, UUID programId) {
    }

    private record RuleDefinition(
            String ruleName,
            int priority,
            UUID languageId,
            UUID programId,
            UUID branchId,
            AssignmentStrategy assignmentStrategy,
            AssignmentRuleStatus status,
            Set<UUID> advisorIds
    ) {
    }
}
