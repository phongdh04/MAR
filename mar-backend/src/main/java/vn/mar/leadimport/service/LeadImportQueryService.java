package vn.mar.leadimport.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.leadimport.dto.request.ImportRowErrorSearchRequest;
import vn.mar.leadimport.dto.request.LeadImportSearchRequest;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.dto.response.ImportBatchSummaryResponse;
import vn.mar.leadimport.dto.response.ImportRowErrorResponse;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.mapper.LeadImportMapper;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class LeadImportQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ImportBatchRepository importBatchRepository;
    private final ImportRowRepository importRowRepository;
    private final LeadImportMapper leadImportMapper;
    private final CurrentUserContext currentUserContext;

    public LeadImportQueryService(
            ImportBatchRepository importBatchRepository,
            ImportRowRepository importRowRepository,
            LeadImportMapper leadImportMapper,
            CurrentUserContext currentUserContext) {
        this.importBatchRepository = importBatchRepository;
        this.importRowRepository = importRowRepository;
        this.leadImportMapper = leadImportMapper;
        this.currentUserContext = currentUserContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportBatchSummaryResponse> searchLeadImports(LeadImportSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        PageRequest pageable = PageRequest.of(resolvePage(request.page()), resolveSize(request.size()));
        Page<ImportBatchSummaryResponse> responsePage = importBatchRepository.search(
                        tenantId,
                        ImportType.LEAD,
                        resolveBatchStatus(request.status()),
                        resolveSourceType(request.sourceType()),
                        pageable
                )
                .map(leadImportMapper::toSummaryResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ImportBatchDetailResponse getLeadImportBatch(UUID batchId) {
        ImportBatch batch = findLeadBatchInCurrentTenant(batchId);
        return leadImportMapper.toDetailResponse(batch);
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportRowErrorResponse> getLeadImportErrors(UUID batchId, ImportRowErrorSearchRequest request) {
        ImportBatch batch = findLeadBatchInCurrentTenant(batchId);
        PageRequest pageable = PageRequest.of(resolvePage(request.page()), resolveSize(request.size()));
        Page<ImportRowErrorResponse> responsePage = importRowRepository
                .findByTenantIdAndImportBatchIdAndRowStatusOrderByRowNumberAsc(
                        batch.tenantId(),
                        batch.id(),
                        ImportRowStatus.ERROR,
                        pageable
                )
                .map(leadImportMapper::toErrorResponse);
        return PageResponse.from(responsePage);
    }

    private ImportBatch findLeadBatchInCurrentTenant(UUID batchId) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        return importBatchRepository.findByIdAndTenantIdAndImportType(batchId, tenantId, ImportType.LEAD)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_BATCH_NOT_FOUND, ErrorCode.IMPORT_BATCH_NOT_FOUND.defaultMessage()));
    }

    private ImportBatchStatus resolveBatchStatus(String requestedStatus) {
        if (requestedStatus == null) {
            return null;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw validation("status", "INVALID_STATUS", "Import batch status is invalid");
        }
        try {
            return ImportBatchStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("status", "INVALID_STATUS", "Import batch status is invalid");
        }
    }

    private ImportSourceType resolveSourceType(String requestedSourceType) {
        if (requestedSourceType == null) {
            return null;
        }
        if (!StringUtils.hasText(requestedSourceType)) {
            throw validation("source_type", "INVALID_SOURCE_TYPE", "Import source type is invalid");
        }
        try {
            return ImportSourceType.valueOf(requestedSourceType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("source_type", "INVALID_SOURCE_TYPE", "Import source type is invalid");
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

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
