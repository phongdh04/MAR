package vn.mar.permission.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.permission.dto.request.UpdatePermissionMatrixRequest;
import vn.mar.permission.dto.response.PermissionMatrixResponse;
import vn.mar.permission.service.PermissionMatrixService;

@RestController
@RequestMapping("/api/v1/permissions/matrix")
public class PermissionMatrixController {

    private final PermissionMatrixService permissionMatrixService;

    public PermissionMatrixController(PermissionMatrixService permissionMatrixService) {
        this.permissionMatrixService = permissionMatrixService;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'permission.manage')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> getMatrix() {
        return ResponseEntity.ok(ApiResponse.success(permissionMatrixService.getMatrix()));
    }

    @PatchMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'permission.manage')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> updateMatrix(
            @Valid @RequestBody UpdatePermissionMatrixRequest request) {
        return ResponseEntity.ok(ApiResponse.success(permissionMatrixService.updateMatrix(request)));
    }
}
