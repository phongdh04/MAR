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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.api.DuplicateCaseCreateCommand;
import vn.mar.customer.api.DuplicateCaseManagementService;
import vn.mar.customer.api.DuplicateCaseResolveCommand;
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
import vn.mar.role.model.RoleCode;

@Service
public class DuplicateCaseService implements DuplicateCaseManagementService {

    private static final String DEFAULT_EMAIL_EXACT_REVIEW_REASON =
            "Email exact match with different phone identifier";
    private static final Set<String> RESOLVE_ALLOWED_ROLES = Set.of(
            RoleCode.ADMIN.name(),
            RoleCode.SALES_LEAD.name()
    );

    private final DuplicateCaseRepository duplicateCaseRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerIdentityRepository customerIdentityRepository;
    private final DuplicateCaseMapper duplicateCaseMapper;
    private final TimeProvider timeProvider;
    private final AuditService auditService;

    public DuplicateCaseService(
            DuplicateCaseRepository duplicateCaseRepository,
            CustomerProfileRepository customerProfileRepository,
            CustomerIdentityRepository customerIdentityRepository,
            DuplicateCaseMapper duplicateCaseMapper,
            TimeProvider timeProvider,
            AuditService auditService) {
        this.duplicateCaseRepository = duplicateCaseRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerIdentityRepository = customerIdentityRepository;
        this.duplicateCaseMapper = duplicateCaseMapper;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
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
        String actorRole = normalizeRole(command.actorRole());
        assertResolvePermission(actorRole);
        String reason = requireText(command.reason(), "reason", "Duplicate resolution reason is required");

        DuplicateCase duplicateCase = duplicateCaseRepository.findByIdAndTenantId(
                        command.duplicateCaseId(),
                        command.tenantId()
                )
                .orElseThrow(() -> notFound("duplicate_case_id", "Duplicate case not found"));
        if (duplicateCase.status() != DuplicateCaseStatus.NEEDS_REVIEW) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Duplicate case has already been resolved",
                    List.of(ErrorDetail.of("status", "ALREADY_RESOLVED", "Duplicate case has already been resolved"))
            );
        }

        ensureCustomerExists(command.tenantId(), duplicateCase.sourceCustomerId(), "source_customer_id");
        ensureCustomerExists(command.tenantId(), duplicateCase.matchedCustomerId(), "matched_customer_id");
        Map<String, Object> beforeData = duplicateCaseMapper.toAuditData(duplicateCase);
        duplicateCase.resolve(command.action(), reason, command.actorId(), timeProvider.now());
        DuplicateCase savedCase = duplicateCaseRepository.save(duplicateCase);
        auditResolved(savedCase, beforeData, command, actorRole, reason);
        return duplicateCaseMapper.toSnapshot(savedCase);
    }

    @Override
    @Transactional(readOnly = true)
    public DuplicateCaseSnapshot findCase(UUID tenantId, UUID duplicateCaseId) {
        if (tenantId == null) {
            throw validation("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (duplicateCaseId == null) {
            throw validation("duplicate_case_id", "REQUIRED", "Duplicate case id is required");
        }
        return duplicateCaseRepository.findByIdAndTenantId(duplicateCaseId, tenantId)
                .map(duplicateCaseMapper::toSnapshot)
                .orElseThrow(() -> notFound("duplicate_case_id", "Duplicate case not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DuplicateCaseSnapshot> listCases(UUID tenantId, DuplicateCaseStatus status) {
        if (tenantId == null) {
            throw validation("tenant_id", "REQUIRED", "Tenant id is required");
        }
        DuplicateCaseStatus resolvedStatus = status == null ? DuplicateCaseStatus.NEEDS_REVIEW : status;
        return duplicateCaseRepository.findByTenantIdAndStatusOrderByCreatedAtAsc(tenantId, resolvedStatus)
                .stream()
                .map(duplicateCaseMapper::toSnapshot)
                .toList();
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
            throw validation("command", "REQUIRED", "Duplicate case command is required");
        }
        if (command.tenantId() == null) {
            throw validation("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (command.sourceCustomerId() == null) {
            throw validation("source_customer_id", "REQUIRED", "Source customer id is required");
        }
        if (command.matchedCustomerId() == null) {
            throw validation("matched_customer_id", "REQUIRED", "Matched customer id is required");
        }
    }

    private void validateResolveCommand(DuplicateCaseResolveCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Duplicate case resolve command is required");
        }
        if (command.tenantId() == null) {
            throw validation("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (command.duplicateCaseId() == null) {
            throw validation("duplicate_case_id", "REQUIRED", "Duplicate case id is required");
        }
        if (command.action() == null) {
            throw validation("action", "REQUIRED", "Resolution action is required");
        }
        if (command.actorId() == null) {
            throw validation("actor_id", "REQUIRED", "Actor id is required");
        }
        if (!StringUtils.hasText(command.actorRole())) {
            throw validation("actor_role", "REQUIRED", "Actor role is required");
        }
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
                .orElseThrow(() -> notFound(field, "Customer profile not found"));
    }

    private DuplicateConfidence resolveConfidence(DuplicateConfidence requestedConfidence, DuplicateConfidence defaultConfidence) {
        return requestedConfidence == null ? defaultConfidence : requestedConfidence;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String requireText(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw validation(field, "REQUIRED", message);
        }
        return value.trim();
    }

    private String canonicalValue(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private void assertResolvePermission(String actorRole) {
        if (!RESOLVE_ALLOWED_ROLES.contains(actorRole)) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    "Permission denied",
                    List.of(ErrorDetail.of("actor_role", "DUPLICATE_RESOLVE_DENIED", "Role cannot resolve duplicate cases"))
            );
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
            DuplicateCaseResolveCommand command,
            String actorRole,
            String reason) {
        auditService.record(new AuditRecordCommand(
                duplicateCase.tenantId(),
                command.actorId(),
                "USER",
                actorRole,
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
        return StringUtils.hasText(role) ? normalizeRole(role) : null;
    }

    private BusinessException notFound(String field, String message) {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                message,
                List.of(ErrorDetail.of(field, "NOT_FOUND", message))
        );
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }

    private record DuplicateCustomerPair(
            CustomerProfile sourceCustomer,
            CustomerProfile matchedCustomer
    ) {
    }
}
