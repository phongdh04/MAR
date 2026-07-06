package vn.mar.assignment.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.assignment.api.AssignOpportunityCommand;
import vn.mar.assignment.api.AssignmentEngineService;
import vn.mar.assignment.api.AssignmentResultSnapshot;
import vn.mar.assignment.api.AssignmentRuleSearchCommand;
import vn.mar.assignment.api.AssignmentRuleSnapshot;
import vn.mar.assignment.api.UnassignedAssignmentItemSearchCommand;
import vn.mar.assignment.api.UnassignedAssignmentItemSnapshot;
import vn.mar.assignment.dto.request.CreateAssignmentRuleRequest;
import vn.mar.assignment.dto.request.UpdateAssignmentRuleRequest;
import vn.mar.assignment.dto.response.AssignmentResultResponse;
import vn.mar.assignment.dto.response.AssignmentRuleResponse;
import vn.mar.assignment.dto.response.UnassignedAssignmentItemResponse;
import vn.mar.assignment.mapper.AssignmentMapper;
import vn.mar.assignment.service.AssignmentRuleService;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@RestController
@RequestMapping("/api/v1")
public class AssignmentController {

    private final AssignmentRuleService assignmentRuleService;
    private final AssignmentEngineService assignmentEngineService;
    private final AssignmentMapper assignmentMapper;
    private final CurrentUserContext currentUserContext;

    public AssignmentController(
            AssignmentRuleService assignmentRuleService,
            AssignmentEngineService assignmentEngineService,
            AssignmentMapper assignmentMapper,
            CurrentUserContext currentUserContext) {
        this.assignmentRuleService = assignmentRuleService;
        this.assignmentEngineService = assignmentEngineService;
        this.assignmentMapper = assignmentMapper;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping("/assignment-rules")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'assignment.view', 'assignment.manage')")
    public ResponseEntity<ApiResponse<PageResponse<AssignmentRuleResponse>>> searchRules(
            @RequestParam(required = false) String status,
            @RequestParam(name = "branch_id", required = false) UUID branchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<AssignmentRuleSnapshot> snapshots = assignmentRuleService.searchRules(
                new AssignmentRuleSearchCommand(status, branchId, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(toRuleResponsePage(snapshots)));
    }

    @GetMapping("/assignment-rules/{assignmentRuleId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'assignment.view', 'assignment.manage')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> getRule(@PathVariable UUID assignmentRuleId) {
        return ResponseEntity.ok(ApiResponse.success(assignmentMapper.toResponse(
                assignmentRuleService.getRule(assignmentRuleId)
        )));
    }

    @PostMapping("/assignment-rules")
    @PreAuthorize("@authz.hasPermission(authentication, 'assignment.manage')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> createRule(@RequestBody CreateAssignmentRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assignmentMapper.toResponse(
                assignmentRuleService.createRule(request)
        )));
    }

    @PatchMapping("/assignment-rules/{assignmentRuleId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'assignment.manage')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> updateRule(
            @PathVariable UUID assignmentRuleId,
            @RequestBody UpdateAssignmentRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assignmentMapper.toResponse(
                assignmentRuleService.updateRule(assignmentRuleId, request)
        )));
    }

    @GetMapping("/assignments/unassigned-items")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'assignment.view', 'assignment.manage')")
    public ResponseEntity<ApiResponse<PageResponse<UnassignedAssignmentItemResponse>>> searchUnassignedItems(
            @RequestParam(required = false) String status,
            @RequestParam(name = "branch_id", required = false) UUID branchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageResponse<UnassignedAssignmentItemSnapshot> snapshots = assignmentRuleService.searchUnassignedItems(
                new UnassignedAssignmentItemSearchCommand(status, branchId, page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(toUnassignedResponsePage(snapshots)));
    }

    @PostMapping("/assignments/opportunities/{opportunityId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'assignment.manage')")
    public ResponseEntity<ApiResponse<AssignmentResultResponse>> assignOpportunity(@PathVariable UUID opportunityId) {
        CurrentUser actor = currentUserContext.currentUser();
        AssignmentResultSnapshot snapshot = assignmentEngineService.assignOpportunity(
                new AssignOpportunityCommand(
                        actor.tenantId(),
                        opportunityId,
                        actor.actorId(),
                        "Manual assignment trigger"
                )
        );
        return ResponseEntity.ok(ApiResponse.success(assignmentMapper.toResponse(snapshot)));
    }

    private PageResponse<AssignmentRuleResponse> toRuleResponsePage(PageResponse<AssignmentRuleSnapshot> snapshots) {
        return new PageResponse<>(
                snapshots.items().stream().map(assignmentMapper::toResponse).toList(),
                snapshots.page(),
                snapshots.size(),
                snapshots.totalElements(),
                snapshots.totalPages()
        );
    }

    private PageResponse<UnassignedAssignmentItemResponse> toUnassignedResponsePage(
            PageResponse<UnassignedAssignmentItemSnapshot> snapshots) {
        return new PageResponse<>(
                snapshots.items().stream().map(assignmentMapper::toResponse).toList(),
                snapshots.page(),
                snapshots.size(),
                snapshots.totalElements(),
                snapshots.totalPages()
        );
    }
}
