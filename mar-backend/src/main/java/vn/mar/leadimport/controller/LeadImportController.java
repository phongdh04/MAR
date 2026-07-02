package vn.mar.leadimport.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.leadimport.dto.request.ImportRowErrorSearchRequest;
import vn.mar.leadimport.dto.request.LeadImportPreviewRequest;
import vn.mar.leadimport.dto.request.LeadImportSearchRequest;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.dto.response.ImportBatchSummaryResponse;
import vn.mar.leadimport.dto.response.ImportRowErrorResponse;
import vn.mar.leadimport.dto.response.LeadImportPreviewResponse;
import vn.mar.leadimport.service.LeadImportPreviewService;
import vn.mar.leadimport.service.LeadImportQueryService;

@RestController
@RequestMapping("/api/v1/imports/leads")
public class LeadImportController {

    private final LeadImportQueryService leadImportQueryService;
    private final LeadImportPreviewService leadImportPreviewService;

    public LeadImportController(
            LeadImportQueryService leadImportQueryService,
            LeadImportPreviewService leadImportPreviewService) {
        this.leadImportQueryService = leadImportQueryService;
        this.leadImportPreviewService = leadImportPreviewService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.hasPermission(authentication, 'lead.import')")
    public ResponseEntity<ApiResponse<LeadImportPreviewResponse>> previewLeadImport(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("mapping_config") LeadImportPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leadImportPreviewService.previewLeadImport(request, file)));
    }

    @GetMapping
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'import.view', 'import.manage')")
    public ResponseEntity<ApiResponse<PageResponse<ImportBatchSummaryResponse>>> searchLeadImports(
            @RequestParam(required = false) String status,
            @RequestParam(name = "source_type", required = false) String sourceType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        LeadImportSearchRequest request = new LeadImportSearchRequest(status, sourceType, page, size);
        return ResponseEntity.ok(ApiResponse.success(leadImportQueryService.searchLeadImports(request)));
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'import.view', 'import.manage')")
    public ResponseEntity<ApiResponse<ImportBatchDetailResponse>> getLeadImportBatch(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.success(leadImportQueryService.getLeadImportBatch(batchId)));
    }

    @GetMapping("/{batchId}/errors")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'import.view', 'import.manage')")
    public ResponseEntity<ApiResponse<PageResponse<ImportRowErrorResponse>>> getLeadImportErrors(
            @PathVariable UUID batchId,
            ImportRowErrorSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leadImportQueryService.getLeadImportErrors(batchId, request)));
    }
}
