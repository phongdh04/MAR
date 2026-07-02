package vn.mar.branch.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.branch.dto.request.BranchSearchRequest;
import vn.mar.branch.dto.request.CreateBranchRequest;
import vn.mar.branch.dto.request.UpdateBranchRequest;
import vn.mar.branch.dto.response.BranchDetailResponse;
import vn.mar.branch.service.BranchService;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'branch.manage')")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> createBranch(
            @Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(branchService.createBranch(request)));
    }

    @GetMapping
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'branch.manage', 'branch.view', 'lead.view')")
    public ResponseEntity<ApiResponse<PageResponse<BranchDetailResponse>>> searchBranches(
            BranchSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.searchBranches(request)));
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'branch.manage', 'branch.view', 'lead.view')")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> getBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranch(branchId)));
    }

    @PatchMapping("/{branchId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'branch.manage')")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateBranch(branchId, request)));
    }
}
