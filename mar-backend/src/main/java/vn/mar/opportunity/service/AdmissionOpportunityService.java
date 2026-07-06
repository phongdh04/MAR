package vn.mar.opportunity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import vn.mar.authz.service.PermissionGuard;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.entity.Course;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.CourseRepository;
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
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.entity.Lead;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.opportunity.api.AssignOpportunityOwnerCommand;
import vn.mar.opportunity.api.AdmissionOpportunityManagementService;
import vn.mar.opportunity.api.AdmissionOpportunitySearchCommand;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.ChangeOpportunityStageCommand;
import vn.mar.opportunity.api.CreateAdmissionOpportunityCommand;
import vn.mar.opportunity.api.CreateOpportunityActivityCommand;
import vn.mar.opportunity.api.OpportunityAssignmentService;
import vn.mar.opportunity.api.OpportunityAssignmentSnapshot;
import vn.mar.opportunity.api.OpportunityActivitySearchCommand;
import vn.mar.opportunity.api.OpportunityActivitySnapshot;
import vn.mar.opportunity.api.StageChangeSnapshot;
import vn.mar.opportunity.api.StageHistorySnapshot;
import vn.mar.opportunity.api.UpdateAdmissionOpportunityCommand;
import vn.mar.opportunity.entity.Activity;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.entity.StageHistory;
import vn.mar.opportunity.entity.Touchpoint;
import vn.mar.opportunity.mapper.AdmissionOpportunityMapper;
import vn.mar.opportunity.model.ActivityActorType;
import vn.mar.opportunity.model.ActivityResult;
import vn.mar.opportunity.model.ActivitySource;
import vn.mar.opportunity.model.ActivityType;
import vn.mar.opportunity.model.LostReason;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.model.QualificationStatus;
import vn.mar.opportunity.model.TouchpointType;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.CompleteFirstResponseSlaTaskCommand;
import vn.mar.sla.api.SlaTaskManagementService;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;

@Service
public class AdmissionOpportunityService implements AdmissionOpportunityManagementService, OpportunityAssignmentService {

