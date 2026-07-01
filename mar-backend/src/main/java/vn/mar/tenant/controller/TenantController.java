package vn.mar.tenant.controller;

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
import vn.mar.common.dto.ApiResponse;
import vn.mar.tenant.dto.request.CreateTenantRequest;
import vn.mar.tenant.dto.request.UpdateTenantRequest;
import vn.mar.tenant.dto.response.TenantDetailResponse;
import vn.mar.tenant.service.TenantService;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'tenant.manage')")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tenantService.createTenant(request)));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'tenant.view', 'tenant.manage')")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> getTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(tenantId)));
    }

    @PatchMapping("/{tenantId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'tenant.manage')")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.updateTenant(tenantId, request)));
    }
}
