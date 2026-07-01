package vn.mar.security.jwt;

import java.time.Instant;
import java.util.UUID;

public record JwtClaims(
        UUID actorId,
        UUID tenantId,
        String roleCode,
        Instant issuedAt,
        Instant expiresAt
) {
}
