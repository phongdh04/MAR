package vn.mar.customer.service;

import java.time.Instant;
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
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.pagination.PageRequestFactory;
import vn.mar.common.search.SearchText;
import vn.mar.common.tenant.TenantContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.common.validation.EnumParser;
import vn.mar.customer.api.DuplicateCaseCreateCommand;
import vn.mar.customer.api.DuplicateCaseManagementService;
import vn.mar.customer.api.DuplicateCaseResolveCommand;
import vn.mar.customer.api.DuplicateCaseSearchCommand;
import vn.mar.customer.api.DuplicateCaseSnapshot;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.mapper.DuplicateCaseMapper;
import vn.mar.customer.model.CustomerIdentityType;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.model.DuplicateResolutionAction;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class DuplicateCaseService implements DuplicateCaseManagementService {

    private static final String DEFAULT_EMAIL_EXACT_REVIEW_REASON =
            "Email exact match with different phone identifier";

    private final DuplicateCaseRepository duplicateCaseRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerIdentityRepository customerIdentityRepository;
    private final DuplicateCaseMapper duplicateCaseMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final CustomerMergeService customerMergeService;
    private final PermissionGuard permissionGuard;

    public DuplicateCaseService(
            DuplicateCaseRepository duplicateCaseRepository,
            CustomerProfileRepository customerProfileRepository,
            CustomerIdentityRepository customerIdentityRepository,
            DuplicateCaseMapper duplicateCaseMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            CustomerMergeService customerMergeService,
            PermissionGuard permissionGuard) {
        this.duplicateCaseRepository = duplicateCaseRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerIdentityRepository = customerIdentityRepository;
        this.duplicateCaseMapper = duplicateCaseMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.customerMergeService = customerMergeService;
        this.permissionGuard = permissionGuard;
    }

    @Override
    @Transactional
    public DuplicateCaseSnapshot createEmailExactPhoneDifferentCase(DuplicateCaseCreateCommand command) {
        DuplicateCustomerPair customerPair = loadCustomerPair(command);
        assertEmailExactPhoneDifferent(customerPair);
        return createNeedsReviewCase(
                command,
                customerPair,
                DuplicateMatchType.EMAIL_EXACT_PHONE_DIFFERENT,
                resolveConfidence(command.confidence(), DuplicateConfidence.MEDIUM),
                defaultText(command.reviewReason(), DEFAULT_EMAIL_EXACT_REVIEW_REASON)
        );
    }

    @Override
    @Transactional
    public DuplicateCaseSnapshot createNearMatchCase(DuplicateCaseCreateCommand command) {
        DuplicateCustomerPair customerPair = loadCustomerPair(command);
        return createNeedsReviewCase(
                command,
                customerPair,
                DuplicateMatchType.NEAR_MATCH,
                resolveConfidence(command.confidence(), DuplicateConfidence.LOW),
                requireText(command.reviewReason(), "review_reason", "Near match review reason is required")
        );
    }

    @Override
    @Transactional
    public DuplicateCaseSnapshot resolveCase(DuplicateCaseResolveCommand command) {
        validateResolveCommand(command);
        DuplicateResolutionAction action = resolveAction(command.action());
        String reason = requireText(command.reason(), "reason", "Duplicate resolution reason is required");
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        permissionGuard.requirePermission(actor, PermissionCodes.DUPLICATE_MANAGE, "DUPLICATE_RESOLVE_DENIED", "Permission is required to resolve duplicate cases", "Permission denied");
        assertMergePermission(actor, action);

        DuplicateCase duplicateCase = duplicateCaseRepository.findByIdAndTenantId(
                        command.duplicateCaseId(),
                        tenantId
                )
                .orElseThrow(() -> BusinessException.notFound("duplicate_case_id", "Duplicate case not found"));
        if (duplicateCase.status() != DuplicateCaseStatus.NEEDS_REVIEW) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Duplicate case has already been resolved",
                    List.of(ErrorDetail.of("status", "ALREADY_RESOLVED", "Duplicate case has already been resolved"))
            );
        }

        CustomerProfile sourceCustomer = ensureCustomerExists(tenantId, duplicateCase.sourceCustomerId(), "source_customer_id");
        CustomerProfile matchedCustomer = ensureCustomerExists(tenantId, duplicateCase.matchedCustomerId(), "candidate_customer_id");
        CustomerProfile targetCustomer = null;
        CustomerProfile mergeSourceCustomer = null;
        if (action == DuplicateResolutionAction.MERGE) {
            targetCustomer = resolveMergeTarget(command.targetCustomerId(), sourceCustomer, matchedCustomer);
            mergeSourceCustomer = sourceCustomer.id().equals(targetCustomer.id()) ? matchedCustomer : sourceCustomer;
        }
        Map<String, Object> beforeData = duplicateCaseMapper.toAuditData(duplicateCase);
        duplicateCase.resolve(action, reason, actor.actorId(), timeProvider.now());
        DuplicateCase savedCase = duplicateCaseRepository.save(duplicateCase);
        if (action == DuplicateResolutionAction.MERGE) {
            customerMergeService.recordMerge(savedCase, mergeSourceCustomer, targetCustomer, actor, reason);
        }
        auditResolved(savedCase, beforeData, actor, reason);
        return duplicateCaseMapper.toSnapshot(savedCase);
    }

    @Override
    @Transactional(readOnly = true)
    public DuplicateCaseSnapshot findCase(UUID duplicateCaseId) {
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.DUPLICATE_MANAGE, "DUPLICATE_VIEW_DENIED", "Permission is required to view duplicate cases", "Permission denied");
        return findCase(TenantContext.requireTenantId(actor), duplicateCaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DuplicateCaseSnapshot findCase(UUID tenantId, UUID duplicateCaseId) {
        if (tenantId == null) {
            throw ValidationException.of("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (duplicateCaseId == null) {
            throw ValidationException.of("duplicate_case_id", "REQUIRED", "Duplicate case id is required");
        }
        return duplicateCaseRepository.findByIdAndTenantId(duplicateCaseId, tenantId)
                .map(duplicateCaseMapper::toSnapshot)
                .orElseThrow(() -> BusinessException.notFound("duplicate_case_id", "Duplicate case not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DuplicateCaseSnapshot> searchCases(DuplicateCaseSearchCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Duplicate case search command is required");
        }
        CurrentUser actor = currentUserContext.currentUser();
        permissionGuard.requirePermission(actor, PermissionCodes.DUPLICATE_MANAGE, "DUPLICATE_VIEW_DENIED", "Permission is required to view duplicate cases", "Permission denied");
        UUID tenantId = TenantContext.requireTenantId(actor);
        DuplicateCaseStatus status = resolveStatus(command.status());
        DuplicateMatchType matchType = resolveMatchType(command.matchType());
        PageRequest pageable = PageRequestFactory.of(command.page(), command.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DuplicateCaseSnapshot> responsePage = duplicateCaseRepository.search(tenantId, status, matchType, pageable)
                .map(duplicateCaseMapper::toSnapshot);
        return PageResponse.from(responsePage);
    }

    private DuplicateCaseSnapshot createNeedsReviewCase(
            DuplicateCaseCreateCommand command,
            DuplicateCustomerPair customerPair,
            DuplicateMatchType matchType,
            DuplicateConfidence confidence,
            String reviewReason) {
        return duplicateCaseRepository.findOpenCaseForPair(
                        command.tenantId(),
                        customerPair.sourceCustomer().id(),
                        customerPair.matchedCustomer().id(),
                        matchType,
                        DuplicateCaseStatus.NEEDS_REVIEW
                )
                .map(duplicateCaseMapper::toSnapshot)
                .orElseGet(() -> createNewCase(command, customerPair, matchType, confidence, reviewReason));
    }

    private DuplicateCaseSnapshot createNewCase(
            DuplicateCaseCreateCommand command,
            DuplicateCustomerPair customerPair,
            DuplicateMatchType matchType,
            DuplicateConfidence confidence,
            String reviewReason) {
        Instant now = timeProvider.now();
        DuplicateCase duplicateCase = DuplicateCase.create(
                UUID.randomUUID(),
                command.tenantId(),
                customerPair.sourceCustomer().id(),
                customerPair.matchedCustomer().id(),
                matchType,
                confidence,
                reviewReason,
                now
        );
        DuplicateCase savedCase = duplicateCaseRepository.save(duplicateCase);
        auditCreated(savedCase, command);
        return duplicateCaseMapper.toSnapshot(savedCase);
    }

    private DuplicateCustomerPair loadCustomerPair(DuplicateCaseCreateCommand command) {
        validateCreateCommand(command);
        if (command.sourceCustomerId().equals(command.matchedCustomerId())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Duplicate case requires two different customers",
                    List.of(ErrorDetail.of("matched_customer_id", "SAME_CUSTOMER", "Cannot create duplicate case for the same customer"))
            );
        }
        CustomerProfile sourceCustomer = ensureCustomerExists(
                command.tenantId(),
                command.sourceCustomerId(),
                "source_customer_id"
        );
        CustomerProfile matchedCustomer = ensureCustomerExists(
                command.tenantId(),
                command.matchedCustomerId(),
                "matched_customer_id"
        );
        return new DuplicateCustomerPair(sourceCustomer, matchedCustomer);
    }

    private void validateCreateCommand(DuplicateCaseCreateCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Duplicate case command is required");
        }
        if (command.tenantId() == null) {
            throw ValidationException.of("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (command.sourceCustomerId() == null) {
            throw ValidationException.of("source_customer_id", "REQUIRED", "Source customer id is required");
        }
        if (command.matchedCustomerId() == null) {
            throw ValidationException.of("matched_customer_id", "REQUIRED", "Matched customer id is required");
        }
    }

    private void validateResolveCommand(DuplicateCaseResolveCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Duplicate case resolve command is required");
        }
        if (command.duplicateCaseId() == null) {
            throw ValidationException.of("duplicate_case_id", "REQUIRED", "Duplicate case id is required");
        }
    }

    private CustomerProfile resolveMergeTarget(
            UUID targetCustomerId,
            CustomerProfile sourceCustomer,
            CustomerProfile matchedCustomer) {
        if (targetCustomerId == null) {
            throw ValidationException.of("target_customer_id", "REQUIRED", "Target customer id is required for merge");
        }
        if (targetCustomerId.equals(sourceCustomer.id())) {
            return sourceCustomer;
        }
        if (targetCustomerId.equals(matchedCustomer.id())) {
            return matchedCustomer;
        }
        throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "Merge target must belong to the duplicate case",
                List.of(ErrorDetail.of("target_customer_id", "TARGET_NOT_IN_DUPLICATE_CASE", "Target customer must belong to the duplicate case"))
        );
    }

    private void assertEmailExactPhoneDifferent(DuplicateCustomerPair customerPair) {
        List<CustomerIdentity> sourceIdentities = customerIdentityRepository.findByTenantIdAndCustomerId(
                customerPair.sourceCustomer().tenantId(),
                customerPair.sourceCustomer().id()
        );
        List<CustomerIdentity> matchedIdentities = customerIdentityRepository.findByTenantIdAndCustomerId(
                customerPair.matchedCustomer().tenantId(),
                customerPair.matchedCustomer().id()
        );
        Set<String> sourceEmails = identityValues(sourceIdentities, CustomerIdentityType.EMAIL);
        Set<String> matchedEmails = identityValues(matchedIdentities, CustomerIdentityType.EMAIL);
        addCanonicalValue(sourceEmails, customerPair.sourceCustomer().primaryEmail());
        addCanonicalValue(matchedEmails, customerPair.matchedCustomer().primaryEmail());

        if (!hasIntersection(sourceEmails, matchedEmails)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Email exact match is required for this duplicate case type",
                    List.of(ErrorDetail.of("email", "EMAIL_EXACT_REQUIRED", "Email exact match is required"))
            );
        }

        Set<String> sourcePhones = identityValues(sourceIdentities, CustomerIdentityType.PHONE);
        Set<String> matchedPhones = identityValues(matchedIdentities, CustomerIdentityType.PHONE);
        addCanonicalValue(sourcePhones, customerPair.sourceCustomer().primaryPhone());
        addCanonicalValue(matchedPhones, customerPair.matchedCustomer().primaryPhone());
        if (sourcePhones.isEmpty() || matchedPhones.isEmpty() || hasIntersection(sourcePhones, matchedPhones)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Different phone identifiers are required for this duplicate case type",
                    List.of(ErrorDetail.of("phone", "PHONE_DIFFERENT_REQUIRED", "Phone identifiers must be present and different"))
            );
        }
    }

    private Set<String> identityValues(List<CustomerIdentity> identities, CustomerIdentityType identityType) {
        return identities.stream()
                .filter(identity -> identity.identityType() == identityType)
                .map(CustomerIdentity::normalizedValue)
                .filter(StringUtils::hasText)
                .map(this::canonicalValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void addCanonicalValue(Set<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(canonicalValue(value));
        }
    }

    private boolean hasIntersection(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private CustomerProfile ensureCustomerExists(UUID tenantId, UUID customerId, String field) {
        return customerProfileRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> BusinessException.notFound(field, "Customer profile not found"));
    }

    private DuplicateConfidence resolveConfidence(DuplicateConfidence requestedConfidence, DuplicateConfidence defaultConfidence) {
        return requestedConfidence == null ? defaultConfidence : requestedConfidence;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String requireText(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return value.trim();
    }

    private String canonicalValue(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private DuplicateResolutionAction resolveAction(String requestedAction) {
        if (!StringUtils.hasText(requestedAction)) {
            throw ValidationException.of("action", "REQUIRED", "Resolution action is required");
        }
        return EnumParser.optionalEnum(DuplicateResolutionAction.class, requestedAction, "action", "INVALID_ACTION", "Resolution action is invalid");
    }

    private DuplicateCaseStatus resolveStatus(String requestedStatus) {
        return EnumParser.optionalEnum(DuplicateCaseStatus.class, requestedStatus, "status", "INVALID_STATUS", "Duplicate case status is invalid");
    }

    private DuplicateMatchType resolveMatchType(String requestedMatchType) {
        return EnumParser.optionalEnum(DuplicateMatchType.class, requestedMatchType, "match_type", "INVALID_MATCH_TYPE", "Duplicate match type is invalid");
    }



    private void assertMergePermission(CurrentUser actor, DuplicateResolutionAction action) {
        if (action == DuplicateResolutionAction.MERGE) {
            permissionGuard.requirePermission(actor, PermissionCodes.CUSTOMER_MERGE, "CUSTOMER_MERGE_DENIED", "Permission is required to merge customers", "Permission denied");
        }
    }

    private void auditCreated(DuplicateCase duplicateCase, DuplicateCaseCreateCommand command) {
        auditService.record(new AuditRecordCommand(
                duplicateCase.tenantId(),
                command.actorId(),
                actorType(command.actorId()),
                normalizeOptionalRole(command.actorRole()),
                AuditActions.DUPLICATE_CASE_CREATED,
                AuditResourceTypes.DUPLICATE_CASE,
                duplicateCase.id(),
                duplicateCase.id().toString(),
                null,
                duplicateCaseMapper.toAuditData(duplicateCase),
                auditMetadata(duplicateCase),
                duplicateCase.reviewReason(),
                LogContext.requestId()
        ));
    }

    private void auditResolved(
            DuplicateCase duplicateCase,
            Map<String, Object> beforeData,
            CurrentUser actor,
            String reason) {
        auditService.record(new AuditRecordCommand(
                duplicateCase.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                AuditActions.DUPLICATE_CASE_RESOLVED,
                AuditResourceTypes.DUPLICATE_CASE,
                duplicateCase.id(),
                duplicateCase.id().toString(),
                beforeData,
                duplicateCaseMapper.toAuditData(duplicateCase),
                auditMetadata(duplicateCase),
                reason,
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(DuplicateCase duplicateCase) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("match_type", duplicateCase.matchType().name());
        metadata.put("confidence", duplicateCase.confidence().name());
        metadata.put("status", duplicateCase.status().name());
        metadata.put("source_customer_id", duplicateCase.sourceCustomerId().toString());
        metadata.put("matched_customer_id", duplicateCase.matchedCustomerId().toString());
        if (duplicateCase.resolutionAction() != null) {
            metadata.put("resolution_action", duplicateCase.resolutionAction().name());
        }
        return metadata;
    }

    private String actorType(UUID actorId) {
        return actorId == null ? "SYSTEM" : "USER";
    }

    private String normalizeOptionalRole(String role) {
        return SearchText.upperOrNull(role);
    }



    private record DuplicateCustomerPair(
            CustomerProfile sourceCustomer,
            CustomerProfile matchedCustomer
    ) {
    }
}
