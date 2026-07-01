package vn.mar.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ROLE_CODE_CLAIM = "role_code";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken createAccessToken(UUID actorId, UUID tenantId, String roleCode) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(actorId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(TENANT_ID_CLAIM, tenantId.toString())
                .claim(ROLE_CODE_CLAIM, roleCode)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
        return new JwtToken(token, expiresAt);
    }

    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtClaims(
                UUID.fromString(requiredValue(claims.getSubject(), "sub")),
                UUID.fromString(requiredClaim(claims, TENANT_ID_CLAIM)),
                requiredClaim(claims, ROLE_CODE_CLAIM),
                requiredValue(claims.getIssuedAt(), "iat").toInstant(),
                requiredValue(claims.getExpiration(), "exp").toInstant()
        );
    }

    public long expiresInSeconds(JwtToken token) {
        Duration remaining = Duration.between(Instant.now(), token.expiresAt());
        return Math.max(0, remaining.toSeconds());
    }

    private String requiredClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return requiredValue(value, name).toString();
    }

    private <T> T requiredValue(T value, String name) {
        if (value == null || value.toString().isBlank()) {
            throw new JwtException("Missing required claim: " + name);
        }
        return value;
    }
}
