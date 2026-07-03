package vn.mar.customer.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import vn.mar.customer.api.CustomerMergeManagementService;
import vn.mar.customer.api.MergeHistorySnapshot;
import vn.mar.customer.api.UnmergeCustomerCommand;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.entity.MergeHistory;
import vn.mar.customer.mapper.MergeHistoryMapper;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.customer.repository.MergeHistoryRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class CustomerMergeService implements CustomerMergeManagementService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final MergeHistoryRepository mergeHistoryRepository;
    private final DuplicateCaseRepository duplicateCaseRepository;
    private final MergeHistoryMapper mergeHistoryMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    public CustomerMergeService(
            MergeHistoryRepository mergeHistoryRepository,
            DuplicateCaseRepository duplicateCaseRepository,
            MergeHistoryMapper mergeHistoryMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService) {
        this.mergeHistoryRepository = mergeHistoryRepository;
        this.duplicateCaseRepository = duplicateCaseRepository;
        this.mergeHistoryMapper = mergeHistoryMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
    }

    @Transactional
    public MergeHistorySnapshot recordMerge(
            DuplicateCase duplicateCase,
            CustomerProfile sourceCustomer,
            CustomerProfile targetCustomer,
            CurrentUser actor,
            String reason) {
        validateMergeCustomers(duplicateCase, sourceCustomer, targetCustomer);
        String cleanReason = requireText(reason, "reason", "Merge reason is required");
        var now = timeProvider.now();
        MergeHistory mergeHistory = MergeHistory.create(
                UUID.randomUUID(),
                duplicateCase.tenantId(),
                sourceCustomer.id(),
                targetCustomer.id(),
                duplicateCase.id(),
                actor.actorId(),
                now,
                cleanReason,
                mergeHistoryMapper.toMergeSnapshot(duplicateCase, sourceCustomer, targetCustomer),
                true
        );
        MergeHistory savedHistory = mergeHistoryRepository.save(mergeHistory);
        auditMerged(savedHistory, actor, cleanReason);
        return mergeHistoryMapper.toSnapshot(savedHistory);
    }

    @Override
    @Transactional
    public MergeHistorySnapshot unmerge(UnmergeCustomerCommand command) {
        validateUnmergeCommand(command);
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        assertHasPermission(actor, PermissionCodes.CUSTOMER_MERGE, "CUSTOMER_UNMERGE_DENIED", "Permission is required to unmerge customers");
        assertAdmin(actor);
        String reason = requireText(command.reason(), "reason", "Unmerge reason is required");

        MergeHistory mergeHistory = mergeHistoryRepository.findByIdAndTenantIdAndTargetCustomerId(
                        command.mergeId(),
                        tenantId,
                        command.customerId()
                )
                .orElseThrow(() -> notFound("merge_id", "Merge history not found"));
        if (!mergeHistory.canUnmerge() || mergeHistory.unmergedAt() != null) {
            throw new BusinessException(
                    ErrorCode.UNMERGE_NOT_ALLOWED,
                    ErrorCode.UNMERGE_NOT_ALLOWED.defaultMessage(),
                    List.of(ErrorDetail.of("merge_id", "UNMERGE_NOT_ALLOWED", "Merge cannot be safely unmerged"))
            );
        }

        Map<String, Object> beforeData = mergeHistoryMapper.toAuditData(mergeHistory);
        mergeHistory.markUnmerged(actor.actorId(), timeProvider.now());
        MergeHistory savedHistory = mergeHistoryRepository.save(mergeHistory);
        markDuplicateCaseUnmerged(savedHistory);
        auditUnmerged(savedHistory, beforeData, actor, reason);
        return mergeHistoryMapper.toSnapshot(savedHistory);
    }

    private void validateMergeCustomers(
            DuplicateCase duplicateCase,
            CustomerProfile sourceCustomer,
            CustomerProfile targetCustomer) {
        if (!duplicateCase.tenantId().equals(sourceCustomer.tenantId())
                || !duplicateCase.tenantId().equals(targetCustomer.tenantId())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Merge customers must belong to the duplicate case tenant",
                    List.of(ErrorDetail.of("tenant_id", "TENANT_MISMATCH", "Merge customers must belong to the same tenant"))
            );
        }
        if (sourceCustomer.id().equals(targetCustomer.id())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot merge customer into itself",
                    List.of(ErrorDetail.of("target_customer_id", "SAME_CUSTOMER", "Cannot merge customer into itself"))
            );
        }
    }

    private void validateUnmergeCommand(UnmergeCustomerCommand command) {
        if (command == null) {
            throw validation("command", "REQUIRED", "Unmerge command is required");
        }
        if (command.customerId() == null) {
            throw validation("customer_id", "REQUIRED", "Customer id is required");
        }
        if (command.mergeId() == null) {
            throw validation("merge_id", "REQUIRED", "Merge id is required");
        }
    }

    private void markDuplicateCaseUnmerged(MergeHistory mergeHistory) {
        if (mergeHistory.duplicateCaseId() == null) {
            return;
        }
        duplicateCaseRepository.findByIdAndTenantId(mergeHistory.duplicateCaseId(), mergeHistory.tenantId())
                .filter(duplicateCase -> duplicateCase.status() == DuplicateCaseStatus.MERGED)
                .ifPresent(duplicateCase -> {
                    duplicateCase.markUnmerged(mergeHistory.unmergedAt());
                    duplicateCaseRepository.save(duplicateCase);
                });
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void assertAdmin(CurrentUser actor) {
        if (!ADMIN_ROLE.equals(actor.roleCode())) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    "Permission denied",
                    List.of(ErrorDetail.of("role", "CUSTOMER_UNMERGE_ADMIN_REQUIRED", "Admin role is required to unmerge customers"))
            );
        }
    }

    private void assertHasPermission(CurrentUser actor, String permissionCode, String detailCode, String message) {
        if (!actor.hasPermission(permissionCode)) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    "Permission denied",
                    List.of(ErrorDetail.of("permission", detailCode, message))
            );
        }
    }

    private String requireText(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw validation(field, "REQUIRED", message);
        }
        return value.trim();
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

    private void auditMerged(MergeHistory mergeHistory, CurrentUser actor, String reason) {
        auditService.record(new AuditRecordCommand(
                mergeHistory.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                AuditActions.CUSTOMER_MERGED,
                AuditResourceTypes.CUSTOMER_PROFILE,
                mergeHistory.targetCustomerId(),
                mergeHistory.targetCustomerId().toString(),
                null,
                mergeHistoryMapper.toAuditData(mergeHistory),
                auditMetadata(mergeHistory),
                reason,
                LogContext.requestId()
        ));
    }

    private void auditUnmerged(
            MergeHistory mergeHistory,
            Map<String, Object> beforeData,
            CurrentUser actor,
            String reason) {
        auditService.record(new AuditRecordCommand(
                mergeHistory.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                AuditActions.CUSTOMER_UNMERGED,
                AuditResourceTypes.MERGE_HISTORY,
                mergeHistory.id(),
                mergeHistory.id().toString(),
                beforeData,
                mergeHistoryMapper.toAuditData(mergeHistory),
                auditMetadata(mergeHistory),
                reason,
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(MergeHistory mergeHistory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source_customer_id", mergeHistory.sourceCustomerId().toString());
        metadata.put("target_customer_id", mergeHistory.targetCustomerId().toString());
        if (mergeHistory.duplicateCaseId() != null) {
            metadata.put("duplicate_case_id", mergeHistory.duplicateCaseId().toString());
        }
        metadata.put("can_unmerge", mergeHistory.canUnmerge());
        return metadata;
    }
}
