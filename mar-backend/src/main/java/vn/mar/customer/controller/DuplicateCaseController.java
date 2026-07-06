package vn.mar.customer.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.customer.api.DuplicateCaseManagementService;
import vn.mar.customer.api.DuplicateCaseResolveCommand;
import vn.mar.customer.api.DuplicateCaseSearchCommand;
import vn.mar.customer.api.DuplicateCaseSnapshot;
import vn.mar.customer.dto.request.ResolveDuplicateCaseRequest;
import vn.mar.customer.dto.response.DuplicateCaseResponse;
import vn.mar.customer.mapper.DuplicateCaseMapper;

@RestController
@RequestMapping("/api/v1/duplicates")
public class DuplicateCaseController {

    private final DuplicateCaseManagementService duplicateCaseManagementService;
    private final DuplicateCaseMapper duplicateCaseMapper;

    public DuplicateCaseController(
            DuplicateCaseManagementService duplicateCaseManagementService,
            DuplicateCaseMapper duplicateCaseMapper) {
        this.duplicateCaseManagementService = duplicateCaseManagementService;
        this.duplicateCaseMapper = duplicateCaseMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'duplicate.manage')")
    public ResponseEntity<ApiResponse<PageResponse<DuplicateCaseResponse>>> searchDuplicateCases(
            @RequestParam(required = false) String status,
            @RequestParam(name = "match_type", required = false) String matchType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<DuplicateCaseSnapshot> snapshots = duplicateCaseManagementService.searchCases(
                new DuplicateCaseSearchCommand(status, matchType, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(snapshots.map(duplicateCaseMapper::toResponse)));
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'duplicate.manage')")
    public ResponseEntity<ApiResponse<DuplicateCaseResponse>> getDuplicateCase(@PathVariable UUID caseId) {
        DuplicateCaseSnapshot snapshot = duplicateCaseManagementService.findCase(caseId);
        return ResponseEntity.ok(ApiResponse.success(duplicateCaseMapper.toResponse(snapshot)));
    }

    @PostMapping("/{caseId}/resolve")
    @PreAuthorize("@authz.hasPermission(authentication, 'duplicate.manage')")
    public ResponseEntity<ApiResponse<DuplicateCaseResponse>> resolveDuplicateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody ResolveDuplicateCaseRequest request) {
        DuplicateCaseSnapshot snapshot = duplicateCaseManagementService.resolveCase(
                new DuplicateCaseResolveCommand(caseId, request.action(), request.targetCustomerId(), request.reason())
        );
        return ResponseEntity.ok(ApiResponse.success(duplicateCaseMapper.toResponse(snapshot)));
    }

}