    private static final Set<OpportunityStage> ACTIVE_DUPLICATE_STAGES = EnumSet.of(
            OpportunityStage.NEW,
            OpportunityStage.CONTACTING,
            OpportunityStage.CONTACTED,
            OpportunityStage.QUALIFIED,
            OpportunityStage.PROGRAM_SELECTED,
            OpportunityStage.APPOINTMENT_BOOKED,
            OpportunityStage.APPOINTMENT_DONE,
            OpportunityStage.NO_SHOW,
            OpportunityStage.CONSULTING,
            OpportunityStage.DEPOSIT_PAID,
            OpportunityStage.NURTURING
    );
    private static final Map<OpportunityStage, Set<OpportunityStage>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(OpportunityStage.NEW, EnumSet.of(OpportunityStage.CONTACTING, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.CONTACTING, EnumSet.of(OpportunityStage.CONTACTED, OpportunityStage.LOST, OpportunityStage.NURTURING)),
            Map.entry(OpportunityStage.CONTACTED, EnumSet.of(OpportunityStage.QUALIFIED, OpportunityStage.LOST, OpportunityStage.NURTURING)),
            Map.entry(OpportunityStage.QUALIFIED, EnumSet.of(OpportunityStage.PROGRAM_SELECTED, OpportunityStage.APPOINTMENT_BOOKED, OpportunityStage.CONSULTING, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.PROGRAM_SELECTED, EnumSet.of(OpportunityStage.APPOINTMENT_BOOKED, OpportunityStage.CONSULTING, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.APPOINTMENT_BOOKED, EnumSet.of(OpportunityStage.APPOINTMENT_DONE, OpportunityStage.NO_SHOW, OpportunityStage.CANCELLED)),
            Map.entry(OpportunityStage.APPOINTMENT_DONE, EnumSet.of(OpportunityStage.CONSULTING, OpportunityStage.DEPOSIT_PAID, OpportunityStage.ENROLLED, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.NO_SHOW, EnumSet.of(OpportunityStage.CONTACTING, OpportunityStage.NURTURING, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.CONSULTING, EnumSet.of(OpportunityStage.DEPOSIT_PAID, OpportunityStage.ENROLLED, OpportunityStage.LOST, OpportunityStage.NURTURING)),
            Map.entry(OpportunityStage.DEPOSIT_PAID, EnumSet.of(OpportunityStage.ENROLLED, OpportunityStage.LOST, OpportunityStage.REFUNDED)),
            Map.entry(OpportunityStage.ENROLLED, EnumSet.noneOf(OpportunityStage.class)),
            Map.entry(OpportunityStage.LOST, EnumSet.of(OpportunityStage.CONTACTING, OpportunityStage.NURTURING)),
            Map.entry(OpportunityStage.NURTURING, EnumSet.of(OpportunityStage.CONTACTING, OpportunityStage.QUALIFIED, OpportunityStage.LOST)),
            Map.entry(OpportunityStage.CANCELLED, EnumSet.noneOf(OpportunityStage.class)),
            Map.entry(OpportunityStage.REFUNDED, EnumSet.noneOf(OpportunityStage.class))
    );
    private static final Set<String> LOST_REOPEN_ROLES = Set.of("ADMIN", "SALES_LEAD");

    private final AdmissionOpportunityRepository admissionOpportunityRepository;
    private final TouchpointRepository touchpointRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final LanguageRepository languageRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AdmissionOpportunityMapper admissionOpportunityMapper;
    private final SlaTaskManagementService slaTaskManagementService;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final PermissionGuard permissionGuard;

    public AdmissionOpportunityService(
            AdmissionOpportunityRepository admissionOpportunityRepository,
            TouchpointRepository touchpointRepository,
            StageHistoryRepository stageHistoryRepository,
            ActivityRepository activityRepository,
            LeadRepository leadRepository,
            CustomerProfileRepository customerProfileRepository,
            LanguageRepository languageRepository,
            ProgramRepository programRepository,
            CourseRepository courseRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            AdmissionOpportunityMapper admissionOpportunityMapper,
            SlaTaskManagementService slaTaskManagementService,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            PermissionGuard permissionGuard) {
        this.admissionOpportunityRepository = admissionOpportunityRepository;
        this.touchpointRepository = touchpointRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.activityRepository = activityRepository;
        this.leadRepository = leadRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.languageRepository = languageRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.admissionOpportunityMapper = admissionOpportunityMapper;
        this.slaTaskManagementService = slaTaskManagementService;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.permissionGuard = permissionGuard;
    }

    @Override
    @Transactional
    public AdmissionOpportunitySnapshot createOrLinkFromLead(CreateAdmissionOpportunityCommand command) {
        validateCreateCommand(command);
        UUID tenantId = command.tenantId();
        Instant now = timeProvider.now();
        Lead lead = findLead(tenantId, command.sourceLeadId());
        CustomerProfile customer = findCustomer(tenantId, command.customerId());
        assertLeadCustomerCompatible(lead, customer);
        CatalogSelection selection = resolveCreateSelection(tenantId, lead, command);
        UUID branchId = resolveBranchId(tenantId, firstNonNull(command.branchId(), lead.branchId()), true);
        UUID ownerId = resolveOwnerId(tenantId, command.ownerId(), true);
        LeadTemperature leadTemperature = firstNonNull(command.leadTemperature(), lead.leadTemperature());
        TouchpointType touchpointType = firstNonNull(command.touchpointType(), toTouchpointType(lead.sourceType()));
        Instant touchTime = firstNonNull(command.touchTime(), firstNonNull(lead.sourceCreatedAt(), now));

        AdmissionOpportunity opportunity = findReusableOpportunity(
                        tenantId,
                        customer.id(),
                        selection.programId()
                )
                .orElseGet(() -> createNewOpportunity(
                        tenantId,
                        customer.id(),
                        lead.id(),
                        selection,
                        branchId,
                        ownerId,
                        leadTemperature,
                        now
                ));
        boolean reusedOpportunity = !opportunity.sourceLeadId().equals(lead.id());

        Touchpoint touchpoint = createTouchpoint(tenantId, customer.id(), lead, opportunity.id(), touchTime, touchpointType, now);
        opportunity.linkTouchpoint(touchpoint.id(), now);
        AdmissionOpportunity savedOpportunity = admissionOpportunityRepository.save(opportunity);
        lead.linkCustomerAndOpportunity(customer.id(), savedOpportunity.id(), now);
        leadRepository.save(lead);

        if (reusedOpportunity) {
            auditOpportunityChange(
                    AuditActions.OPPORTUNITY_LINKED_TO_EXISTING,
                    savedOpportunity,
                    currentUserOrSystem(),
                    null,
                    admissionOpportunityMapper.toAuditData(savedOpportunity),
                    "Linked new lead to existing active opportunity"
            );
        }
        auditTouchpointCreated(savedOpportunity, touchpoint);
        return admissionOpportunityMapper.toSnapshot(savedOpportunity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdmissionOpportunitySnapshot> searchOpportunities(AdmissionOpportunitySearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.LEAD_VIEW, PermissionCodes.OPPORTUNITY_UPDATE), "OPPORTUNITY_VIEW_DENIED", "Permission is required to view opportunities");
        UUID tenantId = TenantContext.requireTenantId(actor);
        UUID ownerId = resolveSearchOwner(actor, command.ownerId());
        OpportunityStage stage = resolveStage(command.stage());
        PageRequest pageable = PageRequestFactory.of(command.page(), command.size(), newestFirstSort());
        Page<AdmissionOpportunitySnapshot> responsePage = admissionOpportunityRepository.search(
                        tenantId,
                        ownerId,
                        stage,
                        command.languageId(),
                        command.programId(),
                        pageable
                )
                .map(admissionOpportunityMapper::toSnapshot);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionOpportunitySnapshot getOpportunity(UUID opportunityId) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.LEAD_VIEW, PermissionCodes.OPPORTUNITY_UPDATE), "OPPORTUNITY_VIEW_DENIED", "Permission is required to view opportunities");
        AdmissionOpportunity opportunity = findOpportunity(TenantContext.requireTenantId(actor), opportunityId);
        assertOpportunityVisibleToActor(actor, opportunity);
        return admissionOpportunityMapper.toSnapshot(opportunity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageHistorySnapshot> getStageHistory(UUID opportunityId) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requireAnyPermission(actor, List.of(PermissionCodes.LEAD_VIEW, PermissionCodes.OPPORTUNITY_UPDATE), "OPPORTUNITY_VIEW_DENIED", "Permission is required to view opportunity stage history");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AdmissionOpportunity opportunity = findOpportunity(tenantId, opportunityId);
        assertOpportunityVisibleToActor(actor, opportunity);
        return stageHistoryRepository
                .findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(tenantId, opportunity.id())
                .stream()
                .map(admissionOpportunityMapper::toSnapshot)
                .toList();
    }

    @Override
    @Transactional
    public OpportunityActivitySnapshot createActivity(CreateOpportunityActivityCommand command) {
        validateCreateActivityCommand(command);
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.ACTIVITY_CREATE, "ACTIVITY_CREATE_DENIED", "Permission is required to create opportunity activities");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AdmissionOpportunity opportunity = findOpportunity(tenantId, command.opportunityId());
        assertOpportunityVisibleToActor(actor, opportunity);

        ActivityType activityType = resolveActivityType(command.activityType());
        ActivityResult activityResult = resolveActivityResult(activityType, command.activityResult());
        ActivitySource source = resolveActivitySource(command.source());
        validateActivityTiming(command.occurredAt(), command.nextActionAt());

        Activity activity = Activity.create(
                UUID.randomUUID(),
                tenantId,
                opportunity.customerId(),
                opportunity.id(),
                actor.actorId(),
                actor.actorId() == null ? ActivityActorType.SYSTEM : ActivityActorType.USER,
                activityType,
                activityResult,
                command.occurredAt(),
                normalizeOptional(command.note()),
                command.nextActionAt(),
                source,
                timeProvider.now()
        );
        Activity savedActivity = activityRepository.save(activity);
        auditActivityCreated(savedActivity, actor);
        if (savedActivity.firstResponseCandidate()) {
            slaTaskManagementService.completeFirstResponseTask(new CompleteFirstResponseSlaTaskCommand(
                    savedActivity.tenantId(),
                    savedActivity.opportunityId(),
                    savedActivity.id(),
                    savedActivity.occurredAt(),
                    actor.actorId(),
                    actor.roleCode()
            ));
        }
        return admissionOpportunityMapper.toSnapshot(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OpportunityActivitySnapshot> searchActivities(OpportunityActivitySearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity activity search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.ACTIVITY_VIEW, "ACTIVITY_VIEW_DENIED", "Permission is required to view opportunity activities");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AdmissionOpportunity opportunity = findOpportunity(tenantId, command.opportunityId());
        assertOpportunityVisibleToActor(actor, opportunity);
        Page<OpportunityActivitySnapshot> activities = activityRepository
                .findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc(
                        tenantId,
                        opportunity.id(),
                        PageRequestFactory.of(command.page(), command.size())
                )
                .map(admissionOpportunityMapper::toSnapshot);
        return PageResponse.from(activities);
    }

    @Override
    @Transactional
    public AdmissionOpportunitySnapshot updateOpportunity(UpdateAdmissionOpportunityCommand command) {
        validateUpdateCommand(command);
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.OPPORTUNITY_UPDATE, "OPPORTUNITY_UPDATE_DENIED", "Permission is required to update opportunities");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AdmissionOpportunity opportunity = findOpportunity(tenantId, command.opportunityId());
        assertOpportunityVisibleToActor(actor, opportunity);
        Map<String, Object> beforeData = admissionOpportunityMapper.toAuditData(opportunity);
        CatalogSelection selection = resolveUpdateSelection(tenantId, opportunity, command);
        UUID branchId = command.branchId() == null
                ? opportunity.branchId()
                : resolveBranchId(tenantId, command.branchId(), true);
        QualificationStatus qualificationStatus = resolveQualificationStatus(command.qualificationStatus(), opportunity.qualificationStatus());
        opportunity.updateFields(
                selection.languageId(),
                selection.programId(),
                selection.courseId(),
                branchId,
                qualificationStatus,
                timeProvider.now()
        );
        AdmissionOpportunity savedOpportunity = admissionOpportunityRepository.save(opportunity);
        auditOpportunityChange(
                AuditActions.OPPORTUNITY_UPDATED,
                savedOpportunity,
                actor,
                beforeData,
                admissionOpportunityMapper.toAuditData(savedOpportunity),
                normalizeOptional(command.note())
        );
        return admissionOpportunityMapper.toSnapshot(savedOpportunity);
    }

    @Override
    @Transactional
    public StageChangeSnapshot changeStage(ChangeOpportunityStageCommand command) {
        validateChangeStageCommand(command);
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.OPPORTUNITY_UPDATE, "OPPORTUNITY_UPDATE_DENIED", "Permission is required to update opportunity stage");
        UUID tenantId = TenantContext.requireTenantId(actor);
        AdmissionOpportunity opportunity = findOpportunity(tenantId, command.opportunityId());
        assertOpportunityVisibleToActor(actor, opportunity);

        OpportunityStage fromStage = opportunity.currentStage();
        OpportunityStage toStage = resolveRequiredStage(command.toStage());
        assertTransitionAllowed(actor, fromStage, toStage);
        LostReason lostReason = resolveLostReasonForStage(toStage, command.lostReason());
        String lostNote = normalizeOptional(command.lostNote());
        String reason = normalizeOptional(command.reason());
        validateStageChangeBusinessRules(opportunity, fromStage, toStage, lostReason, lostNote, reason);

        Instant now = timeProvider.now();
        Map<String, Object> beforeData = admissionOpportunityMapper.toAuditData(opportunity);
        StageHistory stageHistory = StageHistory.create(
                UUID.randomUUID(),
                tenantId,
                opportunity.id(),
                fromStage,
                toStage,
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                now,
                reason,
                durationInPreviousStageSeconds(opportunity, now)
        );
        StageHistory savedStageHistory = stageHistoryRepository.save(stageHistory);
        opportunity.changeStage(toStage, lostReason, lostNote, now);
        AdmissionOpportunity savedOpportunity = admissionOpportunityRepository.save(opportunity);
        auditStageChange(savedOpportunity, actor, savedStageHistory, beforeData, reason);

        return new StageChangeSnapshot(
                savedOpportunity.id(),
                fromStage.name(),
                toStage.name(),
                savedStageHistory.id(),
                savedStageHistory.changedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityAssignmentSnapshot getAssignmentSnapshot(UUID tenantId, UUID opportunityId) {
        requireId(tenantId, "tenant_id", "Tenant id is required");
        AdmissionOpportunity opportunity = findOpportunity(tenantId, opportunityId);
        return toAssignmentSnapshot(opportunity);
    }

    @Override
    @Transactional
    public OpportunityAssignmentSnapshot assignOwner(AssignOpportunityOwnerCommand command) {
        validateAssignOwnerCommand(command);
        Instant now = timeProvider.now();
        AdmissionOpportunity opportunity = findOpportunity(command.tenantId(), command.opportunityId());
        User owner = userRepository.findByIdAndTenantId(command.ownerId(), command.tenantId())
                .orElseThrow(() -> BusinessException.notFound("owner_id", "Owner user not found"));
        validateAssignableOwner(owner);

        Map<String, Object> beforeData = admissionOpportunityMapper.toAuditData(opportunity);
        UUID fromOwnerId = opportunity.ownerId();
        opportunity.assignOwner(owner.id(), now);
        AdmissionOpportunity savedOpportunity = admissionOpportunityRepository.save(opportunity);
        auditOpportunityAssignment(savedOpportunity, currentUserOrSystem(command.assignedBy(), command.tenantId()), beforeData, fromOwnerId, command);
        return toAssignmentSnapshot(savedOpportunity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countActiveOwnedOpportunities(UUID tenantId, Collection<UUID> ownerIds) {
        requireId(tenantId, "tenant_id", "Tenant id is required");
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> workloads = new LinkedHashMap<>();
        ownerIds.forEach(ownerId -> workloads.put(ownerId, 0L));
        admissionOpportunityRepository.countActiveWorkloads(tenantId, ownerIds, ACTIVE_DUPLICATE_STAGES)
                .forEach(row -> workloads.put(row.getOwnerId(), row.getWorkload()));
        return workloads;
    }

    private AdmissionOpportunity createNewOpportunity(
            UUID tenantId,
            UUID customerId,
            UUID sourceLeadId,
            CatalogSelection selection,
            UUID branchId,
            UUID ownerId,
            LeadTemperature leadTemperature,
            Instant now) {
        AdmissionOpportunity opportunity = AdmissionOpportunity.create(
                UUID.randomUUID(),
                tenantId,
                customerId,
                sourceLeadId,
                selection.languageId(),
                selection.programId(),
                selection.courseId(),
                branchId,
                ownerId,
                leadTemperature,
                now
        );
        AdmissionOpportunity savedOpportunity = admissionOpportunityRepository.save(opportunity);
        auditOpportunityChange(
                AuditActions.OPPORTUNITY_CREATED,
                savedOpportunity,
                currentUserOrSystem(),
                null,
                admissionOpportunityMapper.toAuditData(savedOpportunity),
                null
        );
        return savedOpportunity;
    }

    private Touchpoint createTouchpoint(
            UUID tenantId,
            UUID customerId,
            Lead lead,
            UUID opportunityId,
            Instant touchTime,
            TouchpointType touchpointType,
            Instant now) {
        Touchpoint touchpoint = Touchpoint.create(
                UUID.randomUUID(),
                tenantId,
                customerId,
                lead.id(),
                opportunityId,
                lead.source(),
                lead.campaign(),
                lead.adset(),
                lead.ad(),
                lead.utmSource(),
                lead.utmMedium(),
                lead.utmCampaign(),
                touchTime,
                touchpointType,
                now
        );
        return touchpointRepository.save(touchpoint);
    }

    private void validateCreateCommand(CreateAdmissionOpportunityCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity creation command is required");
        }
        requireId(command.tenantId(), "tenant_id", "Tenant id is required");
        requireId(command.customerId(), "customer_id", "Customer id is required");
        requireId(command.sourceLeadId(), "source_lead_id", "Source lead id is required");
    }

    private void validateUpdateCommand(UpdateAdmissionOpportunityCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity update command is required");
        }
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
    }

    private void validateChangeStageCommand(ChangeOpportunityStageCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity stage change command is required");
        }
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
        if (!StringUtils.hasText(command.toStage())) {
            throw ValidationException.of("to_stage", "REQUIRED", "Target stage is required");
        }
    }

    private void validateCreateActivityCommand(CreateOpportunityActivityCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity activity command is required");
        }
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
        if (!StringUtils.hasText(command.activityType())) {
            throw ValidationException.of("activity_type", "REQUIRED", "Activity type is required");
        }
        if (command.occurredAt() == null) {
            throw ValidationException.of("occurred_at", "REQUIRED", "Activity occurred time is required");
        }
        if (!StringUtils.hasText(command.source())) {
            throw ValidationException.of("source", "REQUIRED", "Activity source is required");
        }
    }

    private void validateAssignOwnerCommand(AssignOpportunityOwnerCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Opportunity assignment command is required");
        }
        requireId(command.tenantId(), "tenant_id", "Tenant id is required");
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
        requireId(command.ownerId(), "owner_id", "Owner id is required");
    }

