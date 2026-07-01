package vn.mar.auth.dto.response;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID actorId,
        UUID tenantId,
        String roleCode,
        Set<String> permissionCodes
) {
}
