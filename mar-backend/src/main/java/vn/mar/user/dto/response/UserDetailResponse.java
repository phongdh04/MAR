package vn.mar.user.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDetailResponse(
        UUID userId,
        UUID tenantId,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        List<UUID> branchIds,
        Instant createdAt,
        Instant updatedAt
) {
}
