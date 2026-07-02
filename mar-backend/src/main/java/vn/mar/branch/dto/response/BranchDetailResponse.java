package vn.mar.branch.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BranchDetailResponse(
        UUID branchId,
        UUID tenantId,
        String branchCode,
        String branchName,
        String city,
        String phoneNumber,
        String address,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
