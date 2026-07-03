package vn.mar.customer.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.customer.api.CustomerMergeManagementService;
import vn.mar.customer.api.MergeHistorySnapshot;
import vn.mar.customer.api.UnmergeCustomerCommand;
import vn.mar.customer.dto.request.UnmergeCustomerRequest;
import vn.mar.customer.dto.response.MergeHistoryResponse;
import vn.mar.customer.mapper.MergeHistoryMapper;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerMergeController {

    private final CustomerMergeManagementService customerMergeManagementService;
    private final MergeHistoryMapper mergeHistoryMapper;

    public CustomerMergeController(
            CustomerMergeManagementService customerMergeManagementService,
            MergeHistoryMapper mergeHistoryMapper) {
        this.customerMergeManagementService = customerMergeManagementService;
        this.mergeHistoryMapper = mergeHistoryMapper;
    }

    @PostMapping("/{customerId}/unmerge")
    @PreAuthorize("@authz.hasPermission(authentication, 'customer.merge')")
    public ResponseEntity<ApiResponse<MergeHistoryResponse>> unmergeCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UnmergeCustomerRequest request) {
        MergeHistorySnapshot snapshot = customerMergeManagementService.unmerge(
                new UnmergeCustomerCommand(customerId, request.mergeId(), request.reason())
        );
        return ResponseEntity.ok(ApiResponse.success(mergeHistoryMapper.toResponse(snapshot)));
    }
}
