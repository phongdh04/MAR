package vn.mar.branch.service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import vn.mar.branch.dto.request.BranchSearchRequest;
import vn.mar.branch.dto.request.CreateBranchRequest;
import vn.mar.branch.dto.request.UpdateBranchRequest;
import vn.mar.branch.dto.response.BranchDetailResponse;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.mapper.BranchMapper;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class BranchService {

    private static final int BRANCH_CODE_MAX_LENGTH = 50;
    private static final int BRANCH_CODE_SUFFIX_LENGTH = 8;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    public BranchService(
            BranchRepository branchRepository,
            BranchMapper branchMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService) {
        this.branchRepository = branchRepository;
        this.branchMapper = branchMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
    }

    @Transactional
    public BranchDetailResponse createBranch(CreateBranchRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        UUID branchId = UUID.randomUUID();
        String branchName = requireBranchName(request.branchName());
        String branchCode = resolveBranchCode(request.branchCode(), branchName, branchId);
        BranchStatus status = resolveStatus(request.status(), BranchStatus.ACTIVE);

        assertActiveBranchAvailableForCreate(tenantId, branchCode, branchName, status);

        Branch branch = Branch.create(
                branchId,
                tenantId,
                branchCode,
                branchName,
                normalizeOptional(request.city()),
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.address()),
                status,
                actor.actorId(),
                now
        );

        Branch savedBranch = branchRepository.save(branch);
        auditBranchChange(AuditActions.BRANCH_CREATED, actor, savedBranch, null, branchMapper.toAuditData(savedBranch), null);
        return branchMapper.toDetailResponse(savedBranch);
    }

    @Transactional(readOnly = true)
    public BranchDetailResponse getBranch(UUID branchId) {
        Branch branch = findBranchInCurrentTenant(branchId);
        return branchMapper.toDetailResponse(branch);
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchDetailResponse> searchBranches(BranchSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        int page = resolvePage(request.page());
        int size = resolveSize(request.size());
        BranchStatus status = resolveStatus(request.status(), null);
        String keyword = normalizeKeyword(request.keyword());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BranchDetailResponse> responsePage = branchRepository.search(tenantId, status, keyword, pageable)
                .map(branchMapper::toDetailResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public BranchDetailResponse updateBranch(UUID branchId, UpdateBranchRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Branch branch = findBranch(tenantId, branchId);
        Map<String, Object> beforeData = branchMapper.toAuditData(branch);
        BranchStatus previousStatus = branch.status();

        String branchName = resolveBranchNameForUpdate(request.branchName(), branch.branchName());
        BranchStatus status = resolveStatus(request.status(), branch.status());
        assertActiveBranchAvailableForUpdate(tenantId, branch.id(), branch.branchCode(), branchName, status);

        branch.update(
                branchName,
                resolveOptionalForUpdate(request.city(), branch.city()),
                resolveOptionalForUpdate(request.phoneNumber(), branch.phoneNumber()),
                resolveOptionalForUpdate(request.address(), branch.address()),
                status,
                actor.actorId(),
                timeProvider.now()
        );

        Branch savedBranch = branchRepository.save(branch);
        String action = previousStatus == savedBranch.status()
                ? AuditActions.BRANCH_UPDATED
                : AuditActions.BRANCH_STATUS_CHANGED;
        auditBranchChange(
                action,
                actor,
                savedBranch,
                beforeData,
                branchMapper.toAuditData(savedBranch),
                normalizeOptional(request.reason())
        );
        return branchMapper.toDetailResponse(savedBranch);
    }

    private Branch findBranchInCurrentTenant(UUID branchId) {
        return findBranch(requireTenantContext(currentUserContext.currentUser()), branchId);
    }

    private Branch findBranch(UUID tenantId, UUID branchId) {
        return branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private String requireBranchName(String branchName) {
        if (!StringUtils.hasText(branchName)) {
            throw validation("branch_name", "REQUIRED", "Branch name is required");
        }
        return branchName.trim();
    }

    private String resolveBranchNameForUpdate(String requestedBranchName, String currentBranchName) {
        if (requestedBranchName == null) {
            return currentBranchName;
        }
        return requireBranchName(requestedBranchName);
    }

    private String resolveOptionalForUpdate(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        return normalizeOptional(requestedValue);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BranchStatus resolveStatus(String requestedStatus, BranchStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw validation("status", "INVALID_STATUS", "Branch status is invalid");
        }
        try {
            return BranchStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("status", "INVALID_STATUS", "Branch status is invalid");
        }
    }

    private String resolveBranchCode(String requestedBranchCode, String branchName, UUID branchId) {
        if (StringUtils.hasText(requestedBranchCode)) {
            return normalizeBranchCode(requestedBranchCode, "branch_code");
        }

        String suffix = branchId.toString().substring(0, BRANCH_CODE_SUFFIX_LENGTH).toUpperCase(Locale.ROOT);
        String prefix = normalizeBranchCode(branchName, "branch_name");
        int maxPrefixLength = BRANCH_CODE_MAX_LENGTH - suffix.length() - 1;
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return "%s-%s".formatted(prefix, suffix);
    }

    private String normalizeBranchCode(String source, String fieldName) {
        String ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String code = ascii.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(code)) {
            throw validation(fieldName, "INVALID_FORMAT", "Branch code is invalid");
        }
        if (code.length() > BRANCH_CODE_MAX_LENGTH) {
            return code.substring(0, BRANCH_CODE_MAX_LENGTH);
        }
        return code;
    }

    private void assertActiveBranchAvailableForCreate(
            UUID tenantId,
            String branchCode,
            String branchName,
            BranchStatus status) {
        if (status != BranchStatus.ACTIVE) {
            return;
        }
        if (branchRepository.existsByTenantIdAndBranchCodeIgnoreCaseAndStatus(tenantId, branchCode, BranchStatus.ACTIVE)
                || branchRepository.existsByTenantIdAndBranchNameIgnoreCaseAndStatus(tenantId, branchName, BranchStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_BRANCH, ErrorCode.DUPLICATE_ACTIVE_BRANCH.defaultMessage());
        }
    }

    private void assertActiveBranchAvailableForUpdate(
            UUID tenantId,
            UUID branchId,
            String branchCode,
            String branchName,
            BranchStatus status) {
        if (status != BranchStatus.ACTIVE) {
            return;
        }
        if (branchRepository.existsByTenantIdAndBranchCodeIgnoreCaseAndStatusAndIdNot(tenantId, branchCode, BranchStatus.ACTIVE, branchId)
                || branchRepository.existsByTenantIdAndBranchNameIgnoreCaseAndStatusAndIdNot(tenantId, branchName, BranchStatus.ACTIVE, branchId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_BRANCH, ErrorCode.DUPLICATE_ACTIVE_BRANCH.defaultMessage());
        }
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

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void auditBranchChange(
            String action,
            CurrentUser actor,
            Branch branch,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                branch.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                AuditResourceTypes.BRANCH,
                branch.id(),
                branch.branchCode(),
                beforeData,
                afterData,
                auditMetadata(actor),
                reason,
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

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
