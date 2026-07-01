package vn.mar.security.jwt;

import java.time.Instant;

public record JwtToken(
        String token,
        Instant expiresAt
) {
}
