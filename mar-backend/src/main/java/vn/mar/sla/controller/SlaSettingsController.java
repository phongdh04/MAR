package vn.mar.sla.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.sla.dto.request.UpdateSlaPoliciesRequest;
import vn.mar.sla.dto.request.UpdateWorkingHoursRequest;
import vn.mar.sla.dto.response.SlaPoliciesResponse;
import vn.mar.sla.dto.response.WorkingHoursConfigResponse;
import vn.mar.sla.service.SlaSettingsService;

@RestController
@RequestMapping("/api/v1")
public class SlaSettingsController {

    private final SlaSettingsService slaSettingsService;

    public SlaSettingsController(SlaSettingsService slaSettingsService) {
        this.slaSettingsService = slaSettingsService;
    }

    @GetMapping("/working-hours")
    @PreAuthorize("@authz.hasPermission(authentication, 'sla.view')")
    public ResponseEntity<ApiResponse<WorkingHoursConfigResponse>> getWorkingHours(
            @RequestParam(name = "branch_id", required = false) UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(slaSettingsService.getWorkingHours(branchId)));
    }

    @PatchMapping("/working-hours")
    @PreAuthorize("@authz.hasPermission(authentication, 'sla.manage')")
    public ResponseEntity<ApiResponse<WorkingHoursConfigResponse>> updateWorkingHours(
            @Valid @RequestBody UpdateWorkingHoursRequest request) {
        return ResponseEntity.ok(ApiResponse.success(slaSettingsService.updateWorkingHours(request)));
    }

    @GetMapping("/sla-policies")
    @PreAuthorize("@authz.hasPermission(authentication, 'sla.view')")
    public ResponseEntity<ApiResponse<SlaPoliciesResponse>> getSlaPolicies(
            @RequestParam(name = "branch_id", required = false) UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(slaSettingsService.getSlaPolicies(branchId)));
    }

    @PatchMapping("/sla-policies")
    @PreAuthorize("@authz.hasPermission(authentication, 'sla.manage')")
    public ResponseEntity<ApiResponse<SlaPoliciesResponse>> updateSlaPolicies(
            @Valid @RequestBody UpdateSlaPoliciesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(slaSettingsService.updateSlaPolicies(request)));
    }
}