    private Lead findLead(UUID tenantId, UUID leadId) {
        return leadRepository.findByIdAndTenantId(leadId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("source_lead_id", "Lead not found"));
    }

    private CustomerProfile findCustomer(UUID tenantId, UUID customerId) {
        return customerProfileRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("customer_id", "Customer profile not found"));
    }

    private AdmissionOpportunity findOpportunity(UUID tenantId, UUID opportunityId) {
        if (opportunityId == null) {
            throw ValidationException.of("opportunity_id", "REQUIRED", "Opportunity id is required");
        }
        return admissionOpportunityRepository.findByIdAndTenantId(opportunityId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("opportunity_id", "Opportunity not found"));
    }

    private void assertLeadCustomerCompatible(Lead lead, CustomerProfile customer) {
        if (lead.customerId() != null && !lead.customerId().equals(customer.id())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Lead is already linked to another customer",
                    List.of(ErrorDetail.of("customer_id", "LEAD_CUSTOMER_MISMATCH", "Lead is already linked to another customer"))
            );
        }
    }

    private java.util.Optional<AdmissionOpportunity> findReusableOpportunity(
            UUID tenantId,
            UUID customerId,
            UUID programId) {
        if (programId == null) {
            return java.util.Optional.empty();
        }
        return admissionOpportunityRepository.findFirstByTenantIdAndCustomerIdAndProgramIdAndCurrentStageInOrderByCreatedAtDesc(
                tenantId,
                customerId,
                programId,
                ACTIVE_DUPLICATE_STAGES
        );
    }

    private CatalogSelection resolveCreateSelection(UUID tenantId, Lead lead, CreateAdmissionOpportunityCommand command) {
        return resolveCatalogSelection(
                tenantId,
                firstNonNull(command.languageId(), lead.languageId()),
                firstNonNull(command.programId(), lead.programId()),
                firstNonNull(command.courseId(), null),
                true
        );
    }

    private CatalogSelection resolveUpdateSelection(
            UUID tenantId,
            AdmissionOpportunity opportunity,
            UpdateAdmissionOpportunityCommand command) {
        boolean catalogChanged = command.languageId() != null || command.programId() != null || command.courseId() != null;
        if (!catalogChanged) {
            return new CatalogSelection(opportunity.languageId(), opportunity.programId(), opportunity.courseId());
        }
        return resolveCatalogSelection(
                tenantId,
                firstNonNull(command.languageId(), opportunity.languageId()),
                firstNonNull(command.programId(), opportunity.programId()),
                firstNonNull(command.courseId(), opportunity.courseId()),
                true
        );
    }

    private CatalogSelection resolveCatalogSelection(
            UUID tenantId,
            UUID languageId,
            UUID programId,
            UUID courseId,
            boolean requireActive) {
        UUID resolvedProgramId = programId;
        UUID resolvedLanguageId = languageId;
        UUID resolvedCourseId = courseId;
        if (resolvedCourseId != null) {
            Course course = findCourse(tenantId, resolvedCourseId);
            if (requireActive && course.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("course_id", "Course is inactive");
            }
            if (resolvedProgramId != null && !resolvedProgramId.equals(course.programId())) {
                throw ValidationException.of("course_id", "PROGRAM_MISMATCH", "Course does not belong to selected program");
            }
            resolvedProgramId = course.programId();
        }
        if (resolvedProgramId != null) {
            Program program = findProgram(tenantId, resolvedProgramId);
            if (requireActive && program.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("program_id", "Program is inactive");
            }
            if (resolvedLanguageId != null && !resolvedLanguageId.equals(program.languageId())) {
                throw ValidationException.of("program_id", "LANGUAGE_MISMATCH", "Program does not belong to selected language");
            }
            resolvedLanguageId = program.languageId();
        }
        if (resolvedLanguageId != null) {
            Language language = findLanguage(tenantId, resolvedLanguageId);
            if (requireActive && language.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("language_id", "Language is inactive");
            }
        }
        return new CatalogSelection(resolvedLanguageId, resolvedProgramId, resolvedCourseId);
    }

    private Language findLanguage(UUID tenantId, UUID languageId) {
        return languageRepository.findByIdAndTenantId(languageId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("language_id", "Language not found"));
    }

    private Program findProgram(UUID tenantId, UUID programId) {
        return programRepository.findByIdAndTenantId(programId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("program_id", "Program not found"));
    }

    private Course findCourse(UUID tenantId, UUID courseId) {
        return courseRepository.findByIdAndTenantId(courseId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("course_id", "Course not found"));
    }

    private UUID resolveBranchId(UUID tenantId, UUID branchId, boolean requireActive) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("branch_id", "Branch not found"));
        if (requireActive && branch.status() != BranchStatus.ACTIVE) {
            throw inactiveParent("branch_id", "Branch is inactive");
        }
        return branch.id();
    }

    private UUID resolveOwnerId(UUID tenantId, UUID ownerId, boolean requireActive) {
        if (ownerId == null) {
            return null;
        }
        User user = userRepository.findByIdAndTenantId(ownerId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("owner_id", "Owner user not found"));
        if (requireActive && user.status() != UserStatus.ACTIVE) {
            throw inactiveParent("owner_id", "Owner user is inactive");
        }
        return user.id();
    }

    private void validateAssignableOwner(User owner) {
        if (owner.status() != UserStatus.ACTIVE) {
            throw inactiveParent("owner_id", "Owner user is inactive");
        }
        if (!"ADVISOR".equals(owner.roleCode())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Owner must be an advisor",
                    List.of(ErrorDetail.of("owner_id", "ADVISOR_ROLE_REQUIRED", "Owner must be an advisor"))
            );
        }
    }

    private OpportunityStage resolveStage(String requestedStage) {
        if (!StringUtils.hasText(requestedStage)) {
            return null;
        }
        try {
            return OpportunityStage.valueOf(normalizeEnum(requestedStage));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("stage", "INVALID_STAGE", "Opportunity stage is invalid");
        }
    }

    private OpportunityStage resolveRequiredStage(String requestedStage) {
        try {
            return OpportunityStage.valueOf(normalizeEnum(requestedStage));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("to_stage", "INVALID_STAGE", "Target stage is invalid");
        }
    }

    private LostReason resolveLostReasonForStage(OpportunityStage toStage, String requestedLostReason) {
        if (toStage != OpportunityStage.LOST) {
            return null;
        }
        if (!StringUtils.hasText(requestedLostReason)) {
            throw new BusinessException(
                    ErrorCode.LOST_REASON_REQUIRED,
                    ErrorCode.LOST_REASON_REQUIRED.defaultMessage(),
                    List.of(ErrorDetail.of("lost_reason", "REQUIRED", "Lost reason is required"))
            );
        }
        try {
            return LostReason.valueOf(normalizeEnum(requestedLostReason));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("lost_reason", "INVALID_LOST_REASON", "Lost reason is invalid");
        }
    }

    private QualificationStatus resolveQualificationStatus(
            String requestedStatus,
            QualificationStatus currentStatus) {
        if (requestedStatus == null) {
            return currentStatus;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw ValidationException.of("qualification_status", "INVALID_QUALIFICATION_STATUS", "Qualification status is invalid");
        }
        try {
            return QualificationStatus.valueOf(normalizeEnum(requestedStatus));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("qualification_status", "INVALID_QUALIFICATION_STATUS", "Qualification status is invalid");
        }
    }

    private TouchpointType toTouchpointType(LeadSourceType sourceType) {
        if (sourceType == null) {
            return TouchpointType.MANUAL;
        }
        return switch (sourceType) {
            case CSV, GOOGLE_SHEET -> TouchpointType.IMPORT;
            case WEBSITE_FORM -> TouchpointType.FORM;
            case META_LEAD_ADS -> TouchpointType.META;
            case MANUAL, OTHER -> TouchpointType.MANUAL;
        };
    }

    private ActivityType resolveActivityType(String requestedType) {
        try {
            return ActivityType.valueOf(normalizeEnum(requestedType));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("activity_type", "INVALID_ACTIVITY_TYPE", "Activity type is invalid");
        }
    }

    private ActivityResult resolveActivityResult(ActivityType activityType, String requestedResult) {
        if (!StringUtils.hasText(requestedResult)) {
            if (activityType.requiresResult()) {
                throw ValidationException.of("activity_result", "REQUIRED", "Activity result is required for outbound activity");
            }
            return null;
        }
        try {
            return ActivityResult.valueOf(normalizeEnum(requestedResult));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("activity_result", "INVALID_ACTIVITY_RESULT", "Activity result is invalid");
        }
    }

    private ActivitySource resolveActivitySource(String requestedSource) {
        try {
            return ActivitySource.valueOf(normalizeEnum(requestedSource));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("source", "INVALID_ACTIVITY_SOURCE", "Activity source is invalid");
        }
    }

    private void validateActivityTiming(Instant occurredAt, Instant nextActionAt) {
        if (nextActionAt != null && nextActionAt.isBefore(occurredAt)) {
            throw ValidationException.of("next_action_at", "BEFORE_OCCURRED_AT", "Next action time must be after occurred time");
        }
    }

    private UUID resolveSearchOwner(CurrentUser actor, UUID requestedOwnerId) {
        if (isAdvisor(actor)) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(actor.actorId())) {
                throw BusinessException.forbidden("owner_id", "OWN_SCOPE_REQUIRED", "Advisor can only view own opportunities");
            }
            return actor.actorId();
        }
        return requestedOwnerId;
    }

    private void assertTransitionAllowed(CurrentUser actor, OpportunityStage fromStage, OpportunityStage toStage) {
        Set<OpportunityStage> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(fromStage, Set.of());
        if (!allowedTargets.contains(toStage)) {
            throw invalidStageTransition(fromStage, toStage);
        }
        if (fromStage == OpportunityStage.LOST && !LOST_REOPEN_ROLES.contains(actor.roleCode())) {
            throw BusinessException.forbidden("stage", "REOPEN_SCOPE_REQUIRED", "Only Sales Lead or Admin can reopen lost opportunities");
        }
    }

    private void validateStageChangeBusinessRules(
            AdmissionOpportunity opportunity,
            OpportunityStage fromStage,
            OpportunityStage toStage,
            LostReason lostReason,
            String lostNote,
            String reason) {
        if (toStage == OpportunityStage.LOST && lostReason == LostReason.OTHER && !StringUtils.hasText(lostNote)) {
            throw new BusinessException(
                    ErrorCode.LOST_REASON_REQUIRED,
                    "Lost note is required when lost reason is OTHER",
                    List.of(ErrorDetail.of("lost_note", "LOST_NOTE_REQUIRED", "Lost note is required when lost reason is OTHER"))
            );
        }
        if (fromStage == OpportunityStage.LOST && !StringUtils.hasText(reason)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Reopen reason is required",
                    List.of(ErrorDetail.of("reason", "REOPEN_REASON_REQUIRED", "Reopen reason is required"))
            );
        }
        if (toStage == OpportunityStage.PROGRAM_SELECTED && opportunity.programId() == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Program is required before selecting program stage",
                    List.of(ErrorDetail.of("program_id", "PROGRAM_REQUIRED", "Program is required before selecting program stage"))
            );
        }
    }

    private void assertOpportunityVisibleToActor(CurrentUser actor, AdmissionOpportunity opportunity) {
        if (isAdvisor(actor) && !actor.actorId().equals(opportunity.ownerId())) {
            throw BusinessException.forbidden("opportunity_id", "OWN_SCOPE_REQUIRED", "Advisor can only access own opportunities");
        }
    }

    private boolean isAdvisor(CurrentUser actor) {
        return actor != null && "ADVISOR".equals(actor.roleCode());
    }

    private Long durationInPreviousStageSeconds(AdmissionOpportunity opportunity, Instant changedAt) {
        Optional<StageHistory> latestHistory = stageHistoryRepository
                .findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(opportunity.tenantId(), opportunity.id());
        Instant previousChangedAt = latestHistory
                .map(StageHistory::changedAt)
                .orElse(opportunity.createdAt());
        long seconds = Duration.between(previousChangedAt, changedAt).getSeconds();
        return Math.max(seconds, 0L);
    }

    private Sort newestFirstSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private String normalizeEnum(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private UUID requireId(UUID id, String field, String message) {
        if (id == null) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return id;
    }

    private BusinessException inactiveParent(String field, String message) {
        return new BusinessException(
                ErrorCode.INVALID_PARENT_STATUS,
                message,
                List.of(ErrorDetail.of(field, "INACTIVE", message))
        );
    }



    private BusinessException invalidStageTransition(OpportunityStage fromStage, OpportunityStage toStage) {
        return new BusinessException(
                ErrorCode.INVALID_STAGE_TRANSITION,
                "Stage transition is not allowed",
                List.of(
                        ErrorDetail.of("from_stage", fromStage.name(), "Current stage cannot transition to target stage"),
                        ErrorDetail.of("to_stage", toStage.name(), "Target stage is not allowed")
                )
        );
    }


    private CurrentUser currentUserOrSystem() {
        try {
            return currentUserContext.currentUser();
        } catch (RuntimeException exception) {
            return new CurrentUser(null, null, "SYSTEM", Set.of(), LogContext.requestId());
        }
    }

    private CurrentUser currentUserOrSystem(UUID fallbackActorId, UUID fallbackTenantId) {
        try {
            return currentUserContext.currentUser();
        } catch (RuntimeException exception) {
            return new CurrentUser(fallbackActorId, fallbackTenantId, "SYSTEM", Set.of(), LogContext.requestId());
        }
    }

    private void auditOpportunityChange(
            String action,
            AdmissionOpportunity opportunity,
            CurrentUser actor,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                opportunity.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                action,
                AuditResourceTypes.ADMISSION_OPPORTUNITY,
                opportunity.id(),
                opportunity.id().toString(),
                beforeData,
                afterData,
                auditMetadata(actor),
                reason,
                LogContext.requestId()
        ));
    }

    private void auditStageChange(
            AdmissionOpportunity opportunity,
            CurrentUser actor,
            StageHistory stageHistory,
            Map<String, Object> beforeData,
            String reason) {
        Map<String, Object> metadata = auditMetadata(actor);
        metadata.put("stage_history_id", stageHistory.id().toString());
        metadata.put("from_stage", stageHistory.fromStage().name());
        metadata.put("to_stage", stageHistory.toStage().name());
        metadata.put("duration_in_previous_stage_seconds", stageHistory.durationInPreviousStageSeconds());
        auditService.record(new AuditRecordCommand(
                opportunity.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                AuditActions.OPPORTUNITY_STAGE_CHANGED,
                AuditResourceTypes.ADMISSION_OPPORTUNITY,
                opportunity.id(),
                opportunity.id().toString(),
                beforeData,
                admissionOpportunityMapper.toAuditData(opportunity),
                metadata,
                reason,
                LogContext.requestId()
        ));
    }

    private void auditTouchpointCreated(AdmissionOpportunity opportunity, Touchpoint touchpoint) {
        CurrentUser actor = currentUserOrSystem();
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("touchpoint_id", touchpoint.id().toString());
        afterData.put("opportunity_id", opportunity.id().toString());
        afterData.put("lead_id", touchpoint.leadId().toString());
        afterData.put("customer_id", touchpoint.customerId().toString());
        afterData.put("touch_type", touchpoint.touchType().name());
        afterData.put("touch_time", touchpoint.touchTime().toString());
        auditService.record(new AuditRecordCommand(
                opportunity.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                AuditActions.TOUCHPOINT_CREATED,
                AuditResourceTypes.TOUCHPOINT,
                touchpoint.id(),
                touchpoint.id().toString(),
                null,
                afterData,
                auditMetadata(actor),
                null,
                LogContext.requestId()
        ));
    }

    private void auditActivityCreated(Activity activity, CurrentUser actor) {
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("activity_id", activity.id().toString());
        afterData.put("opportunity_id", activity.opportunityId().toString());
        afterData.put("customer_id", activity.customerId().toString());
        afterData.put("actor_id", activity.actorId() == null ? null : activity.actorId().toString());
        afterData.put("actor_type", activity.actorType().name());
        afterData.put("activity_type", activity.activityType().name());
        afterData.put("activity_result", activity.activityResult() == null ? null : activity.activityResult().name());
        afterData.put("occurred_at", activity.occurredAt().toString());
        afterData.put("next_action_at", activity.nextActionAt() == null ? null : activity.nextActionAt().toString());
        afterData.put("source", activity.source().name());
        afterData.put("first_response_candidate", activity.firstResponseCandidate());
        afterData.put("contact_success", activity.contactSuccess());
        afterData.put("note_present", activity.note() != null);
        auditService.record(new AuditRecordCommand(
                activity.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                AuditActions.ACTIVITY_CREATED,
                AuditResourceTypes.ACTIVITY,
                activity.id(),
                activity.id().toString(),
                null,
                afterData,
                auditMetadata(actor),
                null,
                LogContext.requestId()
        ));
    }

    private void auditOpportunityAssignment(
            AdmissionOpportunity opportunity,
            CurrentUser actor,
            Map<String, Object> beforeData,
            UUID fromOwnerId,
            AssignOpportunityOwnerCommand command) {
        Map<String, Object> metadata = auditMetadata(actor);
        metadata.put("assignment_rule_id", command.assignmentRuleId() == null ? null : command.assignmentRuleId().toString());
        metadata.put("assignment_source", command.assignmentSource());
        metadata.put("assignment_strategy", command.assignmentStrategy());
        metadata.put("from_owner_id", fromOwnerId == null ? null : fromOwnerId.toString());
        metadata.put("to_owner_id", command.ownerId().toString());
        auditService.record(new AuditRecordCommand(
                opportunity.tenantId(),
                actor.actorId(),
                actor.actorId() == null ? "SYSTEM" : "USER",
                actor.roleCode(),
                AuditActions.OPPORTUNITY_ASSIGNED,
                AuditResourceTypes.ADMISSION_OPPORTUNITY,
                opportunity.id(),
                opportunity.id().toString(),
                beforeData,
                admissionOpportunityMapper.toAuditData(opportunity),
                metadata,
                normalizeOptional(command.reason()),
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(CurrentUser actor) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (actor.tenantId() != null) {
            metadata.put("actor_tenant_id", actor.tenantId().toString());
        }
        return metadata;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred == null ? fallback : preferred;
    }

    private OpportunityAssignmentSnapshot toAssignmentSnapshot(AdmissionOpportunity opportunity) {
        return new OpportunityAssignmentSnapshot(
                opportunity.id(),
                opportunity.tenantId(),
                opportunity.sourceLeadId(),
                opportunity.languageId(),
                opportunity.programId(),
                opportunity.branchId(),
                opportunity.ownerId(),
                opportunity.currentStage().name(),
                opportunity.leadTemperature() == null ? null : opportunity.leadTemperature().name(),
                opportunity.createdAt(),
                opportunity.updatedAt()
        );
    }

    private record CatalogSelection(UUID languageId, UUID programId, UUID courseId) {
    }
}
