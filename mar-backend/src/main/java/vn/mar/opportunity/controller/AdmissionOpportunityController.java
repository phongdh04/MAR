package vn.mar.opportunity.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.opportunity.api.AdmissionOpportunityManagementService;
import vn.mar.opportunity.api.AdmissionOpportunitySearchCommand;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.ChangeOpportunityStageCommand;
import vn.mar.opportunity.api.StageChangeSnapshot;
import vn.mar.opportunity.api.StageHistorySnapshot;
import vn.mar.opportunity.api.UpdateAdmissionOpportunityCommand;
import vn.mar.opportunity.dto.request.ChangeOpportunityStageRequest;
import vn.mar.opportunity.dto.request.UpdateOpportunityRequest;
import vn.mar.opportunity.dto.response.OpportunityResponse;
import vn.mar.opportunity.dto.response.StageChangeResponse;
import vn.mar.opportunity.dto.response.StageHistoryResponse;
import vn.mar.opportunity.mapper.AdmissionOpportunityMapper;

@RestController
@RequestMapping("/api/v1/opportunities")
public class AdmissionOpportunityController {

    private final AdmissionOpportunityManagementService admissionOpportunityManagementService;
    private final AdmissionOpportunityMapper admissionOpportunityMapper;

    public AdmissionOpportunityController(
            AdmissionOpportunityManagementService admissionOpportunityManagementService,
            AdmissionOpportunityMapper admissionOpportunityMapper) {
        this.admissionOpportunityManagementService = admissionOpportunityManagementService;
        this.admissionOpportunityMapper = admissionOpportunityMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'lead.view', 'opportunity.update')")
    public ResponseEntity<ApiResponse<PageResponse<OpportunityResponse>>> searchOpportunities(
            @RequestParam(name = "owner_id", required = false) UUID ownerId,
            @RequestParam(required = false) String stage,
            @RequestParam(name = "language_id", required = false) UUID languageId,
            @RequestParam(name = "program_id", required = false) UUID programId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<AdmissionOpportunitySnapshot> snapshots = admissionOpportunityManagementService.searchOpportunities(
                new AdmissionOpportunitySearchCommand(ownerId, stage, languageId, programId, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(toResponsePage(snapshots)));
    }

    @GetMapping("/{opportunityId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'lead.view', 'opportunity.update')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getOpportunity(@PathVariable UUID opportunityId) {
        AdmissionOpportunitySnapshot snapshot = admissionOpportunityManagementService.getOpportunity(opportunityId);
        return ResponseEntity.ok(ApiResponse.success(admissionOpportunityMapper.toResponse(snapshot)));
    }

    @GetMapping("/{opportunityId}/stage-history")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'lead.view', 'opportunity.update')")
    public ResponseEntity<ApiResponse<List<StageHistoryResponse>>> getStageHistory(@PathVariable UUID opportunityId) {
        List<StageHistorySnapshot> snapshots = admissionOpportunityManagementService.getStageHistory(opportunityId);
        return ResponseEntity.ok(ApiResponse.success(snapshots.stream()
                .map(admissionOpportunityMapper::toResponse)
                .toList()));
    }

    @PatchMapping("/{opportunityId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'opportunity.update')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateOpportunity(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody UpdateOpportunityRequest request) {
        AdmissionOpportunitySnapshot snapshot = admissionOpportunityManagementService.updateOpportunity(
                new UpdateAdmissionOpportunityCommand(
                        opportunityId,
                        request.languageId(),
                        request.programId(),
                        request.courseId(),
                        request.branchId(),
                        request.qualificationStatus(),
                        request.note()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(admissionOpportunityMapper.toResponse(snapshot)));
    }

    @PostMapping("/{opportunityId}/stage")
    @PreAuthorize("@authz.hasPermission(authentication, 'opportunity.update')")
    public ResponseEntity<ApiResponse<StageChangeResponse>> changeStage(
            @PathVariable UUID opportunityId,
            @Valid @RequestBody ChangeOpportunityStageRequest request) {
        StageChangeSnapshot snapshot = admissionOpportunityManagementService.changeStage(
                new ChangeOpportunityStageCommand(
                        opportunityId,
                        request.toStage(),
                        request.lostReason(),
                        request.lostNote(),
                        request.reason()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(admissionOpportunityMapper.toResponse(snapshot)));
    }

    private PageResponse<OpportunityResponse> toResponsePage(PageResponse<AdmissionOpportunitySnapshot> snapshots) {
        return new PageResponse<>(
                snapshots.items().stream()
                        .map(admissionOpportunityMapper::toResponse)
                        .toList(),
                snapshots.page(),
                snapshots.size(),
                snapshots.totalElements(),
                snapshots.totalPages()
        );
    }
}
