package vn.mar.opportunity.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.entity.Lead;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.opportunity.api.AdmissionOpportunityManagementService;
import vn.mar.opportunity.api.AdmissionOpportunitySearchCommand;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.CreateAdmissionOpportunityCommand;
import vn.mar.opportunity.api.UpdateAdmissionOpportunityCommand;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.entity.Touchpoint;
import vn.mar.opportunity.mapper.AdmissionOpportunityMapper;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.model.QualificationStatus;
import vn.mar.opportunity.model.TouchpointType;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;

@Service
public class AdmissionOpportunityService implements AdmissionOpportunityManagementService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
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

    private final AdmissionOpportunityRepository admissionOpportunityRepository;
    private final TouchpointRepository touchpointRepository;
    private final LeadRepository leadRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final LanguageRepository languageRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AdmissionOpportunityMapper admissionOpportunityMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    public AdmissionOpportunityService(
            AdmissionOpportunityRepository admissionOpportunityRepository,
            TouchpointRepository touchpointRepository,
            LeadRepository leadRepository,
            CustomerProfileRepository customerProfileRepository,
            LanguageRepository languageRepository,
            ProgramRepository programRepository,
            CourseRepository courseRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            AdmissionOpportunityMapper admissionOpportunityMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService) {
        this.admissionOpportunityRepository = admissionOpportunityRepository;
        this.touchpointRepository = touchpointRepository;
        this.leadRepository = leadRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.languageRepository = languageRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.admissionOpportunityMapper = admissionOpportunityMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
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
            throw validation("command", "REQUIRED", "Opportunity search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        assertAnyPermission(actor, List.of(PermissionCodes.LEAD_VIEW, PermissionCodes.OPPORTUNITY_UPDATE), "OPPORTUNITY_VIEW_DENIED", "Permission is required to view opportunities");
        UUID tenantId = requireTenantContext(actor);
        UUID ownerId = resolveSearchOwner(actor, command.ownerId());
        OpportunityStage stage = resolveStage(command.stage());
        PageRequest pageable = PageRequest.of(resolvePage(command.page()), resolveSize(command.size()), newestFirstSort());
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
        assertAnyPermission(actor, List.of(PermissionCodes.LEAD_VIEW, PermissionCodes.OPPORTUNITY_UPDATE), "OPPORTUNITY_VIEW_DENIED", "Permission is required to view opportunities");
        AdmissionOpportunity opportunity = findOpportunity(requireTenantContext(actor), opportunityId);
        assertOpportunityVisibleToActor(actor, opportunity);
        return admissionOpportunityMapper.toSnapshot(opportunity);
    }

    @Override
    @Transactional
    public AdmissionOpportunitySnapshot updateOpportunity(UpdateAdmissionOpportunityCommand command) {
        validateUpdateCommand(command);
        CurrentUser actor = currentUserContext.currentUser();
        assertPermission(actor, PermissionCodes.OPPORTUNITY_UPDATE, "OPPORTUNITY_UPDATE_DENIED", "Permission is required to update opportunities");
        UUID tenantId = requireTenantContext(actor);
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
            throw validation("command", "REQUIRED", "Opportunity creation command is required");
        }
        requireId(command.tenantId(), "tenant_id", "Tenant id is required");
        requireId(command.customerId(), "customer_id", "Customer id is required");
        requireId(command.sourceLeadId(), "source_lead_id", "Source lead id is required");
    }

    private void validateUpdateCommand(UpdateAdmissionOpportunityCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Opportunity update command is required");
        }
        requireId(command.opportunityId(), "opportunity_id", "Opportunity id is required");
    }

    private Lead findLead(UUID tenantId, UUID leadId) {
        return leadRepository.findByIdAndTenantId(leadId, tenantId)
                .orElseThrow(() -> notFound("source_lead_id", "Lead not found"));
    }

    private CustomerProfile findCustomer(UUID tenantId, UUID customerId) {
        return customerProfileRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> notFound("customer_id", "Customer profile not found"));
    }

    private AdmissionOpportunity findOpportunity(UUID tenantId, UUID opportunityId) {
        if (opportunityId == null) {
            throw validation("opportunity_id", "REQUIRED", "Opportunity id is required");
        }
        return admissionOpportunityRepository.findByIdAndTenantId(opportunityId, tenantId)
                .orElseThrow(() -> notFound("opportunity_id", "Opportunity not found"));
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
                throw validation("course_id", "PROGRAM_MISMATCH", "Course does not belong to selected program");
            }
            resolvedProgramId = course.programId();
        }
        if (resolvedProgramId != null) {
            Program program = findProgram(tenantId, resolvedProgramId);
            if (requireActive && program.status() != CatalogStatus.ACTIVE) {
                throw inactiveParent("program_id", "Program is inactive");
            }
            if (resolvedLanguageId != null && !resolvedLanguageId.equals(program.languageId())) {
                throw validation("program_id", "LANGUAGE_MISMATCH", "Program does not belong to selected language");
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
                .orElseThrow(() -> notFound("language_id", "Language not found"));
    }

    private Program findProgram(UUID tenantId, UUID programId) {
        return programRepository.findByIdAndTenantId(programId, tenantId)
                .orElseThrow(() -> notFound("program_id", "Program not found"));
    }

    private Course findCourse(UUID tenantId, UUID courseId) {
        return courseRepository.findByIdAndTenantId(courseId, tenantId)
                .orElseThrow(() -> notFound("course_id", "Course not found"));
    }

    private UUID resolveBranchId(UUID tenantId, UUID branchId, boolean requireActive) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> notFound("branch_id", "Branch not found"));
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
                .orElseThrow(() -> notFound("owner_id", "Owner user not found"));
        if (requireActive && user.status() != UserStatus.ACTIVE) {
            throw inactiveParent("owner_id", "Owner user is inactive");
        }
        return user.id();
    }

    private OpportunityStage resolveStage(String requestedStage) {
        if (!StringUtils.hasText(requestedStage)) {
            return null;
        }
        try {
            return OpportunityStage.valueOf(normalizeEnum(requestedStage));
        } catch (IllegalArgumentException exception) {
            throw validation("stage", "INVALID_STAGE", "Opportunity stage is invalid");
        }
    }

    private QualificationStatus resolveQualificationStatus(
            String requestedStatus,
            QualificationStatus currentStatus) {
        if (requestedStatus == null) {
            return currentStatus;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw validation("qualification_status", "INVALID_QUALIFICATION_STATUS", "Qualification status is invalid");
        }
        try {
            return QualificationStatus.valueOf(normalizeEnum(requestedStatus));
        } catch (IllegalArgumentException exception) {
            throw validation("qualification_status", "INVALID_QUALIFICATION_STATUS", "Qualification status is invalid");
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

    private UUID resolveSearchOwner(CurrentUser actor, UUID requestedOwnerId) {
        if (isAdvisor(actor)) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(actor.actorId())) {
                throw forbidden("owner_id", "OWN_SCOPE_REQUIRED", "Advisor can only view own opportunities");
            }
            return actor.actorId();
        }
        return requestedOwnerId;
    }

    private void assertOpportunityVisibleToActor(CurrentUser actor, AdmissionOpportunity opportunity) {
        if (isAdvisor(actor) && !actor.actorId().equals(opportunity.ownerId())) {
            throw forbidden("opportunity_id", "OWN_SCOPE_REQUIRED", "Advisor can only access own opportunities");
        }
    }

    private boolean isAdvisor(CurrentUser actor) {
        return actor != null && "ADVISOR".equals(actor.roleCode());
    }

    private void assertPermission(CurrentUser actor, String permissionCode, String detailCode, String message) {
        if (actor == null || !actor.hasPermission(permissionCode)) {
            throw forbidden("permission", detailCode, message);
        }
    }

    private void assertAnyPermission(CurrentUser actor, List<String> permissionCodes, String detailCode, String message) {
        if (actor == null || permissionCodes.stream().noneMatch(actor::hasPermission)) {
            throw forbidden("permission", detailCode, message);
        }
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private int resolvePage(Integer requestedPage) {
        if (requestedPage == null) {
            return DEFAULT_PAGE;
        }
        if (requestedPage < 0) {
            throw validation("page", "MIN_VALUE", "Page must be greater than or equal to 0");
        }
        return requestedPage;
    }

    private int resolveSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_SIZE;
        }
        if (requestedSize < 1 || requestedSize > MAX_SIZE) {
            throw validation("size", "INVALID_SIZE", "Size must be between 1 and 100");
        }
        return requestedSize;
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
            throw validation(field, "REQUIRED", message);
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

    private BusinessException notFound(String field, String message) {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                message,
                List.of(ErrorDetail.of(field, "NOT_FOUND", message))
        );
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

    private CurrentUser currentUserOrSystem() {
        try {
            return currentUserContext.currentUser();
        } catch (RuntimeException exception) {
            return new CurrentUser(null, null, "SYSTEM", Set.of(), LogContext.requestId());
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

    private record CatalogSelection(UUID languageId, UUID programId, UUID courseId) {
    }
}
