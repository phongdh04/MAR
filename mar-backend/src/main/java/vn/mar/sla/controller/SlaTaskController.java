package vn.mar.sla.controller;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.sla.api.SlaOverdueScanSnapshot;
import vn.mar.sla.api.SlaTaskManagementService;
import vn.mar.sla.api.SlaTaskSearchCommand;
import vn.mar.sla.api.SlaTaskSnapshot;
import vn.mar.sla.dto.response.SlaOverdueScanResponse;
import vn.mar.sla.dto.response.SlaTaskResponse;
import vn.mar.sla.mapper.SlaTaskMapper;

@RestController
@RequestMapping("/api/v1/sla-tasks")
public class SlaTaskController {

    private final SlaTaskManagementService slaTaskManagementService;
    private final SlaTaskMapper slaTaskMapper;

    public SlaTaskController(
            SlaTaskManagementService slaTaskManagementService,
            SlaTaskMapper slaTaskMapper) {
        this.slaTaskManagementService = slaTaskManagementService;
        this.slaTaskMapper = slaTaskMapper;
    }

    @GetMapping
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'sla.task.view', 'sla.task.manage')")
    public ResponseEntity<ApiResponse<PageResponse<SlaTaskResponse>>> searchTasks(
            @RequestParam(name = "owner_id", required = false) UUID ownerId,
            @RequestParam(name = "branch_id", required = false) UUID branchId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "task_type", required = false) String taskType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<SlaTaskSnapshot> snapshots = slaTaskManagementService.searchTasks(
                new SlaTaskSearchCommand(ownerId, branchId, status, taskType, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(snapshots.map(slaTaskMapper::toResponse)));
    }

    @PostMapping("/overdue-scan")
    @PreAuthorize("@authz.hasPermission(authentication, 'sla.task.manage')")
    public ResponseEntity<ApiResponse<SlaOverdueScanResponse>> scanOverdueTasks() {
        SlaOverdueScanSnapshot snapshot = slaTaskManagementService.scanOverdueTasks();
        return ResponseEntity.ok(ApiResponse.success(slaTaskMapper.toResponse(snapshot)));
    }

}
